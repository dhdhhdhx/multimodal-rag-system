package com.multimodal.rag.controller;

import com.multimodal.rag.model.ContentAnnotation;
import com.multimodal.rag.service.AnnotationService;
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
public class AnnotationController {
    
    private final AnnotationService annotationService;
    
    @PostMapping
    public ResponseEntity<ContentAnnotation> createAnnotation(
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
    public ResponseEntity<ContentAnnotation> updateAnnotation(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        
        ContentAnnotation annotation = annotationService.updateAnnotation(
                id, request.get("annotationText"), request.get("color"));
        return ResponseEntity.ok(annotation);
    }
    
    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<ContentAnnotation>> getAnnotationsByDocument(@PathVariable Long documentId) {
        return ResponseEntity.ok(annotationService.getAnnotationsByDocument(documentId));
    }
    
    @GetMapping("/my-annotations")
    public ResponseEntity<List<ContentAnnotation>> getMyAnnotations(@AuthenticationPrincipal UserDetails currentUser) {
        Long userId = 1L; // Placeholder
        return ResponseEntity.ok(annotationService.getAnnotationsByUser(userId));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnotation(@PathVariable Long id) {
        annotationService.deleteAnnotation(id);
        return ResponseEntity.noContent().build();
    }
}
