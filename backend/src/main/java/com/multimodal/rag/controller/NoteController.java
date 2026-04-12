package com.multimodal.rag.controller;

import com.multimodal.rag.model.KnowledgeNote;
import com.multimodal.rag.model.User;
import com.multimodal.rag.service.NoteService;
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
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Tag(name = "笔记管理", description = "文档笔记创建、查看、更新和删除")
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    @Operation(
        summary = "创建笔记",
        description = "为文档创建新的笔记记录"
    )
    public ResponseEntity<KnowledgeNote> createNote(
            @Parameter(description = "笔记信息，包含 documentId、title、content")
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
    @Operation(
        summary = "更新笔记",
        description = "修改指定笔记的标题和内容"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "笔记不存在")
    })
    public ResponseEntity<KnowledgeNote> updateNote(
            @Parameter(description = "笔记ID")
            @PathVariable Long id,
            @Parameter(description = "笔记信息，包含 title 和 content")
            @RequestBody Map<String, String> request) {

        KnowledgeNote note = noteService.updateNote(id, request.get("title"), request.get("content"));
        return ResponseEntity.ok(note);
    }

    @GetMapping("/document/{documentId}")
    @Operation(
        summary = "获取文档的所有笔记",
        description = "获取指定文档的所有笔记记录"
    )
    public ResponseEntity<List<KnowledgeNote>> getNotesByDocument(
            @Parameter(description = "文档ID")
            @PathVariable Long documentId) {
        return ResponseEntity.ok(noteService.getNotesByDocument(documentId));
    }

    @GetMapping("/my-notes")
    @Operation(
        summary = "获取我的笔记",
        description = "获取当前用户的所有笔记记录"
    )
    public ResponseEntity<List<KnowledgeNote>> getMyNotes(@AuthenticationPrincipal UserDetails currentUser) {
        Long userId = 1L; // Placeholder
        return ResponseEntity.ok(noteService.getNotesByUser(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除笔记",
        description = "删除指定的笔记记录"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "笔记不存在")
    })
    public ResponseEntity<Void> deleteNote(
            @Parameter(description = "笔记ID")
            @PathVariable Long id) {
        noteService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }
}
