package com.multimodal.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagService {

    private final VectorStoreService vectorStoreService;
    private final ChatClient.Builder chatClientBuilder;
    private final StatisticsService statisticsService;

    private static final String SYSTEM_PROMPT = """
        您是一个专业的知识库助手。请根据下面提供的【上下文】内容来回答用户的问题。
        如果上下文内容不足以回答问题，请诚实地告诉用户你不知道，不要胡乱猜测。
        保持回答简洁、专业，直接回答问题核心。

        【上下文】：
        {context}
        """;

    private static final int MAX_CONTEXT_LENGTH = 6000;

    /**
     * Result DTO for RAG response with source tracing.
     */
    public record RagResult(String answer, List<Map<String, Object>> sources) {}

    /**
     * Executes the RAG pipeline and returns answer + source references.
     */
    public RagResult chatWithSources(String query, Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("Processing RAG chat request for query: '{}'", query);

        // Step 1: SEARCH — also search public documents (userId=null for shared)
        List<Document> userDocs = vectorStoreService.search(query, 5, userId);
        List<Document> publicDocs = vectorStoreService.search(query, 3, null);

        // Merge, deduplicate by content
        Set<String> seen = new HashSet<>();
        List<Document> similarDocuments = new ArrayList<>();
        for (Document doc : userDocs) {
            if (seen.add(doc.getContent())) similarDocuments.add(doc);
        }
        for (Document doc : publicDocs) {
            if (seen.add(doc.getContent())) similarDocuments.add(doc);
        }
        // Limit to top 5
        if (similarDocuments.size() > 5) {
            similarDocuments = similarDocuments.subList(0, 5);
        }

        // Build context string
        StringBuilder contextBuilder = new StringBuilder();
        List<Map<String, Object>> sources = new ArrayList<>();
        Set<Long> seenDocIds = new HashSet<>();

        for (int docIdx = 0; docIdx < similarDocuments.size(); docIdx++) {
            Document doc = similarDocuments.get(docIdx);
            String segment = doc.getContent();
            if (contextBuilder.length() + segment.length() + 2 > MAX_CONTEXT_LENGTH) {
                int remaining = MAX_CONTEXT_LENGTH - contextBuilder.length() - 5;
                if (remaining > 100) {
                    contextBuilder.append("\n\n").append(segment, 0, remaining).append("...");
                }
                break;
            }
            if (contextBuilder.length() > 0) contextBuilder.append("\n\n");
            contextBuilder.append(segment);

            // Collect source references
            Map<String, Object> meta = doc.getMetadata();
            Object docIdObj = meta.get("documentId");
            if (docIdObj != null) {
                Long docId = Long.parseLong(docIdObj.toString());
                if (seenDocIds.add(docId)) {
                    Map<String, Object> source = new LinkedHashMap<>();
                    source.put("docId", docId);
                    source.put("fileName", meta.getOrDefault("fileName", "未知文件"));
                    source.put("fileType", meta.getOrDefault("fileType", "unknown"));
                    source.put("modality", meta.getOrDefault("modality", "TEXT"));
                    // Relevance score: decay from 0.95 based on rank
                    double score = Math.round((0.95 - docIdx * 0.08) * 100.0) / 100.0;
                    source.put("score", Math.max(score, 0.3));
                    // Extract excerpt — provide enough for frontend to display
                    String excerpt = segment.length() > 200 ? segment.substring(0, 200) + "..." : segment;
                    source.put("excerpt", excerpt);
                    sources.add(source);
                }
            }
        }
        String context = contextBuilder.toString();

        log.info("RAG Context: found {} segments, {} unique sources, total length: {}",
                similarDocuments.size(), sources.size(), context.length());

        // Step 2 & 3: PROMPT & GENERATE
        ChatClient chatClient = chatClientBuilder.build();
        String systemPromptWithContext = SYSTEM_PROMPT.replace("{context}",
                context.isEmpty() ? "（无相关背景资料）" : context);

        // Retry LLM call on transient network errors
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
                    try { Thread.sleep(1000L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    throw e;
                }
            }
        }

        // Log query statistics
        long responseTime = System.currentTimeMillis() - startTime;
        try {
            statisticsService.logQuery(userId, query, (int) responseTime, similarDocuments.size());
        } catch (Exception e) {
            log.warn("Failed to log query statistics", e);
        }

        return new RagResult(response, sources);
    }

    /**
     * Legacy method — returns answer only (for backward compatibility).
     */
    public String chat(String query, Long userId) {
        return chatWithSources(query, userId).answer();
    }
}
