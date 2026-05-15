package com.multimodal.rag.repository;

import com.multimodal.rag.model.MultimodalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface MultimodalDocumentRepository extends JpaRepository<MultimodalDocument, Long>, JpaSpecificationExecutor<MultimodalDocument> {
    List<MultimodalDocument> findByUser_Id(Long userId);
    List<MultimodalDocument> findByUser_IdOrSharedTrue(Long userId);
    Page<MultimodalDocument> findByUser_Id(Long userId, Pageable pageable);

    // Combined query for personalized discovery: User's own docs + Public/Seed docs
    Page<MultimodalDocument> findByUser_IdOrSharedTrue(Long userId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM MultimodalDocument d WHERE d.shared = true ORDER BY d.uploadTime DESC")
    List<MultimodalDocument> findTopFeatured(Pageable pageable);

    List<MultimodalDocument> findTop5ByUser_IdOrderByUploadTimeDesc(Long userId);

    Optional<MultimodalDocument> findByIdAndUser_Id(Long id, Long userId);

    // Public document queries for blog-style pages — sorted by viewCount desc then uploadTime desc
    @org.springframework.data.jpa.repository.Query("SELECT d FROM MultimodalDocument d WHERE d.shared = true AND d.status = 'COMPLETED' ORDER BY COALESCE(d.viewCount, 0) DESC, d.uploadTime DESC")
    Page<MultimodalDocument> findPublicDocsSorted(Pageable pageable);

    Page<MultimodalDocument> findBySharedTrueAndStatusOrderByUploadTimeDesc(String status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM MultimodalDocument d WHERE d.shared = true AND d.status = 'COMPLETED' AND d.tags LIKE %:tag% ORDER BY COALESCE(d.viewCount, 0) DESC, d.uploadTime DESC")
    Page<MultimodalDocument> findPublicByTag(@org.springframework.data.repository.query.Param("tag") String tag, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            SELECT d FROM MultimodalDocument d
            WHERE d.shared = true AND d.status = 'COMPLETED'
              AND (
                LOWER(d.fileName) LIKE LOWER(CONCAT('%', :kw, '%'))
                OR LOWER(COALESCE(d.tags, '')) LIKE LOWER(CONCAT('%', :kw, '%'))
                OR LOWER(COALESCE(d.extractedContent, '')) LIKE LOWER(CONCAT('%', :kw, '%'))
              )
            ORDER BY
              CASE
                WHEN LOWER(d.fileName) LIKE LOWER(CONCAT('%', :kw, '%')) THEN 0
                WHEN LOWER(COALESCE(d.tags, '')) LIKE LOWER(CONCAT('%', :kw, '%')) THEN 1
                ELSE 2
              END,
              COALESCE(d.viewCount, 0) DESC,
              d.uploadTime DESC
            """)
    Page<MultimodalDocument> searchPublic(@org.springframework.data.repository.query.Param("kw") String keyword, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            SELECT d FROM MultimodalDocument d
            WHERE d.status = 'COMPLETED'
              AND (d.user.id = :userId OR d.shared = true)
              AND (
                LOWER(d.fileName) LIKE LOWER(CONCAT('%', :kw, '%'))
                OR LOWER(COALESCE(d.tags, '')) LIKE LOWER(CONCAT('%', :kw, '%'))
                OR LOWER(COALESCE(d.extractedContent, '')) LIKE LOWER(CONCAT('%', :kw, '%'))
              )
            ORDER BY
              CASE
                WHEN LOWER(d.fileName) LIKE LOWER(CONCAT('%', :kw, '%')) THEN 0
                WHEN LOWER(COALESCE(d.tags, '')) LIKE LOWER(CONCAT('%', :kw, '%')) THEN 1
                ELSE 2
              END,
              COALESCE(d.viewCount, 0) DESC,
              d.uploadTime DESC
            """)
    Page<MultimodalDocument> searchAccessible(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("kw") String keyword,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT d.tags FROM MultimodalDocument d WHERE d.shared = true AND d.status = 'COMPLETED' AND d.tags IS NOT NULL AND d.tags <> ''")
    List<String> findAllPublicTags();

    long countBySharedTrueAndStatus(String status);

    // Hot/popular public documents for recommendations
    @org.springframework.data.jpa.repository.Query("SELECT d FROM MultimodalDocument d WHERE d.shared = true AND d.status = 'COMPLETED' ORDER BY COALESCE(d.viewCount, 0) DESC")
    List<MultimodalDocument> findHotPublicDocs(Pageable pageable);

    // Direct owner ID query to avoid lazy-loading issues with ByteBuddyInterceptor
    @org.springframework.data.jpa.repository.Query("SELECT d.user.id FROM MultimodalDocument d WHERE d.id = :id")
    Long findOwnerIdById(@org.springframework.data.repository.query.Param("id") Long id);

    // Atomic view count increment
    @Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE MultimodalDocument d SET d.viewCount = COALESCE(d.viewCount, 0) + :increment WHERE d.id = :id")
    int incrementViewCountBy(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("increment") Long increment);
}
