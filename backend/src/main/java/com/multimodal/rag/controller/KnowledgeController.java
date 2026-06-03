package com.multimodal.rag.controller;

import com.multimodal.rag.model.DocumentAccessLog;
import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.UserRepository;
import com.multimodal.rag.repository.DocumentAccessLogRepository;
import com.multimodal.rag.repository.MultimodalDocumentRepository;
import com.multimodal.rag.service.KnowledgeService;
import com.multimodal.rag.service.OssStorageService;
import com.multimodal.rag.service.ViewCountBufferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "文档上传、查看、删除、公开状态管理等知识库相关接口")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final UserRepository userRepository;
    private final DocumentAccessLogRepository accessLogRepository;
    private final MultimodalDocumentRepository documentRepository;
    private final OssStorageService ossStorageService;
    private final ViewCountBufferService viewCountBufferService;

    private User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @PostMapping("/upload")
    @Operation(
        summary = "上传文档",
        description = "上传文件到知识库，支持多模态文档（PDF、图片、音频、视频等），自动提取内容并向量化"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "上传成功", content = @Content(schema = @Schema(implementation = MultimodalDocument.class))),
        @ApiResponse(responseCode = "400", description = "文件格式不支持或文件过大")
    })
    public ResponseEntity<MultimodalDocument> upload(
            @Parameter(description = "文件对象")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "文档标签，逗号分隔")
            @RequestParam(value = "tags", required = false) String tags) throws IOException {
        MultimodalDocument doc = knowledgeService.uploadDocument(file, getCurrentUser());
        if (tags != null && !tags.isBlank()) {
            doc.setTags(tags);
            documentRepository.save(doc);
        }
        return ResponseEntity.ok(doc);
    }

    @GetMapping("/documents")
    @Operation(
        summary = "获取文档列表",
        description = "获取当前用户的知识库文档列表，包含上传时间、状态、标签等信息"
    )
    public ResponseEntity<List<MultimodalDocument>> list() {
        return ResponseEntity.ok(knowledgeService.getAllDocuments(getCurrentUser()));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除文档",
        description = "删除指定文档，同时删除向量数据和文件"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "403", description = "无权删除此文档"),
        @ApiResponse(responseCode = "404", description = "文档不存在")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "文档ID")
            @PathVariable Long id) throws IOException {
        knowledgeService.deleteDocument(id, getCurrentUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle-public")
    @Operation(
        summary = "切换文档公开状态",
        description = "将文档设为公开或取消公开，公开文档可供其他用户访问"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "状态切换成功", content = @Content(schema = @Schema(implementation = MultimodalDocument.class))),
        @ApiResponse(responseCode = "403", description = "无权修改此文档"),
        @ApiResponse(responseCode = "404", description = "文档不存在")
    })
    public ResponseEntity<MultimodalDocument> togglePublic(
            @Parameter(description = "文档ID")
            @PathVariable Long id) {
        return ResponseEntity.ok(knowledgeService.togglePublicStatus(id, getCurrentUser()));
    }

    @PutMapping("/{id}/tags")
    @Operation(
        summary = "更新文档标签",
        description = "修改文档的标签，支持多标签（逗号分隔）"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "标签更新成功", content = @Content(schema = @Schema(implementation = MultimodalDocument.class))),
        @ApiResponse(responseCode = "403", description = "无权修改此文档"),
        @ApiResponse(responseCode = "404", description = "文档不存在")
    })
    public ResponseEntity<MultimodalDocument> updateTags(
            @Parameter(description = "文档ID")
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
    @Operation(
        summary = "查看原始文件",
        description = "获取文档原始文件，支持 OSS 和本地两种存储方式，可使用代理模式避免 CORS 问题"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "文件获取成功"),
        @ApiResponse(responseCode = "302", description = "重定向到 OSS 预签名 URL"),
        @ApiResponse(responseCode = "403", description = "无权查看此文档"),
        @ApiResponse(responseCode = "404", description = "文档不存在")
    })
    public ResponseEntity<?> view(
            @Parameter(description = "文档ID")
            @PathVariable Long id,
            @Parameter(description = "是否通过后端代理（避免 CORS）")
            @RequestParam(value = "proxy", defaultValue = "false") boolean proxy) throws MalformedURLException {
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

        // Increment view count via Redis buffer
        viewCountBufferService.recordView(id);

        // Log the access
        DocumentAccessLog logEntry = new DocumentAccessLog();
        logEntry.setDocumentId(id);
        logEntry.setUserId(currentUser.getId());
        logEntry.setAccessType(DocumentAccessLog.AccessType.VIEW);
        accessLogRepository.save(logEntry);

        return serveFile(doc, proxy);
    }

    /** Get extracted content for authenticated user (own doc or public doc) */
    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    @Operation(
        summary = "获取文档详情",
        description = "获取文档详细信息，包括提取的内容、标签、浏览次数等"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "403", description = "无权查看此文档"),
        @ApiResponse(responseCode = "404", description = "文档不存在")
    })
    public ResponseEntity<?> detail(
            @Parameter(description = "文档ID")
            @PathVariable Long id) {
        MultimodalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        User currentUser = getCurrentUser();
        boolean isOwner = doc.getUser() != null && doc.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getName()));
        if (!doc.isShared() && !isOwner && !isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "无权查看此文档"));
        }
        viewCountBufferService.recordView(id);
        return ResponseEntity.ok(buildDetailMap(doc));
    }

    // ===== Public endpoints (no auth required) =====

    @GetMapping("/public")
    @Transactional(readOnly = true)
    @Operation(
        summary = "获取公开文档列表",
        description = "分页获取所有公开文档，支持按标签或关键词筛选，按浏览量和上传时间排序"
    )
    public ResponseEntity<Map<String, Object>> listPublic(
            @Parameter(description = "页码，从0开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "9") int size,
            @Parameter(description = "按标签筛选")
            @RequestParam(required = false) String tag,
            @Parameter(description = "关键词搜索")
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
    @Operation(
        summary = "获取公开文档标签列表",
        description = "获取所有公开文档的标签及其使用次数，按使用次数降序排列"
    )
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
    @Operation(
        summary = "获取公开文档总数",
        description = "返回已完成处理的公开文档总数"
    )
    public ResponseEntity<Long> publicCount() {
        return ResponseEntity.ok(documentRepository.countBySharedTrueAndStatus("COMPLETED"));
    }

    /** Public view — no auth required, only works for public documents */
    @GetMapping("/public/view/{id}")
    @Transactional
    @Operation(
        summary = "查看公开文档原始文件",
        description = "无需认证即可查看公开文档的原始文件"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "文件获取成功"),
        @ApiResponse(responseCode = "302", description = "重定向到 OSS 预签名 URL"),
        @ApiResponse(responseCode = "403", description = "该文档不是公开文档"),
        @ApiResponse(responseCode = "404", description = "文档不存在")
    })
    public ResponseEntity<?> publicView(
            @Parameter(description = "文档ID")
            @PathVariable Long id,
            @Parameter(description = "是否通过后端代理（避免 CORS）")
            @RequestParam(value = "proxy", defaultValue = "false") boolean proxy) throws MalformedURLException {
        MultimodalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        if (!doc.isShared()) {
            return ResponseEntity.status(403).body(Map.of("error", "该文档不是公开文档"));
        }
        // Increment view count via Redis buffer
        viewCountBufferService.recordView(id);

        return serveFile(doc, proxy);
    }

    /** Get document detail (extracted content) — public doc */
    @GetMapping("/public/detail/{id}")
    @Transactional(readOnly = true)
    @Operation(
        summary = "获取公开文档详情",
        description = "无需认证即可获取公开文档的详细信息"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "403", description = "该文档不是公开文档"),
        @ApiResponse(responseCode = "404", description = "文档不存在")
    })
    public ResponseEntity<?> publicDetail(
            @Parameter(description = "文档ID")
            @PathVariable Long id) {
        MultimodalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        if (!doc.isShared()) {
            return ResponseEntity.status(403).body(Map.of("error", "该文档不是公开文档"));
        }
        viewCountBufferService.recordView(id);
        return ResponseEntity.ok(buildDetailMap(doc));
    }

    /** Get recommended/hot public documents */
    @GetMapping("/public/hot")
    @Transactional(readOnly = true)
    @Operation(
        summary = "获取热门公开文档",
        description = "获取浏览量最高的公开文档列表"
    )
    public ResponseEntity<List<MultimodalDocument>> hotDocs(
            @Parameter(description = "返回数量限制")
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
        detail.put("viewCount", (doc.getViewCount() != null ? doc.getViewCount() : 0)
                + viewCountBufferService.getBufferedViewCount(doc.getId()));
        detail.put("uploadTime", doc.getUploadTime());
        detail.put("isPublic", doc.isShared());
        return detail;
    }

    private static final MediaType TEXT_UTF8 = new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8);

    private ResponseEntity<?> serveFile(MultimodalDocument doc, boolean proxy) throws MalformedURLException {
        String storagePath = doc.getFilePath();

        // Seed documents with no real file — return the extracted content as text
        if (storagePath == null || storagePath.isBlank()) {
            String content = doc.getExtractedContent() != null ? doc.getExtractedContent() : "暂无内容";
            return ResponseEntity.ok()
                    .contentType(TEXT_UTF8)
                    .body(content);
        }

        // If stored in OSS
        if (storagePath.startsWith("oss://")) {
            String ossKey = storagePath.substring(6);
            try {
                if (proxy) {
                    // Proxy mode: backend fetches from OSS and streams back to client
                    // This avoids CORS issues and expired presigned URLs on the client side
                    InputStream is = ossStorageService.getFileStream(ossKey);
                    String contentType = guessContentType(ossKey);
                    byte[] bytes = is.readAllBytes();
                    is.close();
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "inline; filename=\"" + doc.getFileName() + "\"")
                            .body(bytes);
                } else {
                    // Redirect mode: return 302 to a fresh presigned URL
                    String presignedUrl = ossStorageService.generatePresignedUrl(ossKey);
                    return ResponseEntity.status(302)
                            .header(HttpHeaders.LOCATION, presignedUrl)
                            .build();
                }
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

    /** Guess content type from OSS key (file extension) */
    private static String guessContentType(String key) {
        String lower = key.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".avi")) return "video/x-msvideo";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".txt") || lower.endsWith(".md")) return "text/plain; charset=utf-8";
        if (lower.endsWith(".csv")) return "text/csv; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".xml")) return "application/xml";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        return "application/octet-stream";
    }
}
