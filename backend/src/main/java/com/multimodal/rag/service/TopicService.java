package com.multimodal.rag.service;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.Role;
import com.multimodal.rag.model.Topic;
import com.multimodal.rag.model.TopicDocument;
import com.multimodal.rag.model.TopicSubscription;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.MultimodalDocumentRepository;
import com.multimodal.rag.repository.TopicDocumentRepository;
import com.multimodal.rag.repository.TopicRepository;
import com.multimodal.rag.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 话题服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final TopicDocumentRepository topicDocumentRepository;
    private final MultimodalDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final com.multimodal.rag.repository.TopicSubscriptionRepository topicSubscriptionRepository;

    /**
     * 创建话题
     * 权限检查：USER 角色不能创建话题
     */
    @Transactional
    public Topic createTopic(Topic topic, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查权限：USER 角色不能创建话题
        if (!hasRequiredRole(user, Set.of("PREMIUM", "ADMIN"))) {
            throw new RuntimeException("权限不足：仅 PREMIUM 和 ADMIN 用户可以创建话题");
        }

        // 检查父话题是否存在
        if (topic.getParentId() != null) {
            Topic parentTopic = topicRepository.findById(topic.getParentId())
                    .orElseThrow(() -> new RuntimeException("父话题不存在"));
            // 检查父话题是否也有父话题（只支持一级层级）
            if (parentTopic.getParentId() != null) {
                throw new RuntimeException("只支持一级子话题，无法创建更深层级");
            }
        }

        // 检查同名话题
        if (topicRepository.existsByOwnerIdAndName(userId, topic.getName())) {
            throw new RuntimeException("话题名称已存在");
        }

        topic.setOwnerId(userId);
        topic.setIsPublic(topic.getIsPublic() != null ? topic.getIsPublic() : false);

        Topic savedTopic = topicRepository.save(topic);
        log.info("用户 {} 创建话题: {}", userId, topic.getName());
        return savedTopic;
    }

    /**
     * 更新话题
     * 权限检查：仅创建者或 ADMIN 可操作
     */
    @Transactional
    public Topic updateTopic(Long topicId, Topic update, Long userId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("话题不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 权限检查：仅创建者或 ADMIN 可操作
        if (!topic.getOwnerId().equals(userId) && !hasRole(user, "ADMIN")) {
            throw new RuntimeException("权限不足：只能修改自己创建的话题");
        }

        // 检查同名话题（排除自己）
        List<Topic> existingTopics = topicRepository.findByOwnerIdAndName(userId, update.getName());
        if (!existingTopics.isEmpty() && !existingTopics.get(0).getId().equals(topicId)) {
            throw new RuntimeException("话题名称已存在");
        }

        topic.setName(update.getName());
        topic.setDescription(update.getDescription());

        if (update.getIsPublic() != null) {
            topic.setIsPublic(update.getIsPublic());
        }

        Topic savedTopic = topicRepository.save(topic);
        log.info("用户 {} 更新话题: {}", userId, topic.getName());
        return savedTopic;
    }

    /**
     * 删除话题
     * 权限检查：仅创建者或 ADMIN 可操作
     * 连带删除所有关联
     */
    @Transactional
    public void deleteTopic(Long topicId, Long userId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("话题不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 权限检查：仅创建者或 ADMIN 可操作
        if (!topic.getOwnerId().equals(userId) && !hasRole(user, "ADMIN")) {
            throw new RuntimeException("权限不足：只能删除自己创建的话题");
        }

        // 检查是否有子话题
        List<Topic> childTopics = topicRepository.findByParentIdOrderByCreatedAtDesc(topicId);
        if (!childTopics.isEmpty()) {
            throw new RuntimeException("请先删除子话题后再删除父话题");
        }

        // 删除所有文档关联
        topicDocumentRepository.deleteByTopicId(topicId);

        // 删除话题
        topicRepository.delete(topic);
        log.info("用户 {} 删除话题: {}", userId, topic.getName());
    }

    /**
     * 将文档添加到话题
     */
    @Transactional
    public TopicDocument addDocumentToTopic(Long topicId, Long documentId, Long userId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("话题不存在"));

        MultimodalDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 修复：使用直接查询获取 ownerId，避免懒加载问题
        Long ownerId = documentRepository.findOwnerIdById(documentId);
        if (ownerId == null) {
            throw new RuntimeException("文档所有者信息不存在");
        }

        // 权限检查：只能添加自己的文档，或 ADMIN 可以添加任何文档
        if (!ownerId.equals(userId) && !hasRole(user, "ADMIN")) {
            throw new RuntimeException("权限不足：只能添加自己的文档");
        }

        // 检查是否已添加
        Optional<TopicDocument> existing = topicDocumentRepository.findByTopicIdAndDocumentId(topicId, documentId);
        if (existing.isPresent()) {
            throw new RuntimeException("文档已在该话题中");
        }

        TopicDocument topicDocument = TopicDocument.builder()
                .topicId(topicId)
                .documentId(documentId)
                .build();

        TopicDocument saved = topicDocumentRepository.save(topicDocument);
        log.info("用户 {} 将文档 {} 添加到话题 {}", userId, documentId, topicId);
        return saved;
    }

    /**
     * 从话题移除文档
     */
    @Transactional
    public void removeDocumentFromTopic(Long topicId, Long documentId, Long userId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("话题不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 权限检查：只能从自己创建的话题中移除，或 ADMIN 可以移除任何
        if (!topic.getOwnerId().equals(userId) && !hasRole(user, "ADMIN")) {
            throw new RuntimeException("权限不足：只能从自己创建的话题中移除文档");
        }

        TopicDocument topicDocument = topicDocumentRepository.findByTopicIdAndDocumentId(topicId, documentId)
                .orElseThrow(() -> new RuntimeException("文档不在该话题中"));

        topicDocumentRepository.delete(topicDocument);
        log.info("用户 {} 从话题 {} 移除文档 {}", userId, topicId, documentId);
    }

    /**
     * 获取用户创建的所有话题
     */
    public List<Topic> getTopicsByUser(Long userId) {
        return topicRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取话题下的所有文档
     */
    public List<MultimodalDocument> getDocumentsByTopic(Long topicId) {
        List<TopicDocument> topicDocuments = topicDocumentRepository.findByTopicIdOrderByAddedAtDesc(topicId);
        List<Long> documentIds = topicDocuments.stream()
                .map(TopicDocument::getDocumentId)
                .collect(Collectors.toList());

        if (documentIds.isEmpty()) {
            return List.of();
        }

        return documentRepository.findAllById(documentIds);
    }

    /**
     * 分页获取话题下的文档（支持关键词搜索）
     */
    public Map<String, Object> getDocumentsByTopicPaged(Long topicId, String keyword, int page, int size) {
        // 验证话题是否存在
        topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("话题不存在"));

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<MultimodalDocument> documentPage =
                topicDocumentRepository.findDocumentsByTopicIdPaged(topicId, keyword, pageable);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", documentPage.getContent());
        result.put("totalElements", documentPage.getTotalElements());
        result.put("totalPages", documentPage.getTotalPages());
        result.put("number", documentPage.getNumber());
        result.put("size", documentPage.getSize());

        return result;
    }

    /**
     * 获取话题树结构
     */
    public List<Map<String, Object>> getTopicTree(Long userId) {
        List<Topic> allTopics = topicRepository.findByOwnerIdOrderByCreatedAtDesc(userId);

        // 获取所有顶级话题（parentId 为空）
        List<Topic> topTopics = allTopics.stream()
                .filter(t -> t.getParentId() == null)
                .collect(Collectors.toList());

        // 构建树结构
        return topTopics.stream()
                .map(this::buildTopicNode)
                .collect(Collectors.toList());
    }

    /**
     * 构建话题树节点
     */
    private Map<String, Object> buildTopicNode(Topic topic) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", topic.getId());
        node.put("name", topic.getName());
        node.put("description", topic.getDescription());
        node.put("isPublic", topic.getIsPublic());
        node.put("createdAt", topic.getCreatedAt());

        // 统计文档数量
        long docCount = topicDocumentRepository.countByTopicId(topic.getId());
        node.put("documentCount", docCount);

        // 获取子话题
        List<Topic> children = topicRepository.findByParentIdOrderByCreatedAtDesc(topic.getId());
        if (!children.isEmpty()) {
            List<Map<String, Object>> childNodes = children.stream()
                    .map(this::buildTopicNode)
                    .collect(Collectors.toList());
            node.put("children", childNodes);
        }

        return node;
    }

    /**
     * 获取话题详情（包含订阅信息）
     */
    public Map<String, Object> getTopicDetail(Long topicId, Long currentUserId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("话题不存在"));

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", topic.getId());
        detail.put("name", topic.getName());
        detail.put("description", topic.getDescription());
        detail.put("parentId", topic.getParentId());
        detail.put("ownerId", topic.getOwnerId());
        detail.put("isPublic", topic.getIsPublic());
        detail.put("createdAt", topic.getCreatedAt());
        detail.put("updatedAt", topic.getUpdatedAt());

        // 统计文档数量
        long docCount = topicDocumentRepository.countByTopicId(topicId);
        detail.put("documentCount", docCount);

        // 统计订阅数量
        long subscriberCount = topicSubscriptionRepository.countByTopicId(topicId);
        detail.put("subscriberCount", subscriberCount);

        // 检查当前用户是否已订阅
        boolean subscribed = false;
        if (currentUserId != null) {
            subscribed = topicSubscriptionRepository.existsByTopicIdAndUserId(topicId, currentUserId);
        }
        detail.put("subscribed", subscribed);

        // 获取创建者名称
        userRepository.findById(topic.getOwnerId()).ifPresent(owner -> {
            detail.put("ownerName", owner.getUsername());
        });

        return detail;
    }

    /**
     * 获取所有公开话题
     */
    public List<Topic> getPublicTopics() {
        return topicRepository.findByIsPublicTrueOrderByCreatedAtDesc();
    }

    // ==================== 订阅相关方法 ====================

    /**
     * 订阅话题
     */
    @Transactional
    public Map<String, Object> subscribeToTopic(Long topicId, Long userId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("话题不存在"));

        // 只能订阅公开话题
        if (!topic.getIsPublic()) {
            throw new RuntimeException("只能订阅公开话题");
        }

        // 检查是否已订阅
        if (topicSubscriptionRepository.existsByTopicIdAndUserId(topicId, userId)) {
            throw new RuntimeException("已经订阅过该话题");
        }

        // 创建订阅记录
        TopicSubscription subscription = TopicSubscription.builder()
                .topicId(topicId)
                .userId(userId)
                .build();
        topicSubscriptionRepository.save(subscription);

        log.info("用户 {} 订阅话题: {}", userId, topic.getName());

        // 返回订阅状态和订阅数
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subscribed", true);
        result.put("subscriberCount", topicSubscriptionRepository.countByTopicId(topicId));
        return result;
    }

    /**
     * 取消订阅话题
     */
    @Transactional
    public void unsubscribeFromTopic(Long topicId, Long userId) {
        // 检查话题是否存在
        topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("话题不存在"));

        // 检查是否已订阅
        if (!topicSubscriptionRepository.existsByTopicIdAndUserId(topicId, userId)) {
            throw new RuntimeException("未订阅该话题");
        }

        // 删除订阅记录
        topicSubscriptionRepository.deleteByTopicIdAndUserId(topicId, userId);
        log.info("用户 {} 取消订阅话题: {}", userId, topicId);
    }

    /**
     * 获取订阅状态
     */
    public Map<String, Object> getSubscriptionStatus(Long topicId, Long userId) {
        // 检查话题是否存在
        topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("话题不存在"));

        boolean subscribed = topicSubscriptionRepository.existsByTopicIdAndUserId(topicId, userId);
        long subscriberCount = topicSubscriptionRepository.countByTopicId(topicId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subscribed", subscribed);
        result.put("subscriberCount", subscriberCount);
        return result;
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查用户是否拥有指定角色
     */
    private boolean hasRole(User user, String roleName) {
        if (user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    /**
     * 检查用户是否拥有任一指定角色
     */
    private boolean hasRequiredRole(User user, Set<String> requiredRoles) {
        if (user.getRoles() == null) {
            return false;
        }
        Set<String> userRoles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return userRoles.stream().anyMatch(requiredRoles::contains);
    }

    // ==================== 推荐相关方法 ====================

    /**
     * 获取推荐话题（基于用户兴趣标签）
     */
    public List<Map<String, Object>> getRecommendedTopics(Long userId, int limit) {
        // 1. 获取用户最近的文档标签
        Set<String> interestTags = new java.util.HashSet<>();
        try {
            // 获取用户最近查看的文档（这里简化处理，获取用户的所有文档标签）
            List<MultimodalDocument> userDocs = documentRepository.findByUser_Id(userId)
                    .stream()
                    .limit(20)
                    .collect(Collectors.toList());

            for (MultimodalDocument doc : userDocs) {
                if (doc.getTags() != null && !doc.getTags().isBlank()) {
                    String[] tags = doc.getTags().split(",");
                    for (String tag : tags) {
                        interestTags.add(tag.trim());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取用户兴趣标签失败，将返回热门话题", e);
        }

        // 2. 如果没有标签，返回热门话题
        if (interestTags.isEmpty()) {
            return getHotTopics(limit);
        }

        // 3. 获取所有公开话题（排除用户自己的和已订阅的）
        List<Topic> allPublicTopics = topicRepository.findByIsPublicTrueOrderByCreatedAtDesc();
        List<Long> subscribedTopicIds = topicSubscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ts -> ts.getTopicId())
                .collect(Collectors.toList());

        // 4. 计算每个话题的相关性得分
        List<Map<String, Object>> scoredTopics = new java.util.ArrayList<>();
        for (Topic topic : allPublicTopics) {
            // 排除自己的话题和已订阅的话题
            if (topic.getOwnerId().equals(userId) || subscribedTopicIds.contains(topic.getId())) {
                continue;
            }

            int score = 0;
            // 匹配话题名称/描述中的标签
            String topicText = (topic.getName() + " " + (topic.getDescription() != null ? topic.getDescription() : "")).toLowerCase();
            for (String tag : interestTags) {
                if (topicText.contains(tag.toLowerCase())) {
                    score++;
                }
            }

            // 获取话题下文档的标签（简化处理，直接使用话题本身的信息）
            Map<String, Object> topicData = buildTopicSummary(topic);
            topicData.put("score", score);
            scoredTopics.add(topicData);
        }

        // 5. 按得分排序，订阅数作为第二排序条件
        scoredTopics.sort((a, b) -> {
            int scoreA = (Integer) a.getOrDefault("score", 0);
            int scoreB = (Integer) b.getOrDefault("score", 0);
            if (scoreA != scoreB) {
                return Integer.compare(scoreB, scoreA);
            }
            return Long.compare((Long) b.getOrDefault("subscriberCount", 0L),
                                (Long) a.getOrDefault("subscriberCount", 0L));
        });

        // 6. 返回 top N
        return scoredTopics.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取热门话题（按订阅数和文档数排序）
     */
    public List<Map<String, Object>> getHotTopics(int limit) {
        List<Topic> allPublicTopics = topicRepository.findByIsPublicTrueOrderByCreatedAtDesc();

        return allPublicTopics.stream()
                .map(this::buildTopicSummary)
                .sorted((a, b) -> {
                    // 综合得分：订阅数 * 10 + 文档数
                    long scoreA = (Long) a.getOrDefault("subscriberCount", 0L) * 10 +
                                  (Long) a.getOrDefault("documentCount", 0L);
                    long scoreB = (Long) b.getOrDefault("subscriberCount", 0L) * 10 +
                                  (Long) b.getOrDefault("documentCount", 0L);
                    return Long.compare(scoreB, scoreA);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 构建话题摘要信息
     */
    private Map<String, Object> buildTopicSummary(Topic topic) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", topic.getId());
        summary.put("name", topic.getName());
        summary.put("description", topic.getDescription());
        summary.put("isPublic", topic.getIsPublic());
        summary.put("createdAt", topic.getCreatedAt());
        summary.put("ownerId", topic.getOwnerId());
        summary.put("documentCount", topicDocumentRepository.countByTopicId(topic.getId()));
        summary.put("subscriberCount", topicSubscriptionRepository.countByTopicId(topic.getId()));

        // 获取创建者名称
        userRepository.findById(topic.getOwnerId()).ifPresent(owner -> {
            summary.put("ownerName", owner.getUsername());
        });

        return summary;
    }
}
