package com.multimodal.rag.repository;

import com.multimodal.rag.model.TopicSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 话题订阅 Repository
 */
public interface TopicSubscriptionRepository extends JpaRepository<TopicSubscription, Long> {

    /**
     * 查找用户是否已订阅指定话题
     */
    Optional<TopicSubscription> findByTopicIdAndUserId(Long topicId, Long userId);

    /**
     * 检查用户是否已订阅指定话题
     */
    boolean existsByTopicIdAndUserId(Long topicId, Long userId);

    /**
     * 删除用户对指定话题的订阅
     */
    void deleteByTopicIdAndUserId(Long topicId, Long userId);

    /**
     * 获取话题的所有订阅者（按订阅时间倒序）
     */
    List<TopicSubscription> findByTopicIdOrderByCreatedAtDesc(Long topicId);

    /**
     * 获取话题的所有订阅者（分页）
     */
    Page<TopicSubscription> findByTopicId(Long topicId, Pageable pageable);

    /**
     * 获取用户的所有订阅（按订阅时间倒序）
     */
    List<TopicSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 统计话题的订阅数
     */
    long countByTopicId(Long topicId);
}
