package com.multimodal.rag.controller;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.UserRepository;
import com.multimodal.rag.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "推荐系统", description = "个性化文档推荐和发现功能")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(
        summary = "获取个性化推荐",
        description = "基于用户历史行为和兴趣获取个性化文档推荐"
    )
    public ResponseEntity<List<MultimodalDocument>> getRecommendations(
            @Parameter(description = "推荐数量限制")
            @RequestParam(defaultValue = "10") int limit) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<MultimodalDocument> recommendations = recommendationService.recommendDocuments(user.getId(), limit);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/discovery")
    @Operation(
        summary = "获取发现页数据",
        description = "获取发现页的各类推荐文档集合（热门、最新、相关等）"
    )
    public ResponseEntity<java.util.Map<String, List<MultimodalDocument>>> getDiscoveryData(
            @Parameter(description = "每类推荐数量限制")
            @RequestParam(defaultValue = "6") int limit) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(recommendationService.getDiscoveryData(user.getId(), limit));
    }
}
