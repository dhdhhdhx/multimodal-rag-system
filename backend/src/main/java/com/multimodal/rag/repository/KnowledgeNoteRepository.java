package com.multimodal.rag.repository;

import com.multimodal.rag.model.KnowledgeNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeNoteRepository extends JpaRepository<KnowledgeNote, Long> {
    List<KnowledgeNote> findByDocumentId(Long documentId);
    List<KnowledgeNote> findByUserId(Long userId);
    List<KnowledgeNote> findByDocumentIdAndUserId(Long documentId, Long userId);
}
