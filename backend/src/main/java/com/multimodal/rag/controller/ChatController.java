package com.multimodal.rag.controller;

import com.multimodal.rag.model.ChatMessage;
import com.multimodal.rag.model.ChatSession;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.ChatMessageRepository;
import com.multimodal.rag.repository.ChatSessionRepository;
import com.multimodal.rag.repository.UserRepository;
import com.multimodal.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
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
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
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
    public ResponseEntity<List<ChatSession>> listSessions() {
        User user = getCurrentUser();
        return ResponseEntity.ok(sessionRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable Long sessionId) {
        return ResponseEntity.ok(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
    }

    @PostMapping("/sessions")
    public ResponseEntity<ChatSession> createSession(@RequestBody Map<String, String> request) {
        User user = getCurrentUser();
        String title = request.getOrDefault("title", "新会话");
        ChatSession session = ChatSession.builder()
                .userId(user.getId())
                .title(title)
                .build();
        return ResponseEntity.ok(sessionRepository.save(session));
    }

    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatSession> updateSession(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> request) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setTitle(request.get("title"));
        return ResponseEntity.ok(sessionRepository.save(session));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Transactional
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) {
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
        return ResponseEntity.noContent().build();
    }
}
