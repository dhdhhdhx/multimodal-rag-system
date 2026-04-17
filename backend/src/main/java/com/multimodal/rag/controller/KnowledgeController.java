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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    @Operation(
        summary = "查看原始文件",
        description = "获取文档原始文件，支持 OSS 和本地两种存储方式，可使用代理模式避免 CORS 问题。已认证用户可以查看自己的文档和公开文档，ADMIN 可查看所有文档。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "文件获取成功"),
        @ApiResponse(responseCode = "302", description = "重定向到 OSS 预签名 URL"),
        @ApiResponse(responseCode = "403", description = "无权查看此文档"),
        @ApiResponse(responseCode = "404", description = "文档不存在")
    })
    public void view(
            @Parameter(description = "文档ID")
            @PathVariable Long id,
            @Parameter(description = "是否通过后端代理（避免 CORS）")
            @RequestParam(value = "proxy", defaultValue = "false") boolean proxy,
            jakarta.servlet.http.HttpServletResponse response) throws Exception {
        MultimodalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // 检查认证状态
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());

        // 如果文档不是公开的，要求用户必须认证
        if (!doc.isShared() && !isAuthenticated) {
            response.setStatus(403);
            response.getWriter().write(Map.of("error", "需要登录才能查看此文档").toString());
            return;
        }

        User currentUser = null;
        boolean isOwner = false;
        boolean isAdmin = false;

        // 如果用户已认证，获取用户信息并检查权限
        if (isAuthenticated) {
            try {
                currentUser = getCurrentUser();
                isOwner = doc.getUser() != null && doc.getUser().getId().equals(currentUser.getId());
                isAdmin = currentUser.getRoles().stream()
                        .anyMatch(r -> "ADMIN".equals(r.getName()));
            } catch (Exception e) {
                // 获取用户信息失败，当作未认证处理
                isAuthenticated = false;
            }
        }

        // 权限检查：文档所有者、ADMIN 或公开文档
        if (!isOwner && !isAdmin && !doc.isShared()) {
            response.setStatus(403);
            response.getWriter().write(Map.of("error", "无权查看此文档").toString());
            return;
        }

        // 异步记录浏览计数和访问日志（不阻塞文件返回）
        final User finalCurrentUser = currentUser;
        final boolean finalIsAuthenticated = isAuthenticated;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // 使用 Redis 缓冲浏览计数，避免行锁竞争
                viewCountBufferService.recordView(id);

                // Log the access (only for authenticated users)
                if (finalIsAuthenticated && finalCurrentUser != null) {
                    DocumentAccessLog logEntry = new DocumentAccessLog();
                    logEntry.setDocumentId(id);
                    logEntry.setUserId(finalCurrentUser.getId());
                    logEntry.setAccessType(DocumentAccessLog.AccessType.VIEW);
                    accessLogRepository.save(logEntry);
                }
            } catch (Exception e) {
                // 异步操作失败不影响文件返回
                org.slf4j.LoggerFactory.getLogger(KnowledgeController.class)
                    .warn("Failed to record view log for document {}", id, e);
            }
        });

        serveFile(doc, proxy, response);
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
    public void publicView(
            @Parameter(description = "文档ID")
            @PathVariable Long id,
            @Parameter(description = "是否通过后端代理（避免 CORS）")
            @RequestParam(value = "proxy", defaultValue = "false") boolean proxy,
            jakarta.servlet.http.HttpServletResponse response) throws Exception {
        MultimodalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        if (!doc.isShared()) {
            response.setStatus(403);
            response.getWriter().write(Map.of("error", "该文档不是公开文档").toString());
            return;
        }

        // 异步更新浏览计数（不阻塞文件返回）
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                doc.setViewCount((doc.getViewCount() != null ? doc.getViewCount() : 0) + 1);
                documentRepository.save(doc);
            } catch (Exception e) {
                // 异步操作失败不影响文件返回
                org.slf4j.LoggerFactory.getLogger(KnowledgeController.class)
                    .warn("Failed to update view count for document {}", id, e);
            }
        });

        serveFile(doc, proxy, response);
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
        detail.put("viewCount", doc.getViewCount());
        detail.put("uploadTime", doc.getUploadTime());
        detail.put("isPublic", doc.isShared());
        return detail;
    }

    private static final MediaType TEXT_UTF8 = new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8);

    private void serveFile(MultimodalDocument doc, boolean proxy, jakarta.servlet.http.HttpServletResponse response) throws Exception {
        String storagePath = doc.getFilePath();

        // Seed documents with no real file — return the extracted content as text
        if (storagePath == null || storagePath.isBlank()) {
            String content = doc.getExtractedContent();
            if (content == null || content.isBlank()) {
                content = "该文档没有原始文件内容";
            }
            // 如果内容太长（可能是RAG检索返回的上下文），截断显示
            if (content.length() > 5000) {
                content = content.substring(0, 5000) + "\n\n... (内容已截断，完整内容请通过在线预览查看)";
            }
            response.setContentType("text/plain; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(content);
            return;
        }

        // If stored in OSS
        if (storagePath.startsWith("oss://")) {
            String ossKey = storagePath.substring(6);
            try {
                if (proxy) {
                    // Proxy mode: backend fetches from OSS and streams back to client
                    // This avoids CORS issues and expired presigned URLs on the client side
                    String contentType = guessContentType(ossKey);
                    response.setContentType(contentType);

                    // Office 文件和 PDF 使用 attachment 头触发下载，其他文件使用 inline 预览
                    String dispositionType = isDownloadFile(doc.getFileName()) ? "attachment" : "inline";
                    if ("inline".equals(dispositionType)) {
                        // 对于 inline 预览的文件，不设置 Content-Disposition 避免标题乱码
                        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline");
                    } else {
                        // 对于 attachment 下载的文件，使用 RFC 5987 编码
                        String encodedFileName = encodeFileName(doc.getFileName());
                        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                                dispositionType + "; filename*=UTF-8''" + encodedFileName);
                    }

                    // 流式传输文件
                    InputStream is = ossStorageService.getFileStream(ossKey);
                    byte[] buffer = new byte[8192];
                    int read;
                    java.io.OutputStream os = response.getOutputStream();
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    is.close();
                    os.flush();
                    return;
                } else {
                    // Redirect mode: return 302 to a fresh presigned URL
                    String presignedUrl = ossStorageService.generatePresignedUrl(ossKey);
                    response.setStatus(302);
                    response.setHeader(HttpHeaders.LOCATION, presignedUrl);
                    return;
                }
            } catch (Exception e) {
                // OSS file not found, return extracted content
                String content = doc.getExtractedContent() != null ? doc.getExtractedContent() : "文件不可用";
                response.setContentType("text/plain; charset=UTF-8");
                response.getWriter().write(content);
                return;
            }
        }

        // Local file fallback
        Path filePath = java.nio.file.Paths.get(storagePath);
        if (!filePath.toFile().exists()) {
            // File doesn't exist on disk (seed docs), return extracted content
            String content = doc.getExtractedContent() != null ? doc.getExtractedContent() : "文件不可用";
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write(content);
            return;
        }

        String contentType = guessContentType(doc.getFileName());
        response.setContentType(contentType);

        // Office 文件和 PDF 使用 attachment 头触发下载，其他文件使用 inline 预览
        String dispositionType = isDownloadFile(doc.getFileName()) ? "attachment" : "inline";
        if ("inline".equals(dispositionType)) {
            // 对于 inline 预览的文件，不设置 Content-Disposition 避免标题乱码
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline");
        } else {
            // 对于 attachment 下载的文件，使用 RFC 5987 编码
            String encodedFileName = encodeFileName(doc.getFileName());
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    dispositionType + "; filename*=UTF-8''" + encodedFileName);
        }

        // 流式传输本地文件
        java.io.OutputStream os = response.getOutputStream();
        java.nio.file.Files.copy(filePath, os);
        os.flush();
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

    /**
     * 编码文件名用于 Content-Disposition header
     * 使用 RFC 5987 格式: filename*=UTF-8''URL编码
     */
    private String encodeFileName(String fileName) {
        if (fileName == null) {
            return "file";
        }
        try {
            // 只对非 ASCII 字符进行编码，保留 ASCII 字符不变
            StringBuilder sb = new StringBuilder();
            for (char c : fileName.toCharArray()) {
                if (c > 127) {
                    // 非 ASCII 字符：URL 编码
                    sb.append(URLEncoder.encode(String.valueOf(c), StandardCharsets.UTF_8.name()));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString().replace("+", "%20");
        } catch (Exception e) {
            // 降级：移除非 ASCII 字符
            return fileName.replaceAll("[^\\x00-\\x7F]", "_");
        }
    }

    /**
     * 判断是否为需要下载的文件类型
     * PDF、Office 文件无法在浏览器中直接预览，应使用 attachment 头触发下载
     */
    private static boolean isDownloadFile(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".pdf")
                || lower.endsWith(".doc") || lower.endsWith(".docx")
                || lower.endsWith(".xls") || lower.endsWith(".xlsx")
                || lower.endsWith(".ppt") || lower.endsWith(".pptx");
    }

    /**
     * 判断是否为可以预览的文件类型
     * 图片、音频、视频、文本文件可以在浏览器中直接预览
     */
    private static boolean isPreviewableFile(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        // 图片
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp")) {
            return true;
        }
        // 音频
        if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".ogg")
                || lower.endsWith(".m4a") || lower.endsWith(".flac")) {
            return true;
        }
        // 视频
        if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".avi")
                || lower.endsWith(".mov") || lower.endsWith(".mkv")) {
            return true;
        }
        // 文本
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".html")
                || lower.endsWith(".css") || lower.endsWith(".js") || lower.endsWith(".json")
                || lower.endsWith(".xml") || lower.endsWith(".csv")) {
            return true;
        }
        return false;
    }
}
