package com.multimodal.rag.repository;

import com.multimodal.rag.model.DocumentAccessLog;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentAccessLogRepository extends JpaRepository<DocumentAccessLog, Long> {
    
    @Query("SELECT d.documentId FROM DocumentAccessLog d WHERE d.userId = :userId AND d.accessType = 'VIEW' ORDER BY d.createdAt DESC")
    List<Long> findRecentViewedDocumentIds(Long userId, Pageable pageable);
    
    List<DocumentAccessLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT d.documentId, COUNT(d) as count FROM DocumentAccessLog d WHERE d.createdAt >= :since GROUP BY d.documentId ORDER BY count DESC")
    List<Object[]> findHotDocuments(LocalDateTime since);

    List<DocumentAccessLog> findByDocumentId(Long documentId);

    @Query("SELECT DISTINCT d.userId FROM DocumentAccessLog d WHERE d.documentId IN :documentIds AND d.userId != :userId")
    List<Long> findOtherUsersBySharedDocuments(List<Long> documentIds, Long userId);

    @Query("SELECT d.documentId, COUNT(d) as freq FROM DocumentAccessLog d WHERE d.userId IN :userIds AND d.documentId NOT IN :excludedDocIds GROUP BY d.documentId ORDER BY freq DESC")
    List<Object[]> findRecommendedDocumentIdsBySimilarUsers(List<Long> userIds, List<Long> excludedDocIds, Pageable pageable);
}
