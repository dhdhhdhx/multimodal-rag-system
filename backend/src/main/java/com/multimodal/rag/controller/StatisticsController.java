package com.multimodal.rag.controller;

import com.multimodal.rag.model.DocumentAccessLog;
import com.multimodal.rag.model.QueryLog;
import com.multimodal.rag.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "统计分析", description = "查询统计、热门文档、访问历史等数据分析接口")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/queries/recent")
    @Operation(
        summary = "获取近期查询记录",
        description = "获取指定天数内的查询日志记录"
    )
    public ResponseEntity<List<QueryLog>> getRecentQueries(
            @Parameter(description = "统计天数")
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(statisticsService.getRecentQueries(days));
    }

    @GetMapping("/queries/summary")
    @Operation(
        summary = "获取查询统计摘要",
        description = "获取查询次数、平均响应时间等统计数据"
    )
    public ResponseEntity<Map<String, Object>> getQueryStatistics() {
        return ResponseEntity.ok(statisticsService.getQueryStatistics());
    }

    @GetMapping("/documents/hot")
    @Operation(
        summary = "获取热门文档",
        description = "获取指定天数内浏览量最高的文档列表"
    )
    public ResponseEntity<List<Map<String, Object>>> getHotDocuments(
            @Parameter(description = "统计天数")
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(statisticsService.getHotDocuments(days));
    }

    @GetMapping("/documents/{documentId}/access-history")
    @Operation(
        summary = "获取文档访问历史",
        description = "获取指定文档的所有访问记录"
    )
    public ResponseEntity<List<DocumentAccessLog>> getDocumentAccessHistory(
            @Parameter(description = "文档ID")
            @PathVariable Long documentId) {
        return ResponseEntity.ok(statisticsService.getDocumentAccessHistory(documentId));
    }
}
