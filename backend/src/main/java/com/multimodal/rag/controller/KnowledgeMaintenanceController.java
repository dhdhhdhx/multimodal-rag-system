package com.multimodal.rag.controller;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.UserRepository;
import com.multimodal.rag.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Tag(name = "Knowledge Maintenance", description = "Maintenance endpoints for reindexing stored knowledge documents")
public class KnowledgeMaintenanceController {

    private final KnowledgeService knowledgeService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @PostMapping("/{id}/reindex")
    @Operation(
            summary = "Reindex one document",
            description = "Reprocess the original file and rebuild vector chunks for a single stored document"
    )
    public ResponseEntity<MultimodalDocument> reindexDocument(@PathVariable Long id) throws IOException {
        return ResponseEntity.ok(knowledgeService.reindexDocument(id, getCurrentUser()));
    }

    @PostMapping("/reindex-owned")
    @Operation(
            summary = "Reindex my documents",
            description = "Reprocess and rebuild vector chunks for all documents owned by the current user"
    )
    public ResponseEntity<KnowledgeService.ReindexSummary> reindexOwnedDocuments() {
        return ResponseEntity.ok(knowledgeService.reindexOwnedDocuments(getCurrentUser()));
    }
}
