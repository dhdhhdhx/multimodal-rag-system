package com.multimodal.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStoreService {

    private final VectorStore vectorStore;
    private final JdbcTemplate vectorJdbcTemplate;

    public void addDocument(String content, Map<String, Object> metadata) {
        log.info("Adding content to vector store with metadata: {}", metadata);
        Document doc = new Document(content, metadata);
        vectorStore.add(List.of(doc));
    }

    /**
     * Add document with metadata (Python service integration placeholder)
     * 
     * Note: Spring AI 1.0.0-M4 doesn't support custom embeddings directly.
     * For now, we store the content and let Spring AI embed it.
     * Future: Upgrade to newer Spring AI version with add(List<Document>, List<float[]>) support
     * 
     * @param embedding Pre-computed embedding (currently unused, for future use)
     * @param metadata Document metadata including modality type
     */
    public void addDocumentWithEmbedding(List<Double> embedding, String content, Map<String, Object> metadata) {
        log.info("Adding document with metadata (embedding dimension: {}), content length: {}, metadata: {}", 
            embedding.size(), content.length(), metadata);
        log.warn("Custom embeddings not yet supported in Spring AI 1.0.0-M4. Using default embedding.");
        
        Document doc = new Document(content, metadata);
        vectorStore.add(List.of(doc));
    }

    public List<Document> search(String query, int topK) {
        return search(query, topK, null);
    }

    public List<Document> search(String query, int topK, Long userId) {
        log.info("Searching vector store for query: '{}', topK: {}, userId: {}", query, topK, userId);

        SearchRequest request = SearchRequest.query(query).withTopK(topK);

        if (userId != null) {
            // Filter by userId in metadata.
            // PGVector stores metadata as JSONB. A Long value becomes a JSON number (e.g. 42),
            // but Spring AI M4 filter DSL converts expressions to SQL using the text operator ->>,
            // so we compare against the string representation of the userId.
            String filterExpr = "userId == '" + userId + "'";
            request = request.withFilterExpression(filterExpr);
            log.debug("Using filter expression: {}", filterExpr);
        }

        // Retry up to 3 times on transient I/O errors (e.g. embedding API connection reset)
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return vectorStore.similaritySearch(request);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                boolean isTransient = msg.contains("Connection reset")
                        || msg.contains("I/O error")
                        || msg.contains("Connection refused")
                        || msg.contains("Read timed out");
                if (isTransient && attempt < maxRetries) {
                    log.warn("Transient error on search attempt {}/{}: {}. Retrying...", attempt, maxRetries, msg);
                    try { Thread.sleep(500L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    log.error("Vector search failed after {} attempts: {}", attempt, msg);
                    throw e;
                }
            }
        }
        return List.of(); // unreachable
    }

    /**
     * Delete all vector rows for a given documentId.
     * Uses direct SQL against the PGVector store table to clean up orphaned embeddings.
     */
    public void deleteByDocumentId(Long documentId) {
        log.info("Deleting vectors for documentId: {}", documentId);
        try {
            int deleted = vectorJdbcTemplate.update(
                "DELETE FROM vector_store WHERE metadata->>'documentId' = ?",
                documentId.toString()
            );
            log.info("Deleted {} vector rows for documentId: {}", deleted, documentId);
        } catch (Exception e) {
            log.warn("Failed to delete vectors for documentId {}: {}", documentId, e.getMessage());
        }
    }
}
