package com.multimodal.rag.service;

import com.multimodal.rag.model.KnowledgeNote;
import com.multimodal.rag.repository.KnowledgeNoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {
    
    private final KnowledgeNoteRepository noteRepository;
    
    @Transactional
    public KnowledgeNote createNote(Long documentId, Long userId, String title, String content) {
        KnowledgeNote note = new KnowledgeNote();
        note.setDocumentId(documentId);
        note.setUserId(userId);
        note.setTitle(title);
        note.setContent(content);
        
        note = noteRepository.save(note);
        log.info("Created note {} for document {} by user {}", note.getId(), documentId, userId);
        return note;
    }
    
    @Transactional
    public KnowledgeNote updateNote(Long noteId, String title, String content) {
        KnowledgeNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));
        
        note.setTitle(title);
        note.setContent(content);
        
        note = noteRepository.save(note);
        log.info("Updated note {}", noteId);
        return note;
    }
    
    public List<KnowledgeNote> getNotesByDocument(Long documentId) {
        return noteRepository.findByDocumentId(documentId);
    }
    
    public List<KnowledgeNote> getNotesByUser(Long userId) {
        return noteRepository.findByUserId(userId);
    }
    
    public List<KnowledgeNote> getNotesByDocumentAndUser(Long documentId, Long userId) {
        return noteRepository.findByDocumentIdAndUserId(documentId, userId);
    }
    
    @Transactional
    public void deleteNote(Long noteId) {
        noteRepository.deleteById(noteId);
        log.info("Deleted note {}", noteId);
    }
}
