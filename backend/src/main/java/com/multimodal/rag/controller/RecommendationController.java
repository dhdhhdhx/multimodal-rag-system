package com.multimodal.rag.controller;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.UserRepository;
import com.multimodal.rag.service.RecommendationService;
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
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<MultimodalDocument>> getRecommendations(@RequestParam(defaultValue = "10") int limit) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<MultimodalDocument> recommendations = recommendationService.recommendDocuments(user.getId(), limit);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/discovery")
    public ResponseEntity<java.util.Map<String, List<MultimodalDocument>>> getDiscoveryData(@RequestParam(defaultValue = "6") int limit) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(recommendationService.getDiscoveryData(user.getId(), limit));
    }
}
