package com.multimodal.rag.repository;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.TopicDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 话题文档关联 Repository
 */
@Repository
public interface TopicDocumentRepository extends JpaRepository<TopicDocument, Long> {

    /**
     * 获取话题下的所有文档关联
     */
    List<TopicDocument> findByTopicIdOrderByAddedAtDesc(Long topicId);

    /**
     * 获取文档所在的所有话题
     */
    List<TopicDocument> findByDocumentId(Long documentId);

    /**
     * 检查文档是否已添加到话题
     */
    Optional<TopicDocument> findByTopicIdAndDocumentId(Long topicId, Long documentId);

    /**
     * 删除话题下的所有文档关联
     */
    void deleteByTopicId(Long topicId);

    /**
     * 从话题中移除指定文档
     */
    void deleteByTopicIdAndDocumentId(Long topicId, Long documentId);

    /**
     * 统计话题下的文档数量
     */
    long countByTopicId(Long topicId);

    /**
     * 分页获取话题下的文档（支持关键词搜索）
     */
    @Query("SELECT d FROM MultimodalDocument d " +
           "INNER JOIN TopicDocument td ON d.id = td.documentId " +
           "WHERE td.topicId = :topicId " +
           "AND (:keyword IS NULL OR :keyword = '' " +
           "OR LOWER(d.fileName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(COALESCE(d.tags, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY td.addedAt DESC")
    Page<MultimodalDocument> findDocumentsByTopicIdPaged(
            @Param("topicId") Long topicId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
