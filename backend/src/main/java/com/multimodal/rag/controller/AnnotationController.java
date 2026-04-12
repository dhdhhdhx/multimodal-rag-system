package com.multimodal.rag.controller;

import com.multimodal.rag.model.ContentAnnotation;
import com.multimodal.rag.service.AnnotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/annotations")
@RequiredArgsConstructor
@Tag(name = "标注管理", description = "文档内容标注创建、查看、更新和删除")
public class AnnotationController {

    private final AnnotationService annotationService;

    @PostMapping
    @Operation(
        summary = "创建标注",
        description = "为文档内容创建高亮标注和注释"
    )
    public ResponseEntity<ContentAnnotation> createAnnotation(
            @Parameter(description = "标注信息，包含 documentId、startOffset、endOffset、highlightedText、annotationText、color")
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails currentUser) {

        Long documentId = ((Number) request.get("documentId")).longValue();
        Integer startOffset = ((Number) request.get("startOffset")).intValue();
        Integer endOffset = ((Number) request.get("endOffset")).intValue();
        String highlightedText = (String) request.get("highlightedText");
        String annotationText = (String) request.get("annotationText");
        String color = (String) request.get("color");

        Long userId = 1L; // Placeholder

        ContentAnnotation annotation = annotationService.createAnnotation(
                documentId, userId, startOffset, endOffset, highlightedText, annotationText, color);
        return ResponseEntity.ok(annotation);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新标注",
        description = "修改指定标注的注释文本和颜色"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "标注不存在")
    })
    public ResponseEntity<ContentAnnotation> updateAnnotation(
            @Parameter(description = "标注ID")
            @PathVariable Long id,
            @Parameter(description = "标注信息，包含 annotationText 和 color")
            @RequestBody Map<String, String> request) {

        ContentAnnotation annotation = annotationService.updateAnnotation(
                id, request.get("annotationText"), request.get("color"));
        return ResponseEntity.ok(annotation);
    }

    @GetMapping("/document/{documentId}")
    @Operation(
        summary = "获取文档的所有标注",
        description = "获取指定文档的所有标注记录"
    )
    public ResponseEntity<List<ContentAnnotation>> getAnnotationsByDocument(
            @Parameter(description = "文档ID")
            @PathVariable Long documentId) {
        return ResponseEntity.ok(annotationService.getAnnotationsByDocument(documentId));
    }

    @GetMapping("/my-annotations")
    @Operation(
        summary = "获取我的标注",
        description = "获取当前用户的所有标注记录"
    )
    public ResponseEntity<List<ContentAnnotation>> getMyAnnotations(@AuthenticationPrincipal UserDetails currentUser) {
        Long userId = 1L; // Placeholder
        return ResponseEntity.ok(annotationService.getAnnotationsByUser(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除标注",
        description = "删除指定的标注记录"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "标注不存在")
    })
    public ResponseEntity<Void> deleteAnnotation(
            @Parameter(description = "标注ID")
            @PathVariable Long id) {
        annotationService.deleteAnnotation(id);
        return ResponseEntity.noContent().build();
    }
}
