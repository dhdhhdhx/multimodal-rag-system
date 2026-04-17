package com.multimodal.rag.controller;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.Topic;
import com.multimodal.rag.model.TopicDocument;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.UserRepository;
import com.multimodal.rag.repository.TopicDocumentRepository;
import com.multimodal.rag.repository.TopicRepository;
import com.multimodal.rag.repository.TopicSubscriptionRepository;
import com.multimodal.rag.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 话题管理 Controller
 */
@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "话题管理", description = "话题的创建、查询、更新、删除，以及文档与话题的关联管理")
public class TopicController {

    private final TopicService topicService;
    private final UserRepository userRepository;
    private final TopicDocumentRepository topicDocumentRepository;
    private final TopicSubscriptionRepository topicSubscriptionRepository;
    private final TopicRepository topicRepository;

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));
    }

    @PostMapping
    @Operation(
        summary = "创建话题",
        description = "创建新话题。仅 PREMIUM 和 ADMIN 用户可以创建话题。支持一级子话题。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功", content = @Content(schema = @Schema(implementation = Topic.class))),
        @ApiResponse(responseCode = "400", description = "权限不足或参数无效"),
        @ApiResponse(responseCode = "401", description = "未登录")
    })
    public ResponseEntity<Topic> createTopic(
            @Parameter(description = "话题信息")
            @RequestBody Topic topic) {
        Topic created = topicService.createTopic(topic, getCurrentUser().getId());
        return ResponseEntity.ok(created);
    }

    @GetMapping
    @Operation(
        summary = "获取用户的话题列表",
        description = "获取当前登录用户创建的所有话题，包含文档数量和订阅数统计"
    )
    public ResponseEntity<List<Map<String, Object>>> getTopicsByUser() {
        List<Topic> topics = topicService.getTopicsByUser(getCurrentUser().getId());
        // 为每个话题添加文档数量、订阅数量和创建者信息
        List<Map<String, Object>> result = topics.stream()
                .map(topic -> {
                    Map<String, Object> topicData = new LinkedHashMap<>();
                    topicData.put("id", topic.getId());
                    topicData.put("name", topic.getName());
                    topicData.put("description", topic.getDescription());
                    topicData.put("isPublic", topic.getIsPublic());
                    topicData.put("ownerId", topic.getOwnerId());
                    topicData.put("parentId", topic.getParentId());
                    topicData.put("createdAt", topic.getCreatedAt());
                    topicData.put("updatedAt", topic.getUpdatedAt());
                    topicData.put("documentCount", topicDocumentRepository.countByTopicId(topic.getId()));
                    topicData.put("subscriberCount", topicSubscriptionRepository.countByTopicId(topic.getId()));
                    // 获取创建者名称
                    userRepository.findById(topic.getOwnerId()).ifPresent(owner -> {
                        topicData.put("ownerName", owner.getUsername());
                    });
                    return topicData;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/public")
    @Operation(
        summary = "获取所有公开话题",
        description = "获取系统中所有公开的话题列表，包含文档数量统计"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "未登录")
    })
    public ResponseEntity<List<Map<String, Object>>> getPublicTopics() {
        List<Topic> publicTopics = topicService.getPublicTopics();
        // 为每个话题添加文档数量和创建者信息
        List<Map<String, Object>> result = publicTopics.stream()
                .map(topic -> {
                    Map<String, Object> topicData = new LinkedHashMap<>();
                    topicData.put("id", topic.getId());
                    topicData.put("name", topic.getName());
                    topicData.put("description", topic.getDescription());
                    topicData.put("isPublic", topic.getIsPublic());
                    topicData.put("ownerId", topic.getOwnerId());
                    topicData.put("createdAt", topic.getCreatedAt());
                    topicData.put("documentCount", topicDocumentRepository.countByTopicId(topic.getId()));
                    return topicData;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/tree")
    @Operation(
        summary = "获取话题树",
        description = "获取当前用户的话题树形结构，包含文档数量统计"
    )
    public ResponseEntity<List<Map<String, Object>>> getTopicTree() {
        List<Map<String, Object>> tree = topicService.getTopicTree(getCurrentUser().getId());
        return ResponseEntity.ok(tree);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取话题详情",
        description = "获取指定话题的详细信息，包括文档数量、订阅状态"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "话题不存在")
    })
    public ResponseEntity<Map<String, Object>> getTopicDetail(
            @Parameter(description = "话题ID")
            @PathVariable Long id) {
        // 获取当前用户 ID，如果未登录则为 null
        Long currentUserId = null;
        try {
            currentUserId = getCurrentUser().getId();
        } catch (Exception e) {
            // 未登录，currentUserId 保持为 null
        }
        Map<String, Object> detail = topicService.getTopicDetail(id, currentUserId);
        return ResponseEntity.ok(detail);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新话题",
        description = "更新话题信息。仅话题创建者或 ADMIN 可以操作。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "话题不存在")
    })
    public ResponseEntity<Topic> updateTopic(
            @Parameter(description = "话题ID")
            @PathVariable Long id,
            @Parameter(description = "更新的话题信息")
            @RequestBody Topic topic) {
        Topic updated = topicService.updateTopic(id, topic, getCurrentUser().getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除话题",
        description = "删除指定话题及其所有文档关联。仅话题创建者或 ADMIN 可以操作。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "权限不足或有子话题"),
        @ApiResponse(responseCode = "404", description = "话题不存在")
    })
    public ResponseEntity<Void> deleteTopic(
            @Parameter(description = "话题ID")
            @PathVariable Long id) {
        topicService.deleteTopic(id, getCurrentUser().getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/documents")
    @Operation(
        summary = "获取话题下的文档",
        description = "获取指定话题下的所有文档列表"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "话题不存在")
    })
    public ResponseEntity<List<Map<String, Object>>> getDocumentsByTopic(
            @Parameter(description = "话题ID")
            @PathVariable Long id) {
        List<MultimodalDocument> documents = topicService.getDocumentsByTopic(id);
        // 转换为 Map 避免序列化懒加载实体
        List<Map<String, Object>> result = documents.stream()
                .map(doc -> {
                    Map<String, Object> docData = new LinkedHashMap<>();
                    docData.put("id", doc.getId());
                    docData.put("fileName", doc.getFileName());
                    docData.put("fileType", doc.getFileType());
                    docData.put("filePath", doc.getFilePath());
                    docData.put("fileSize", doc.getFileSize());
                    docData.put("extractedContent", doc.getExtractedContent());
                    docData.put("tags", doc.getTags());
                    docData.put("status", doc.getStatus());
                    docData.put("shared", doc.isShared());
                    docData.put("viewCount", doc.getViewCount());
                    docData.put("uploadTime", doc.getUploadTime());
                    return docData;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/documents/paged")
    @Operation(
        summary = "分页获取话题文档",
        description = "支持关键词搜索的分页文档列表"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "话题不存在")
    })
    public ResponseEntity<Map<String, Object>> getDocumentsByTopicPaged(
            @Parameter(description = "话题ID")
            @PathVariable Long id,
            @Parameter(description = "搜索关键词")
            @RequestParam(defaultValue = "") String keyword,
            @Parameter(description = "页码（从0开始）")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "9") int size) {
        Map<String, Object> result = topicService.getDocumentsByTopicPaged(id, keyword, page, size);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/documents/{docId}")
    @Operation(
        summary = "添加文档到话题",
        description = "将指定文档添加到话题中"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "添加成功"),
        @ApiResponse(responseCode = "400", description = "文档已在该话题中或权限不足"),
        @ApiResponse(responseCode = "404", description = "话题或文档不存在")
    })
    public ResponseEntity<TopicDocument> addDocumentToTopic(
            @Parameter(description = "话题ID")
            @PathVariable Long id,
            @Parameter(description = "文档ID")
            @PathVariable Long docId) {
        TopicDocument topicDocument = topicService.addDocumentToTopic(id, docId, getCurrentUser().getId());
        return ResponseEntity.ok(topicDocument);
    }

    @DeleteMapping("/{id}/documents/{docId}")
    @Operation(
        summary = "从话题移除文档",
        description = "从指定话题中移除文档"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "移除成功"),
        @ApiResponse(responseCode = "400", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "话题不存在或文档不在该话题中")
    })
    public ResponseEntity<Void> removeDocumentFromTopic(
            @Parameter(description = "话题ID")
            @PathVariable Long id,
            @Parameter(description = "文档ID")
            @PathVariable Long docId) {
        topicService.removeDocumentFromTopic(id, docId, getCurrentUser().getId());
        return ResponseEntity.noContent().build();
    }

    // ==================== 订阅相关端点 ====================

    @PostMapping("/{id}/subscribe")
    @Operation(
        summary = "订阅话题",
        description = "订阅指定的公开话题"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "订阅成功"),
        @ApiResponse(responseCode = "400", description = "话题不公开或已订阅"),
        @ApiResponse(responseCode = "404", description = "话题不存在")
    })
    public ResponseEntity<Map<String, Object>> subscribeTopic(
            @Parameter(description = "话题ID")
            @PathVariable Long id) {
        Map<String, Object> result = topicService.subscribeToTopic(id, getCurrentUser().getId());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}/subscribe")
    @Operation(
        summary = "取消订阅话题",
        description = "取消订阅指定话题"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "取消订阅成功"),
        @ApiResponse(responseCode = "400", description = "未订阅该话题"),
        @ApiResponse(responseCode = "404", description = "话题不存在")
    })
    public ResponseEntity<Void> unsubscribeTopic(
            @Parameter(description = "话题ID")
            @PathVariable Long id) {
        topicService.unsubscribeFromTopic(id, getCurrentUser().getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/subscription-status")
    @Operation(
        summary = "获取订阅状态",
        description = "获取当前用户对指定话题的订阅状态"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "话题不存在")
    })
    public ResponseEntity<Map<String, Object>> getSubscriptionStatus(
            @Parameter(description = "话题ID")
            @PathVariable Long id) {
        Map<String, Object> result = topicService.getSubscriptionStatus(id, getCurrentUser().getId());
        return ResponseEntity.ok(result);
    }

    // ==================== 推荐相关端点 ====================

    @GetMapping("/recommended")
    @Operation(
        summary = "获取推荐话题",
        description = "基于用户兴趣标签推荐话题"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "未登录")
    })
    public ResponseEntity<List<Map<String, Object>>> getRecommendedTopics(
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "6") int limit) {
        List<Map<String, Object>> topics = topicService.getRecommendedTopics(getCurrentUser().getId(), limit);
        return ResponseEntity.ok(topics);
    }

    @GetMapping("/hot")
    @Operation(
        summary = "获取热门话题",
        description = "获取订阅数最多的公开话题"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    public ResponseEntity<List<Map<String, Object>>> getHotTopics(
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "6") int limit) {
        List<Map<String, Object>> topics = topicService.getHotTopics(limit);
        return ResponseEntity.ok(topics);
    }

    @GetMapping("/public/paged")
    @Operation(
        summary = "分页获取公开话题",
        description = "支持关键词搜索的公开话题列表"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    public ResponseEntity<Map<String, Object>> getPublicTopicsPaged(
            @Parameter(description = "搜索关键词") @RequestParam(defaultValue = "") String keyword,
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "12") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Topic> topicPage = topicRepository.searchPublicTopics(keyword, pageable);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", topicPage.getContent().stream()
                .map(topic -> {
                    Map<String, Object> topicData = new LinkedHashMap<>();
                    topicData.put("id", topic.getId());
                    topicData.put("name", topic.getName());
                    topicData.put("description", topic.getDescription());
                    topicData.put("isPublic", topic.getIsPublic());
                    topicData.put("ownerId", topic.getOwnerId());
                    topicData.put("createdAt", topic.getCreatedAt());
                    topicData.put("documentCount", topicDocumentRepository.countByTopicId(topic.getId()));
                    topicData.put("subscriberCount", topicSubscriptionRepository.countByTopicId(topic.getId()));
                    return topicData;
                })
                .collect(Collectors.toList()));
        result.put("totalElements", topicPage.getTotalElements());
        result.put("totalPages", topicPage.getTotalPages());
        result.put("number", topicPage.getNumber());
        result.put("size", topicPage.getSize());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/subscribed")
    @Operation(
        summary = "获取我的订阅话题",
        description = "获取当前用户已订阅的话题列表"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "未登录")
    })
    public ResponseEntity<List<Map<String, Object>>> getMySubscriptions() {
        Long userId = getCurrentUser().getId();
        List<Long> topicIds = topicSubscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(com.multimodal.rag.model.TopicSubscription::getTopicId)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Long topicId : topicIds) {
            try {
                Map<String, Object> detail = topicService.getTopicDetail(topicId, userId);
                result.add(detail);
            } catch (Exception e) {
                log.warn("获取订阅话题详情失败: topicId={}", topicId, e);
            }
        }

        return ResponseEntity.ok(result);
    }
}
