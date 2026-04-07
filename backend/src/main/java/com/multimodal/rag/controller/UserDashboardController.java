package com.multimodal.rag.controller;

import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.UserRepository;
import com.multimodal.rag.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class UserDashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMyStats() {
        return ResponseEntity.ok(dashboardService.getUserStatistics(getCurrentUser().getId()));
    }
}
