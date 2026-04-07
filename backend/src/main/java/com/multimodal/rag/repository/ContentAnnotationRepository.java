package com.multimodal.rag.repository;

import com.multimodal.rag.model.ContentAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentAnnotationRepository extends JpaRepository<ContentAnnotation, Long> {
    List<ContentAnnotation> findByDocumentId(Long documentId);
    List<ContentAnnotation> findByUserId(Long userId);
    List<ContentAnnotation> findByDocumentIdAndUserId(Long documentId, Long userId);
}
