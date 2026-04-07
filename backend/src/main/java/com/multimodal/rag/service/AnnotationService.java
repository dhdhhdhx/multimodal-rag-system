package com.multimodal.rag.service;

import com.multimodal.rag.model.ContentAnnotation;
import com.multimodal.rag.repository.ContentAnnotationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnotationService {
    
    private final ContentAnnotationRepository annotationRepository;
    
    @Transactional
    public ContentAnnotation createAnnotation(Long documentId, Long userId, 
                                              Integer startOffset, Integer endOffset,
                                              String highlightedText, String annotationText, String color) {
        ContentAnnotation annotation = new ContentAnnotation();
        annotation.setDocumentId(documentId);
        annotation.setUserId(userId);
        annotation.setStartOffset(startOffset);
        annotation.setEndOffset(endOffset);
        annotation.setHighlightedText(highlightedText);
        annotation.setAnnotationText(annotationText);
        annotation.setColor(color != null ? color : "#FFFF00");
        
        annotation = annotationRepository.save(annotation);
        log.info("Created annotation {} for document {} by user {}", annotation.getId(), documentId, userId);
        return annotation;
    }
    
    @Transactional
    public ContentAnnotation updateAnnotation(Long annotationId, String annotationText, String color) {
        ContentAnnotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new RuntimeException("Annotation not found"));
        
        annotation.setAnnotationText(annotationText);
        if (color != null) {
            annotation.setColor(color);
        }
        
        annotation = annotationRepository.save(annotation);
        log.info("Updated annotation {}", annotationId);
        return annotation;
    }
    
    public List<ContentAnnotation> getAnnotationsByDocument(Long documentId) {
        return annotationRepository.findByDocumentId(documentId);
    }
    
    public List<ContentAnnotation> getAnnotationsByUser(Long userId) {
        return annotationRepository.findByUserId(userId);
    }
    
    public List<ContentAnnotation> getAnnotationsByDocumentAndUser(Long documentId, Long userId) {
        return annotationRepository.findByDocumentIdAndUserId(documentId, userId);
    }
    
    @Transactional
    public void deleteAnnotation(Long annotationId) {
        annotationRepository.deleteById(annotationId);
        log.info("Deleted annotation {}", annotationId);
    }
}
