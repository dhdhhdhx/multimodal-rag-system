package com.multimodal.rag.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multimodal.rag.service.resilience.ResilienceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStoreService {

    private final VectorStore vectorStore;
    private final JdbcTemplate vectorJdbcTemplate;
    private final ResilienceManager resilienceManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void addDocument(String content, Map<String, Object> metadata) {
        log.info("Adding content to vector store with metadata: {}", metadata);
        vectorStore.add(List.of(new Document(content, metadata)));
    }

    /**
     * Spring AI M4 still owns the final embedding write path in this project.
     * We keep the method for compatibility and store the retrieval text plus metadata.
     */
    public void addDocumentWithEmbedding(List<Double> embedding, String content, Map<String, Object> metadata) {
        int dimension = embedding == null ? 0 : embedding.size();
        log.info("Adding document with compatible fallback storage, embedding dimension: {}, metadata: {}", dimension, metadata);
        vectorStore.add(List.of(new Document(content, metadata)));
    }

    public List<Document> search(String query, int topK) {
        return search(query, topK, null);
    }

    public List<Document> search(String query, int topK, Long userId) {
        log.info("Searching vector store for query: '{}', topK: {}, userId: {}", query, topK, userId);

        SearchRequest request = SearchRequest.query(query).withTopK(topK);
        if (userId != null) {
            String filterExpr = "userId == '" + userId + "'";
            request = request.withFilterExpression(filterExpr);
            log.debug("Using filter expression: {}", filterExpr);
        }

        final SearchRequest finalRequest = request;
        return resilienceManager.executeWithRetry("vector-store", () ->
                vectorStore.similaritySearch(finalRequest)
        );
    }

    public List<Document> loadChunkWindow(Long documentId, Integer chunkIndex, int radius) {
        if (documentId == null || chunkIndex == null || radius < 1) {
            return List.of();
        }

        int start = Math.max(0, chunkIndex - radius);
        int end = chunkIndex + radius;

        try {
            return vectorJdbcTemplate.query(
                    """
                    SELECT content, metadata
                    FROM vector_store
                    WHERE metadata->>'documentId' = ?
                      AND metadata ? 'chunkIndex'
                      AND CAST(metadata->>'chunkIndex' AS INTEGER) BETWEEN ? AND ?
                    ORDER BY CAST(metadata->>'chunkIndex' AS INTEGER)
                    """,
                    ps -> {
                        ps.setString(1, documentId.toString());
                        ps.setInt(2, start);
                        ps.setInt(3, end);
                    },
                    (rs, rowNum) -> new Document(
                            rs.getString("content"),
                            parseMetadata(rs.getString("metadata"))
                    )
            );
        } catch (Exception e) {
            log.debug("Failed to load chunk window for documentId={}, chunkIndex={}: {}", documentId, chunkIndex, e.getMessage());
            return List.of();
        }
    }

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

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.debug("Failed to parse vector metadata JSON: {}", e.getMessage());
            return Map.of();
        }
    }
}
