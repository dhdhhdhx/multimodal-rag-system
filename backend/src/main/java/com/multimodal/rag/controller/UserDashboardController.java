package com.multimodal.rag.controller;

import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.UserRepository;
import com.multimodal.rag.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "用户仪表板", description = "用户个人统计数据和概览信息")
public class UserDashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @GetMapping("/stats")
    @Operation(
        summary = "获取我的统计数据",
        description = "获取当前用户的个人统计数据，包括文档数、查询次数、存储使用等"
    )
    public ResponseEntity<Map<String, Object>> getMyStats() {
        return ResponseEntity.ok(dashboardService.getUserStatistics(getCurrentUser().getId()));
    }
}
