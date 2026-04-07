package com.multimodal.rag.service;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.QueryLog;
import com.multimodal.rag.repository.MultimodalDocumentRepository;
import com.multimodal.rag.repository.QueryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MultimodalDocumentRepository documentRepository;
    private final QueryLogRepository queryLogRepository;
    private final MultimodalEmbeddingService multimodalService;
    
    private static final long DEFAULT_QUOTA = 100 * 1024 * 1024; // 100 MB

    public Map<String, Object> getUserStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<MultimodalDocument> userDocs = documentRepository.findByUser_Id(userId);
        
        // 1. Storage Quota
        long usedStorage = userDocs.stream()
                .mapToLong(MultimodalDocument::getFileSize)
                .sum();
        
        stats.put("storage", Map.of(
            "used", usedStorage,
            "total", DEFAULT_QUOTA,
            "percentage", Math.round((double) usedStorage / DEFAULT_QUOTA * 100)
        ));
        
        // 2. Modality Breakdown
        Map<String, Long> modalityBreakdown = userDocs.stream()
                .collect(Collectors.groupingBy(doc -> 
                    doc.getFileType() != null ? doc.getFileType().toUpperCase() : "UNKNOWN", 
                    Collectors.counting()));
        
        stats.put("modalities", modalityBreakdown);
        
        // 3. Document Status
        Map<String, Long> statusBreakdown = userDocs.stream()
                .collect(Collectors.groupingBy(MultimodalDocument::getStatus, Collectors.counting()));
        stats.put("status", statusBreakdown);
        
        // 4. Query Activity
        List<QueryLog> recentQueries = queryLogRepository.findByUserId(userId);
        stats.put("totalQueries", recentQueries.size());
        
        // 5. System Status (Python service health)
        stats.put("pythonService", multimodalService.isPythonServiceAvailable());
        
        return stats;
    }
}
