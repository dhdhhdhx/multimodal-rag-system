package com.multimodal.rag.controller;

import com.multimodal.rag.model.KnowledgeNote;
import com.multimodal.rag.model.User;
import com.multimodal.rag.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {
    
    private final NoteService noteService;
    
    @PostMapping
    public ResponseEntity<KnowledgeNote> createNote(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        Long documentId = ((Number) request.get("documentId")).longValue();
        String title = (String) request.get("title");
        String content = (String) request.get("content");
        
        // TODO: Extract userId from currentUser
        Long userId = 1L; // Placeholder
        
        KnowledgeNote note = noteService.createNote(documentId, userId, title, content);
        return ResponseEntity.ok(note);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeNote> updateNote(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        
        KnowledgeNote note = noteService.updateNote(id, request.get("title"), request.get("content"));
        return ResponseEntity.ok(note);
    }
    
    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<KnowledgeNote>> getNotesByDocument(@PathVariable Long documentId) {
        return ResponseEntity.ok(noteService.getNotesByDocument(documentId));
    }
    
    @GetMapping("/my-notes")
    public ResponseEntity<List<KnowledgeNote>> getMyNotes(@AuthenticationPrincipal UserDetails currentUser) {
        Long userId = 1L; // Placeholder
        return ResponseEntity.ok(noteService.getNotesByUser(userId));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }
}
