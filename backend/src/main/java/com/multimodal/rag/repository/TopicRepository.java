package com.multimodal.rag.repository;

import com.multimodal.rag.model.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 话题 Repository
 */
@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    /**
     * 获取用户创建的所有话题
     */
    List<Topic> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    /**
     * 获取所有公开话题
     */
    List<Topic> findByIsPublicTrueOrderByCreatedAtDesc();

    /**
     * 获取用户的父话题（parentId 为空）
     */
    List<Topic> findByOwnerIdAndParentIdIsNullOrderByCreatedAtDesc(Long ownerId);

    /**
     * 获取指定父话题下的子话题
     */
    List<Topic> findByParentIdOrderByCreatedAtDesc(Long parentId);

    /**
     * 检查话题名称是否已存在（同一用户下）
     */
    boolean existsByOwnerIdAndName(Long ownerId, String name);

    /**
     * 根据 ownerId 和 name 查找话题
     */
    List<Topic> findByOwnerIdAndName(Long ownerId, String name);

    /**
     * 搜索公开话题（按关键词）
     */
    @Query("SELECT t FROM Topic t " +
           "WHERE t.isPublic = true " +
           "AND (:keyword IS NULL OR :keyword = '' " +
           "OR LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY t.createdAt DESC")
    Page<Topic> searchPublicTopics(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 分页获取公开话题
     */
    @Query("SELECT t FROM Topic t WHERE t.isPublic = true ORDER BY t.createdAt DESC")
    Page<Topic> findPublicTopicsPaged(Pageable pageable);
}
