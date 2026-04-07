package com.multimodal.rag.controller;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.MultimodalDocumentRepository;
import com.multimodal.rag.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final MultimodalDocumentRepository documentRepository;

    // ===== User Management =====

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> toggleUserActive(@PathVariable Long id) {
        User user = adminService.toggleUserActive(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "user", user,
                "message", user.getIsActive() ? "用户已启用" : "用户已禁用"
        ));
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String roleName = request.get("roleName");
        User user = adminService.updateUserRole(id, roleName);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "user", user,
                "message", "角色已更新"
        ));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        User user = adminService.updateUserInfo(id,
                request.get("fullName"),
                request.get("email"));
        return ResponseEntity.ok(Map.of("success", true, "user", user));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemStatistics() {
        return ResponseEntity.ok(adminService.getSystemStatistics());
    }

    // ===== Document Management =====

    @GetMapping("/documents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> listAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadTime"));
        Page<MultimodalDocument> result;
        if (keyword != null && !keyword.isBlank()) {
            result = documentRepository.searchPublic(keyword.trim(), pageable);
        } else {
            result = documentRepository.findAll(pageable);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("content", result.getContent());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("number", result.getNumber());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/documents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MultimodalDocument> updateDocument(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        MultimodalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        if (request.containsKey("tags")) {
            doc.setTags((String) request.get("tags"));
        }
        if (request.containsKey("shared")) {
            doc.setShared((Boolean) request.get("shared"));
        }
        if (request.containsKey("extractedContent")) {
            doc.setExtractedContent((String) request.get("extractedContent"));
        }
        return ResponseEntity.ok(documentRepository.save(doc));
    }

    // ===== Tag Management =====

    @GetMapping("/tags")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listAllTags() {
        // Get tags from ALL documents (not just public)
        List<MultimodalDocument> allDocs = documentRepository.findAll();
        Map<String, Integer> tagCounts = new LinkedHashMap<>();
        for (MultimodalDocument doc : allDocs) {
            if (doc.getTags() == null || doc.getTags().isBlank()) continue;
            for (String t : doc.getTags().split(",")) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) {
                    tagCounts.merge(trimmed, 1, Integer::sum);
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        tagCounts.forEach((name, count) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("count", count);
            result.add(m);
        });
        result.sort((a, b) -> (int) b.get("count") - (int) a.get("count"));
        return ResponseEntity.ok(result);
    }

    @PutMapping("/tags/rename")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> renameTag(@RequestBody Map<String, String> request) {
        String oldName = request.get("oldName");
        String newName = request.get("newName");
        int count = adminService.renameTag(oldName, newName);
        return ResponseEntity.ok(Map.of("success", true, "affected", count));
    }

    @DeleteMapping("/tags/{tagName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteTag(@PathVariable String tagName) {
        int count = adminService.removeTag(tagName);
        return ResponseEntity.ok(Map.of("success", true, "affected", count));
    }

    // ===== Enhanced Statistics =====

    @GetMapping("/statistics/hot-keywords")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> hotKeywords(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminService.getHotKeywords(days));
    }

    @GetMapping("/statistics/user-activity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> userActivity(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminService.getUserActivity(days));
    }
}
