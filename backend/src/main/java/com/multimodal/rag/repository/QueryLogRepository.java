package com.multimodal.rag.repository;

import com.multimodal.rag.model.QueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueryLogRepository extends JpaRepository<QueryLog, Long> {
    List<QueryLog> findByUserId(Long userId);
    
    @Query("SELECT q FROM QueryLog q WHERE q.createdAt BETWEEN :startDate AND :endDate ORDER BY q.createdAt DESC")
    List<QueryLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT AVG(q.responseTimeMs) FROM QueryLog q")
    Double getAverageResponseTime();
}
