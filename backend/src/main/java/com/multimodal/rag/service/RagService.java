package com.multimodal.rag.service;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.MultimodalDocumentRepository;
import com.multimodal.rag.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagService {

    private final VectorStoreService vectorStoreService;
    private final ChatClient.Builder chatClientBuilder;
    private final StatisticsService statisticsService;
    private final MultimodalDocumentRepository documentRepository;
    private final UserRepository userRepository;

    private static final String SYSTEM_PROMPT = """
        你是一名专业的知识库助手。请根据提供的【上下文】回答用户问题。
        如果上下文不足以回答，请明确说明不知道，不要编造。
        请保持回答准确、简洁，并优先依据检索到的内容作答。
        【上下文】：
        {context}
        """;

    private static final int DEFAULT_MAX_CONTEXT_LENGTH = 6000;
    private static final int PREMIUM_MAX_CONTEXT_LENGTH = 9000;

    public record RagResult(String answer, List<Map<String, Object>> sources) {}

    public RagResult chatWithSources(String query, Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("Processing RAG chat request for query: '{}'", query);

        boolean premiumUser = isPremiumUser(userId);
        int maxContextLength = premiumUser ? PREMIUM_MAX_CONTEXT_LENGTH : DEFAULT_MAX_CONTEXT_LENGTH;
        List<Document> similarDocuments = retrieveRelevantDocuments(query, userId, premiumUser);

        StringBuilder contextBuilder = new StringBuilder();
        List<Map<String, Object>> sources = new ArrayList<>();
        Set<Long> seenDocIds = new HashSet<>();

        for (Document doc : similarDocuments) {
            String segment = doc.getContent();
            if (segment == null || segment.isBlank()) {
                continue;
            }
            if (contextBuilder.length() + segment.length() + 2 > maxContextLength) {
                int remaining = maxContextLength - contextBuilder.length() - 5;
                if (remaining > 100) {
                    contextBuilder.append("\n\n").append(segment, 0, remaining).append("...");
                }
                break;
            }
            if (contextBuilder.length() > 0) {
                contextBuilder.append("\n\n");
            }
            contextBuilder.append(segment);

            Map<String, Object> meta = doc.getMetadata();
            Object docIdObj = meta.get("documentId");
            if (docIdObj != null) {
                Long docId = Long.parseLong(docIdObj.toString());
                if (seenDocIds.add(docId)) {
                    Map<String, Object> source = new LinkedHashMap<>();
                    source.put("docId", docId);
                    source.put("fileName", meta.getOrDefault("fileName", "未知文件"));
                    source.put("fileType", meta.getOrDefault("fileType", "unknown"));
                    String excerpt = segment.length() > 120 ? segment.substring(0, 120) + "..." : segment;
                    source.put("excerpt", excerpt);
                    // Include similarity score (0~1) from vector or keyword search
                    source.put("score", getScoreFromMeta(meta));
                    sources.add(source);
                }
            }
        }

        String context = contextBuilder.toString();
        log.info("RAG Context: found {} segments, {} unique sources, total length: {}",
                similarDocuments.size(), sources.size(), context.length());

        ChatClient chatClient = chatClientBuilder.build();
        String systemPromptWithContext = SYSTEM_PROMPT.replace("{context}",
                context.isEmpty() ? "（无相关背景资料）" : context);

        String response = null;
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                response = chatClient.prompt()
                        .system(systemPromptWithContext)
                        .user(query)
                        .call()
                        .content();
                break;
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                boolean isTransient = msg.contains("Connection reset")
                        || msg.contains("I/O error")
                        || msg.contains("Read timed out");
                if (isTransient && attempt < maxRetries) {
                    log.warn("Transient error on LLM call attempt {}/{}: {}. Retrying...", attempt, maxRetries, msg);
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    throw e;
                }
            }
        }

        long responseTime = System.currentTimeMillis() - startTime;
        try {
            statisticsService.logQuery(userId, query, (int) responseTime, similarDocuments.size());
        } catch (Exception e) {
            log.warn("Failed to log query statistics", e);
        }

        return new RagResult(response, sources);
    }

    public String chat(String query, Long userId) {
        return chatWithSources(query, userId).answer();
    }

    private boolean isPremiumUser(Long userId) {
        return userRepository.findById(userId)
                .map(User::getRoles)
                .stream()
                .flatMap(Set::stream)
                .anyMatch(role -> "PREMIUM".equalsIgnoreCase(role.getName()));
    }

    private List<Document> retrieveRelevantDocuments(String query, Long userId, boolean premiumUser) {
        int vectorTopK = premiumUser ? 8 : 5;
        int keywordTopK = premiumUser ? 6 : 4;
        int finalLimit = premiumUser ? 8 : 5;

        LinkedHashMap<String, Document> merged = new LinkedHashMap<>();
        for (Document doc : vectorStoreService.search(query, vectorTopK, userId)) {
            String key = buildMergeKey(doc);
            if (!merged.containsKey(key)) {
                // PgVector returns distance in metadata; convert to similarity score (0~1)
                Object dist = doc.getMetadata().get("distance");
                double score = 0.5;
                if (dist instanceof Number) {
                    // cosine distance → similarity: 1 - distance (distance is 0~2 for cosine)
                    score = Math.max(0.0, Math.min(1.0, 1.0 - ((Number) dist).doubleValue() / 2.0));
                }
                doc.getMetadata().put("score", score);
                doc.getMetadata().put("sourceType", "vector");
                merged.put(key, doc);
            }
        }
        for (Document doc : keywordSearch(query, userId, keywordTopK)) {
            String key = buildMergeKey(doc);
            if (!merged.containsKey(key)) {
                // keyword results have their own score from toSearchDocument
                merged.put(key, doc);
            } else {
                // Keep the higher score when the same doc appears in both results
                double existingScore = getScoreFromMeta(doc.getMetadata());
                double newScore = getScoreFromMeta(merged.get(key).getMetadata());
                if (existingScore > newScore) {
                    doc.getMetadata().put("score", existingScore);
                    merged.put(key, doc);
                }
            }
        }

        return merged.values().stream()
                .limit(finalLimit)
                .collect(Collectors.toList());
    }

    private String buildMergeKey(Document doc) {
        Object docId = doc.getMetadata().get("documentId");
        return String.valueOf(docId) + "::" + doc.getContent();
    }

    private List<Document> keywordSearch(String query, Long userId, int limit) {
        LinkedHashMap<Long, ScoredDocument> scored = new LinkedHashMap<>();
        for (String keyword : extractKeywords(query)) {
            List<MultimodalDocument> docs = documentRepository.searchAccessible(
                    userId,
                    keyword,
                    PageRequest.of(0, limit)
            ).getContent();

            for (MultimodalDocument doc : docs) {
                ScoredDocument entry = scored.computeIfAbsent(doc.getId(), ignored -> new ScoredDocument(doc));
                entry.score += scoreDocument(doc, keyword);
                entry.highlights.add(keyword);
            }
        }

        return scored.values().stream()
                .sorted(Comparator.comparingInt(ScoredDocument::score).reversed())
                .limit(limit)
                .map(this::toSearchDocument)
                .collect(Collectors.toList());
    }

    private List<String> extractKeywords(String query) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        if (query == null) {
            return List.of();
        }
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }

        keywords.add(trimmed);
        Arrays.stream(trimmed.split("[\\s,，。；;、]+"))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .forEach(keywords::add);
        return new ArrayList<>(keywords);
    }

    private int scoreDocument(MultimodalDocument doc, String keyword) {
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        int score = 0;
        score += contains(doc.getFileName(), normalizedKeyword) ? 12 : 0;
        score += contains(doc.getTags(), normalizedKeyword) ? 8 : 0;
        score += countOccurrences(doc.getExtractedContent(), normalizedKeyword) * 3;
        score += doc.isShared() ? 1 : 0;
        return score;
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private int countOccurrences(String text, String keyword) {
        if (text == null || keyword == null || keyword.isBlank()) {
            return 0;
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        int count = 0;
        int index = 0;
        while ((index = lowerText.indexOf(keyword, index)) >= 0) {
            count++;
            index += keyword.length();
        }
        return count;
    }

    private Document toSearchDocument(ScoredDocument scoredDocument) {
        MultimodalDocument doc = scoredDocument.document;
        String snippet = buildSnippet(doc.getExtractedContent(), scoredDocument.highlights);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("documentId", doc.getId());
        if (doc.getUser() != null) {
            metadata.put("userId", doc.getUser().getId());
        }
        metadata.put("fileName", doc.getFileName());
        metadata.put("fileType", doc.getFileType());
        metadata.put("sourceType", "keyword");
        // Normalize keyword score: keyword scores are typically small integers, map to 0~1
        double normalizedScore = Math.min(1.0, scoredDocument.score / 10.0);
        metadata.put("score", Math.max(0.05, normalizedScore));
        return new Document(snippet, metadata);
    }

    private String buildSnippet(String content, Set<String> highlights) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalizedContent = content.trim();
        String lower = normalizedContent.toLowerCase(Locale.ROOT);
        for (String keyword : highlights) {
            String loweredKeyword = keyword.toLowerCase(Locale.ROOT);
            int index = lower.indexOf(loweredKeyword);
            if (index >= 0) {
                int start = Math.max(0, index - 120);
                int end = Math.min(normalizedContent.length(), index + Math.max(240, keyword.length() + 120));
                String snippet = normalizedContent.substring(start, end).trim();
                if (start > 0) {
                    snippet = "..." + snippet;
                }
                if (end < normalizedContent.length()) {
                    snippet = snippet + "...";
                }
                return snippet;
            }
        }
        return normalizedContent.length() > 360 ? normalizedContent.substring(0, 360) + "..." : normalizedContent;
    }

    private double getScoreFromMeta(Map<String, Object> meta) {
        Object score = meta.get("score");
        if (score instanceof Number) {
            return ((Number) score).doubleValue();
        }
        return 0.5; // fallback default
    }

    private static class ScoredDocument {
        private final MultimodalDocument document;
        private final Set<String> highlights = new LinkedHashSet<>();
        private int score = 0;

        private ScoredDocument(MultimodalDocument document) {
            this.document = document;
        }

        private int score() {
            return score;
        }
    }
}
