package com.multimodal.rag.controller;

import com.multimodal.rag.service.resilience.ResilienceManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final ResilienceManager resilienceManager;

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = Map.of("status", "UP");
        return ResponseEntity.ok(body);
    }

    @GetMapping("/api/health/detail")
    public ResponseEntity<Map<String, Object>> healthDetail() {
        Map<String, Object> body = Map.of(
                "status", "UP",
                "circuitBreakers", resilienceManager.getHealthSummary()
        );
        return ResponseEntity.ok(body);
    }
}
