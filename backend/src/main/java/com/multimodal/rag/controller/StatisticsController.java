package com.multimodal.rag.controller;

import com.multimodal.rag.model.DocumentAccessLog;
import com.multimodal.rag.model.QueryLog;
import com.multimodal.rag.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    @GetMapping("/queries/recent")
    public ResponseEntity<List<QueryLog>> getRecentQueries(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(statisticsService.getRecentQueries(days));
    }
    
    @GetMapping("/queries/summary")
    public ResponseEntity<Map<String, Object>> getQueryStatistics() {
        return ResponseEntity.ok(statisticsService.getQueryStatistics());
    }
    
    @GetMapping("/documents/hot")
    public ResponseEntity<List<Map<String, Object>>> getHotDocuments(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(statisticsService.getHotDocuments(days));
    }
    
    @GetMapping("/documents/{documentId}/access-history")
    public ResponseEntity<List<DocumentAccessLog>> getDocumentAccessHistory(@PathVariable Long documentId) {
        return ResponseEntity.ok(statisticsService.getDocumentAccessHistory(documentId));
    }
}
