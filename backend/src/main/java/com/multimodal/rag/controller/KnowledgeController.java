package com.multimodal.rag.controller;

import com.multimodal.rag.model.DocumentAccessLog;
import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.UserRepository;
import com.multimodal.rag.repository.DocumentAccessLogRepository;
import com.multimodal.rag.repository.MultimodalDocumentRepository;
import com.multimodal.rag.service.KnowledgeService;
import com.multimodal.rag.service.OssStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.Path;
import java.net.MalformedURLException;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final UserRepository userRepository;
    private final DocumentAccessLogRepository accessLogRepository;
    private final MultimodalDocumentRepository documentRepository;
    private final OssStorageService ossStorageService;

    private User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @PostMapping("/upload")
    public ResponseEntity<MultimodalDocument> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tags", required = false) String tags) throws IOException {
        MultimodalDocument doc = knowledgeService.uploadDocument(file, getCurrentUser());
        if (tags != null && !tags.isBlank()) {
            doc.setTags(tags);
            documentRepository.save(doc);
        }
        return ResponseEntity.ok(doc);
    }

    @GetMapping("/documents")
    public ResponseEntity<List<MultimodalDocument>> list() {
        return ResponseEntity.ok(knowledgeService.getAllDocuments(getCurrentUser()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws IOException {
        knowledgeService.deleteDocument(id, getCurrentUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle-public")
    public ResponseEntity<MultimodalDocument> togglePublic(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeService.togglePublicStatus(id, getCurrentUser()));
    }

    @PutMapping("/{id}/tags")
    public ResponseEntity<MultimodalDocument> updateTags(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        MultimodalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        User currentUser = getCurrentUser();
        boolean isOwner = doc.getUser() != null && doc.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getName()));
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("无权修改此文档");
        }
        doc.setTags(request.get("tags"));
        return ResponseEntity.ok(documentRepository.save(doc));
    }

    @GetMapping("/view/{id}")
    @Transactional
    public ResponseEntity<?> view(@PathVariable Long id) throws MalformedURLException {
        MultimodalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Permission check: owner, admin, or public doc
        User currentUser = getCurrentUser();
        boolean isOwner = doc.getUser() != null && doc.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getName()));
        boolean isPublic = doc.isShared();

        if (!isOwner && !isAdmin && !isPublic) {
            return ResponseEntity.status(403).body(Map.of("error", "无权查看此文档"));
        }

        // Increment view count
        doc.setViewCount((doc.getViewCount() != null ? doc.getViewCount() : 0) + 1);
        documentRepository.save(doc);

        // Log the access
        DocumentAccessLog logEntry = new DocumentAccessLog();
        logEntry.setDocumentId(id);
        logEntry.setUserId(currentUser.getId());
        logEntry.setAccessType(DocumentAccessLog.AccessType.VIEW);
        accessLogRepository.save(logEntry);

        return serveFile(doc);
    }

    /** Get extracted content for authenticated user (own doc or public doc) */
    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detail(@PathVariable Long id) {
        MultimodalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        User currentUser = getCurrentUser();
        boolean isOwner = doc.getUser() != null && doc.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getName()));
        if (!doc.isShared() && !isOwner && !isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "无权查看此文档"));
        }
        return ResponseEntity.ok(buildDetailMap(doc));
    }

    // ===== Public endpoints (no auth required) =====

    @GetMapping("/public")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> listPublic(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<MultimodalDocument> result;

        if (keyword != null && !keyword.isBlank()) {
            result = documentRepository.searchPublic(keyword.trim(), pageable);
        } else if (tag != null && !tag.isBlank()) {
            result = documentRepository.findPublicByTag(tag.trim(), pageable);
        } else {
            // Sorted by viewCount desc, then uploadTime desc
            result = documentRepository.findPublicDocsSorted(pageable);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", result.getContent());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("number", result.getNumber());
        response.put("size", result.getSize());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/tags")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> listTags() {
        List<String> rawTags = documentRepository.findAllPublicTags();
        Map<String, Integer> tagCounts = new HashMap<>();
        for (String tagStr : rawTags) {
            if (tagStr == null || tagStr.isBlank()) continue;
            for (String t : tagStr.split(",")) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) {
                    tagCounts.merge(trimmed, 1, Integer::sum);
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        tagCounts.forEach((name, count) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", name);
            m.put("count", count);
            result.add(m);
        });
        result.sort((a, b) -> (int) b.get("count") - (int) a.get("count"));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/public/count")
    public ResponseEntity<Long> publicCount() {
        return ResponseEntity.ok(documentRepository.countBySharedTrueAndStatus("COMPLETED"));
    }

    /** Public view — no auth required, only works for public documents */
    @GetMapping("/public/view/{id}")
    @Transactional
    public ResponseEntity<?> publicView(@PathVariable Long id) throws MalformedURLException {
        MultimodalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        if (!doc.isShared()) {
            return ResponseEntity.status(403).body(Map.of("error", "该文档不是公开文档"));
        }
        doc.setViewCount((doc.getViewCount() != null ? doc.getViewCount() : 0) + 1);
        documentRepository.save(doc);

        return serveFile(doc);
    }

    /** Get document detail (extracted content) — public doc */
    @GetMapping("/public/detail/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> publicDetail(@PathVariable Long id) {
        MultimodalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        if (!doc.isShared()) {
            return ResponseEntity.status(403).body(Map.of("error", "该文档不是公开文档"));
        }
        return ResponseEntity.ok(buildDetailMap(doc));
    }

    /** Get recommended/hot public documents */
    @GetMapping("/public/hot")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MultimodalDocument>> hotDocs(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(documentRepository.findHotPublicDocs(PageRequest.of(0, limit)));
    }

    // ===== Helpers =====

    private Map<String, Object> buildDetailMap(MultimodalDocument doc) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", doc.getId());
        detail.put("fileName", doc.getFileName());
        detail.put("fileType", doc.getFileType());
        detail.put("extractedContent", doc.getExtractedContent());
        detail.put("tags", doc.getTags());
        detail.put("viewCount", doc.getViewCount());
        detail.put("uploadTime", doc.getUploadTime());
        detail.put("isPublic", doc.isShared());
        return detail;
    }

    private static final MediaType TEXT_UTF8 = new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8);

    private ResponseEntity<?> serveFile(MultimodalDocument doc) throws MalformedURLException {
        String storagePath = doc.getFilePath();

        // Seed documents with no real file — return the extracted content as text
        if (storagePath == null || storagePath.isBlank()) {
            String content = doc.getExtractedContent() != null ? doc.getExtractedContent() : "暂无内容";
            return ResponseEntity.ok()
                    .contentType(TEXT_UTF8)
                    .body(content);
        }

        // If stored in OSS, redirect to pre-signed URL
        if (storagePath.startsWith("oss://")) {
            String ossKey = storagePath.substring(6);
            try {
                String presignedUrl = ossStorageService.generatePresignedUrl(ossKey);
                return ResponseEntity.status(302)
                        .header(HttpHeaders.LOCATION, presignedUrl)
                        .build();
            } catch (Exception e) {
                // OSS file not found, return extracted content
                String content = doc.getExtractedContent() != null ? doc.getExtractedContent() : "文件不可用";
                return ResponseEntity.ok().contentType(TEXT_UTF8).body(content);
            }
        }

        // Local file fallback
        Path filePath = java.nio.file.Paths.get(storagePath);
        if (!filePath.toFile().exists()) {
            // File doesn't exist on disk (seed docs), return extracted content
            String content = doc.getExtractedContent() != null ? doc.getExtractedContent() : "文件不可用";
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(content);
        }
        Resource resource = new UrlResource(filePath.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
