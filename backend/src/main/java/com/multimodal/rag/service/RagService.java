package com.multimodal.rag.service;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.MultimodalDocumentRepository;
import com.multimodal.rag.repository.UserRepository;
import com.multimodal.rag.service.resilience.CircuitBreaker;
import com.multimodal.rag.service.resilience.ResilienceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagService {

    private static final String KEYWORD_SPLIT_REGEX =
            "[\\s\\p{Punct}\\uFF0C\\u3002\\uFF01\\uFF1F\\uFF1B\\uFF1A\\u3001\\uFF08\\uFF09\\u3010\\u3011\\u300A\\u300B\\u201C\\u201D\\u2018\\u2019]+";

    private static final String SYSTEM_PROMPT = """
            You are a retrieval-augmented knowledge-base assistant.
            Answer strictly from the provided context.
            If the context is missing, weak, or contradictory, say that clearly.
            Keep the answer concise, factual, and in the user's language.
            Mention source file names when they materially support the answer.

            Context:
            {context}
            """;

    private static final int DEFAULT_MAX_CONTEXT_LENGTH = 7000;
    private static final int PREMIUM_MAX_CONTEXT_LENGTH = 11000;

    private final VectorStoreService vectorStoreService;
    private final ChatClient.Builder chatClientBuilder;
    private final StatisticsService statisticsService;
    private final MultimodalDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ResilienceManager resilienceManager;

    public record RagResult(String answer, List<Map<String, Object>> sources, boolean fallback) {
        public RagResult {
        }
        /** Backward-compatible constructor without fallback flag. */
        public RagResult(String answer, List<Map<String, Object>> sources) {
            this(answer, sources, false);
        }
    }

    public RagResult chatWithSources(String query, Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("Processing RAG chat request for query: '{}'", query);

        boolean premiumUser = isPremiumUser(userId);
        int maxContextLength = premiumUser ? PREMIUM_MAX_CONTEXT_LENGTH : DEFAULT_MAX_CONTEXT_LENGTH;
        List<Document> retrievedDocuments = retrieveRelevantDocuments(query, userId, premiumUser);

        StringBuilder contextBuilder = new StringBuilder();
        List<Map<String, Object>> sources = new ArrayList<>();
        Set<Long> seenDocIds = new HashSet<>();
        double maxScore = 0;

        for (Document doc : retrievedDocuments) {
            String segment = buildContextSegment(doc);
            if (segment.isBlank()) {
                continue;
            }

            if (contextBuilder.length() + segment.length() + 2 > maxContextLength) {
                int remaining = maxContextLength - contextBuilder.length() - 5;
                if (remaining > 160) {
                    contextBuilder.append("\n\n").append(segment, 0, remaining).append("...");
                }
                break;
            }

            if (contextBuilder.length() > 0) {
                contextBuilder.append("\n\n");
            }
            contextBuilder.append(segment);

            Long docId = asLong(doc.getMetadata().get("documentId"));
            if (docId != null && seenDocIds.add(docId)) {
                sources.add(buildSourceEntry(doc));
                // Track max raw score for normalization
                Object rawScore = doc.getMetadata().get("relevanceScore");
                if (rawScore instanceof Number) {
                    maxScore = Math.max(maxScore, ((Number) rawScore).doubleValue());
                }
            }
        }

        // Normalize scores to 0.0-1.0 range based on the best result in this batch
        if (maxScore > 0) {
            for (Map<String, Object> src : sources) {
                Object raw = src.get("score");
                if (raw instanceof Number) {
                    double normalized = ((Number) raw).doubleValue() / maxScore;
                    src.put("score", Math.round(normalized * 100.0) / 100.0);
                }
            }
        }

        String context = contextBuilder.toString();
        log.info("RAG context assembled: {} retrieved chunks, {} unique sources, {} chars",
                retrievedDocuments.size(), sources.size(), context.length());

        String systemPromptWithContext = SYSTEM_PROMPT.replace(
                "{context}",
                context.isBlank() ? "(No reliable context retrieved.)" : context
        );

        ChatClient chatClient = chatClientBuilder.build();
        final String finalContext = context;
        final boolean[] llmFallback = {false};
        String response = resilienceManager.executeWithRetry(
                "llm-chat",
                () -> chatClient.prompt()
                        .system(systemPromptWithContext)
                        .user(query)
                        .call()
                        .content(),
                () -> {
                    // LLM 不可用时的降级回答：基于已检索到的文档返回摘要
                    llmFallback[0] = true;
                    log.warn("[RagService] LLM unavailable, generating fallback response with {} sources", sources.size());
                    if (sources.isEmpty()) {
                        return "抱歉，当前 AI 服务暂时不可用，且未检索到相关文档。请稍后重试。";
                    }
                    StringBuilder fallback = new StringBuilder();
                    fallback.append("（AI 服务暂时不可用，以下为检索到的相关文档摘要）\n\n");
                    for (int i = 0; i < Math.min(sources.size(), 5); i++) {
                        Map<String, Object> src = sources.get(i);
                        String fileName = String.valueOf(src.getOrDefault("fileName", "未知文档"));
                        String excerpt = String.valueOf(src.getOrDefault("excerpt", ""));
                        fallback.append("  ").append(fileName);
                        if (!excerpt.isEmpty() && !excerpt.equals("null")) {
                            fallback.append("\n   ").append(excerpt.length() > 200 ? excerpt.substring(0, 200) + "..." : excerpt);
                        }
                        fallback.append("\n\n");
                    }
                    fallback.append("以上是为您找到的相关文档，AI 问答功能恢复后将提供更精准的回答。");
                    return fallback.toString();
                }
        );

        long responseTime = System.currentTimeMillis() - startTime;
        try {
            statisticsService.logQuery(userId, query, (int) responseTime, retrievedDocuments.size());
        } catch (Exception e) {
            log.warn("Failed to log query statistics", e);
        }

        boolean usedFallback = llmFallback[0]
                || resilienceManager.getCircuitState("vector-search") != CircuitBreaker.State.CLOSED;
        return new RagResult(response, sources, usedFallback);
    }

    public String chat(String query, Long userId) {
        return chatWithSources(query, userId).answer();
    }

    private List<Document> retrieveRelevantDocuments(String query, Long userId, boolean premiumUser) {
        int vectorDepth = premiumUser ? 24 : 16;
        int keywordDepth = premiumUser ? 10 : 8;
        int finalLimit = premiumUser ? 8 : 6;

        List<String> keywords = extractKeywords(query);
        LinkedHashMap<String, Candidate> candidates = new LinkedHashMap<>();

        // Resilient vector search: retry + circuit breaker + FULLTEXT fallback
        ResilienceManager.ResilientSearchResult vectorResult = resilienceManager.executeVectorSearch(
                query,
                () -> vectorStoreService.search(query, vectorDepth),
                () -> fulltextFallbackSearch(query, userId, vectorDepth)
        );

        List<Document> vectorDocuments = vectorResult.documents();
        if (vectorResult.fallback()) {
            log.info("[RagService] Using FULLTEXT fallback search (circuit: {})", vectorResult.circuitState());
        }

        int vectorRank = 0;
        for (Document doc : filterAccessibleDocuments(vectorDocuments, userId)) {
            rememberCandidate(candidates, doc, true, vectorRank++);
        }

        int keywordRank = 0;
        for (Document doc : keywordSearch(query, userId, keywordDepth)) {
            rememberCandidate(candidates, doc, false, keywordRank++);
        }

        List<Candidate> reranked = candidates.values().stream()
                .peek(candidate -> candidate.score = scoreCandidate(candidate, query, keywords, vectorDepth, keywordDepth))
                .sorted(Comparator.comparingDouble(Candidate::score).reversed())
                .toList();

        List<Document> selected = new ArrayList<>();
        Map<Long, Integer> perDocumentCounter = new HashMap<>();
        for (Candidate candidate : reranked) {
            Long docId = asLong(candidate.document().getMetadata().get("documentId"));
            if (docId != null && perDocumentCounter.getOrDefault(docId, 0) >= 2) {
                continue;
            }
            // Embed score into document metadata so buildSourceEntry can expose it
            candidate.document().getMetadata().put("relevanceScore", candidate.score);
            selected.add(candidate.document());
            if (docId != null) {
                perDocumentCounter.put(docId, perDocumentCounter.getOrDefault(docId, 0) + 1);
            }
            if (selected.size() >= finalLimit) {
                break;
            }
        }

        return selected;
    }

    private void rememberCandidate(
            LinkedHashMap<String, Candidate> candidates,
            Document document,
            boolean vectorCandidate,
            int rank
    ) {
        String key = buildMergeKey(document);
        Candidate candidate = candidates.computeIfAbsent(key, ignored -> new Candidate(document));
        if (vectorCandidate) {
            candidate.vectorRank = Math.min(candidate.vectorRank, rank);
        } else {
            candidate.keywordRank = Math.min(candidate.keywordRank, rank);
        }
    }

    private List<Document> filterAccessibleDocuments(List<Document> candidates, Long userId) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        Set<Long> ids = candidates.stream()
                .map(doc -> asLong(doc.getMetadata().get("documentId")))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, MultimodalDocument> docsById = documentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(MultimodalDocument::getId, doc -> doc));

        List<Document> filtered = new ArrayList<>();
        for (Document candidate : candidates) {
            Long docId = asLong(candidate.getMetadata().get("documentId"));
            if (docId == null) {
                continue;
            }
            MultimodalDocument stored = docsById.get(docId);
            if (stored == null) {
                continue;
            }
            boolean accessible = "COMPLETED".equalsIgnoreCase(stored.getStatus())
                    && (Objects.equals(stored.getOwnerId(), userId) || stored.isShared());
            if (accessible) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }

    /**
     * FULLTEXT fallback search when vector DB is unavailable.
     * Converts query terms to MySQL FULLTEXT boolean mode syntax.
     */
    private List<Document> fulltextFallbackSearch(String query, Long userId, int limit) {
        try {
            List<String> keywords = extractKeywords(query);
            if (keywords.isEmpty()) {
                return List.of();
            }

            // Build FULLTEXT boolean mode query: each term OR'd together
            String fulltextQuery = keywords.stream()
                    .map(kw -> "+" + kw.replace("+", "").replace("-", "").replace("*", ""))
                    .limit(5) // Cap terms to avoid overly complex queries
                    .collect(Collectors.joining(" "));

            List<MultimodalDocument> docs = documentRepository.fulltextSearchAccessible(
                    userId, fulltextQuery, limit);

            return docs.stream()
                    .map(doc -> {
                        Map<String, Object> metadata = new LinkedHashMap<>();
                        metadata.put("documentId", doc.getId());
                        metadata.put("userId", doc.getOwnerId());
                        metadata.put("fileName", doc.getFileName());
                        metadata.put("fileType", doc.getFileType());
                        metadata.put("sourceType", "fulltext-fallback");
                        String snippet = doc.getExtractedContent() != null && doc.getExtractedContent().length() > 420
                                ? doc.getExtractedContent().substring(0, 420) + "..."
                                : doc.getExtractedContent();
                        return new Document(snippet != null ? snippet : "", metadata);
                    })
                    .toList();
        } catch (Exception e) {
            log.error("[RagService] FULLTEXT fallback search failed: {}", e.getMessage());
            return List.of();
        }
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
                .toList();
    }

    private List<String> extractKeywords(String query) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        if (query == null) {
            return List.of();
        }

        String normalized = normalize(query);
        if (normalized.isBlank()) {
            return List.of();
        }

        keywords.add(normalized);
        for (String token : normalized.split(KEYWORD_SPLIT_REGEX)) {
            String trimmed = token.trim();
            if (trimmed.length() >= 2) {
                keywords.add(trimmed);
                if (isMostlyCjk(trimmed) && trimmed.length() >= 6) {
                    for (int i = 0; i <= trimmed.length() - 3 && i < 8; i++) {
                        keywords.add(trimmed.substring(i, i + 3));
                    }
                }
            }
        }
        return new ArrayList<>(keywords);
    }

    private double scoreCandidate(
            Candidate candidate,
            String query,
            List<String> keywords,
            int vectorDepth,
            int keywordDepth
    ) {
        Document document = candidate.document();
        String content = normalize(document.getContent());
        String fileName = normalize(String.valueOf(document.getMetadata().getOrDefault("fileName", "")));
        String headingPath = normalize(String.valueOf(document.getMetadata().getOrDefault("headingPath", "")));
        String normalizedQuery = normalize(query);

        double score = 0;
        if (candidate.vectorRank != Integer.MAX_VALUE) {
            score += (vectorDepth - candidate.vectorRank) * 0.9;
        }
        if (candidate.keywordRank != Integer.MAX_VALUE) {
            score += (keywordDepth - candidate.keywordRank) * 1.1;
        }
        if (!normalizedQuery.isBlank() && content.contains(normalizedQuery)) {
            score += 10.0;
        }

        int matchedKeywords = 0;
        for (String keyword : keywords) {
            if (keyword.length() < 2) {
                continue;
            }
            int contentHits = countOccurrences(content, keyword);
            if (contentHits > 0) {
                matchedKeywords++;
                score += 2.5 + Math.min(contentHits, 3);
            }
            if (fileName.contains(keyword)) {
                score += 4.0;
            }
            if (headingPath.contains(keyword)) {
                score += 3.0;
            }
        }

        if (!keywords.isEmpty() && matchedKeywords >= Math.max(1, keywords.size() / 2)) {
            score += 4.0;
        }

        score += bigramOverlapScore(normalizedQuery, content);
        return score;
    }

    private double bigramOverlapScore(String query, String content) {
        String compactQuery = query.replaceAll("\\s+", "");
        if (compactQuery.length() < 2 || content.isBlank()) {
            return 0;
        }
        Set<String> grams = new LinkedHashSet<>();
        for (int i = 0; i < compactQuery.length() - 1; i++) {
            grams.add(compactQuery.substring(i, i + 2));
        }
        int overlap = 0;
        for (String gram : grams) {
            if (content.contains(gram)) {
                overlap++;
            }
        }
        return overlap * 0.6;
    }

    private String buildMergeKey(Document doc) {
        Object docId = doc.getMetadata().get("documentId");
        Object chunkIndex = doc.getMetadata().get("chunkIndex");
        return String.valueOf(docId) + "::" + String.valueOf(chunkIndex) + "::" + doc.getContent();
    }

    private String buildContextSegment(Document doc) {
        String content = mergeChunkWindowIfAvailable(doc);
        if (content.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("[Source]").append('\n');
        builder.append("File: ").append(doc.getMetadata().getOrDefault("fileName", "unknown")).append('\n');
        builder.append("Type: ").append(doc.getMetadata().getOrDefault("fileType", "unknown")).append('\n');
        Object heading = doc.getMetadata().get("headingPath");
        if (heading != null && !heading.toString().isBlank()) {
            builder.append("Section: ").append(heading).append('\n');
        }
        builder.append(content.trim());
        return builder.toString();
    }

    private String mergeChunkWindowIfAvailable(Document doc) {
        Long documentId = asLong(doc.getMetadata().get("documentId"));
        Integer chunkIndex = asInt(doc.getMetadata().get("chunkIndex"));
        List<Document> window = vectorStoreService.loadChunkWindow(documentId, chunkIndex, 1);
        if (window.isEmpty()) {
            return doc.getContent() == null ? "" : doc.getContent();
        }
        return window.stream()
                .map(Document::getContent)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("\n\n"));
    }

    private Map<String, Object> buildSourceEntry(Document doc) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("docId", asLong(doc.getMetadata().get("documentId")));
        source.put("fileName", doc.getMetadata().getOrDefault("fileName", "unknown"));
        source.put("fileType", doc.getMetadata().getOrDefault("fileType", "unknown"));
        if (doc.getMetadata().containsKey("headingPath")) {
            source.put("section", doc.getMetadata().get("headingPath"));
        }
        source.put("excerpt", buildExcerpt(doc.getContent()));
        // Pass raw relevance score (will be normalized later)
        Object rawScore = doc.getMetadata().get("relevanceScore");
        if (rawScore instanceof Number) {
            source.put("score", ((Number) rawScore).doubleValue());
        }
        return source;
    }

    private boolean isPremiumUser(Long userId) {
        return userRepository.findById(userId)
                .map(User::getRoles)
                .stream()
                .flatMap(Set::stream)
                .anyMatch(role -> "PREMIUM".equalsIgnoreCase(role.getName()));
    }

    private int scoreDocument(MultimodalDocument doc, String keyword) {
        String normalizedKeyword = normalize(keyword);
        int score = 0;
        score += contains(doc.getFileName(), normalizedKeyword) ? 12 : 0;
        score += contains(doc.getTags(), normalizedKeyword) ? 8 : 0;
        score += countOccurrences(normalize(doc.getExtractedContent()), normalizedKeyword) * 2;
        score += doc.isShared() ? 1 : 0;
        return score;
    }

    private boolean contains(String text, String keyword) {
        return normalize(text).contains(keyword);
    }

    private int countOccurrences(String text, String keyword) {
        if (text == null || keyword == null || keyword.isBlank()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) >= 0) {
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
        metadata.put("userId", doc.getOwnerId());
        metadata.put("fileName", doc.getFileName());
        metadata.put("fileType", doc.getFileType());
        metadata.put("sourceType", "keyword");
        return new Document(snippet, metadata);
    }

    private String buildSnippet(String content, Set<String> highlights) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String original = content.trim();
        String lower = original.toLowerCase(Locale.ROOT);
        for (String keyword : highlights) {
            String loweredKeyword = keyword.toLowerCase(Locale.ROOT).trim();
            int index = lower.indexOf(loweredKeyword);
            if (index >= 0) {
                int start = Math.max(0, index - 160);
                int end = Math.min(original.length(), index + Math.max(320, keyword.length() + 160));
                String snippet = original.substring(start, end).trim();
                if (start > 0) {
                    snippet = "..." + snippet;
                }
                if (end < original.length()) {
                    snippet = snippet + "...";
                }
                return snippet;
            }
        }
        return original.length() > 420 ? original.substring(0, 420) + "..." : original;
    }

    private String buildExcerpt(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        int marker = content.indexOf("Content:\n");
        String excerpt = marker >= 0 ? content.substring(marker + "Content:\n".length()) : content;
        excerpt = excerpt.trim();
        return excerpt.length() > 160 ? excerpt.substring(0, 160) + "..." : excerpt;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isMostlyCjk(String token) {
        long visible = token.chars().filter(ch -> !Character.isWhitespace(ch)).count();
        if (visible == 0) {
            return false;
        }
        long cjk = token.chars().filter(ch -> ch >= 0x2E80 && ch <= 0x9FFF).count();
        return cjk * 1.0 / visible > 0.5;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer asInt(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class Candidate {
        private final Document document;
        private int vectorRank = Integer.MAX_VALUE;
        private int keywordRank = Integer.MAX_VALUE;
        private double score;

        private Candidate(Document document) {
            this.document = document;
        }

        private Document document() {
            return document;
        }

        private double score() {
            return score;
        }
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
