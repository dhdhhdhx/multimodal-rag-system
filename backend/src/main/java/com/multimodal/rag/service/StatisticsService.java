package com.multimodal.rag.service;

import com.multimodal.rag.model.DocumentAccessLog;
import com.multimodal.rag.model.QueryLog;
import com.multimodal.rag.repository.DocumentAccessLogRepository;
import com.multimodal.rag.repository.QueryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {
    
    private final QueryLogRepository queryLogRepository;
    private final DocumentAccessLogRepository accessLogRepository;
    
    @Transactional
    public void logQuery(Long userId, String queryText, Integer responseTimeMs, Integer documentCount) {
        QueryLog log = new QueryLog();
        log.setUserId(userId);
        log.setQueryText(queryText);
        log.setResponseTimeMs(responseTimeMs);
        log.setDocumentCount(documentCount);
        queryLogRepository.save(log);
    }
    
    @Transactional
    public void logDocumentAccess(Long documentId, Long userId, DocumentAccessLog.AccessType accessType) {
        DocumentAccessLog log = new DocumentAccessLog();
        log.setDocumentId(documentId);
        log.setUserId(userId);
        log.setAccessType(accessType);
        accessLogRepository.save(log);
    }
    
    public List<QueryLog> getRecentQueries(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return queryLogRepository.findByDateRange(since, LocalDateTime.now());
    }
    
    public List<QueryLog> getUserQueries(Long userId) {
        return queryLogRepository.findByUserId(userId);
    }
    
    public Map<String, Object> getQueryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQueries", queryLogRepository.count());
        stats.put("averageResponseTime", queryLogRepository.getAverageResponseTime());
        return stats;
    }
    
    public List<Map<String, Object>> getHotDocuments(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> results = accessLogRepository.findHotDocuments(since);
        
        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("documentId", row[0]);
                    map.put("accessCount", row[1]);
                    return map;
                })
                .toList();
    }
    
    public List<DocumentAccessLog> getDocumentAccessHistory(Long documentId) {
        return accessLogRepository.findByDocumentId(documentId);
    }
}
