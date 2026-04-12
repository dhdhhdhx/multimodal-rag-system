package com.multimodal.rag.controller;

import com.multimodal.rag.model.ChatMessage;
import com.multimodal.rag.model.ChatSession;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.ChatMessageRepository;
import com.multimodal.rag.repository.ChatSessionRepository;
import com.multimodal.rag.repository.UserRepository;
import com.multimodal.rag.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "对话管理", description = "RAG 对话、会话管理相关接口")
public class ChatController {

    private final RagService ragService;
    private final UserRepository userRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    private User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    // ===== Chat with RAG =====

    @PostMapping
    @Operation(
        summary = "发送对话消息",
        description = "向 AI 助手发送问题，基于用户知识库进行 RAG 检索并返回答案，包含来源引用"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "对话成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<Map<String, Object>> chat(
            @Parameter(description = "对话请求，包含 query（问题）和可选的 sessionId（会话ID）")
            @RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        Long sessionId = request.get("sessionId") != null
                ? Long.parseLong(request.get("sessionId").toString()) : null;

        User user = getCurrentUser();

        // Create or update session
        ChatSession session;
        if (sessionId != null) {
            session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));
        } else {
            // Auto-create a new session with query as title
            String title = query.length() > 50 ? query.substring(0, 50) + "..." : query;
            session = ChatSession.builder()
                    .userId(user.getId())
                    .title(title)
                    .build();
            session = sessionRepository.save(session);
        }

        // Save user message
        ChatMessage userMsg = ChatMessage.builder()
                .sessionId(session.getId())
                .role("user")
                .content(query)
                .build();
        messageRepository.save(userMsg);

        // RAG with sources
        RagService.RagResult result = ragService.chatWithSources(query, user.getId());

        // Save assistant message with sources
        String sourcesJson = null;
        if (!result.sources().isEmpty()) {
            try {
                sourcesJson = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(result.sources());
            } catch (Exception ignored) {}
        }

        ChatMessage assistantMsg = ChatMessage.builder()
                .sessionId(session.getId())
                .role("assistant")
                .content(result.answer())
                .sources(sourcesJson)
                .build();
        messageRepository.save(assistantMsg);

        // Update session timestamp
        session.setUpdatedAt(java.time.LocalDateTime.now());
        sessionRepository.save(session);

        return ResponseEntity.ok(Map.of(
                "answer", result.answer(),
                "sources", result.sources(),
                "sessionId", session.getId()
        ));
    }

    // ===== Session management =====

    @GetMapping("/sessions")
    @Operation(
        summary = "获取会话列表",
        description = "获取当前用户的所有对话会话，按更新时间倒序排列"
    )
    public ResponseEntity<List<ChatSession>> listSessions() {
        User user = getCurrentUser();
        return ResponseEntity.ok(sessionRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(
        summary = "获取会话消息",
        description = "获取指定会话的所有消息记录，按时间顺序排列"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "会话不存在")
    })
    public ResponseEntity<List<ChatMessage>> getMessages(
            @Parameter(description = "会话ID")
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
    }

    @PostMapping("/sessions")
    @Operation(
        summary = "创建新会话",
        description = "创建一个新的对话会话"
    )
    public ResponseEntity<ChatSession> createSession(
            @Parameter(description = "会话信息，包含 title（标题）")
            @RequestBody Map<String, String> request) {
        User user = getCurrentUser();
        String title = request.getOrDefault("title", "新会话");
        ChatSession session = ChatSession.builder()
                .userId(user.getId())
                .title(title)
                .build();
        return ResponseEntity.ok(sessionRepository.save(session));
    }

    @PutMapping("/sessions/{sessionId}")
    @Operation(
        summary = "更新会话标题",
        description = "修改指定会话的标题"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "会话不存在")
    })
    public ResponseEntity<ChatSession> updateSession(
            @Parameter(description = "会话ID")
            @PathVariable Long sessionId,
            @Parameter(description = "会话标题")
            @RequestBody Map<String, String> request) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setTitle(request.get("title"));
        return ResponseEntity.ok(sessionRepository.save(session));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Transactional
    @Operation(
        summary = "删除会话",
        description = "删除指定会话及其所有消息记录"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "会话不存在")
    })
    public ResponseEntity<Void> deleteSession(
            @Parameter(description = "会话ID")
            @PathVariable Long sessionId) {
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
        return ResponseEntity.noContent().build();
    }
}
