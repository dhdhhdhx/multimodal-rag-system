package com.multimodal.rag.controller;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.MultimodalDocumentRepository;
import com.multimodal.rag.repository.UserRepository;
import com.multimodal.rag.service.AdminService;
import com.multimodal.rag.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.io.IOException;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "后台管理", description = "系统管理员专用接口，需要 ADMIN 角色权限")
public class AdminController {

    private final AdminService adminService;
    private final MultimodalDocumentRepository documentRepository;
    private final KnowledgeService knowledgeService;
    private final UserRepository userRepository;

    // ===== User Management =====

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "获取所有用户",
        description = "获取系统中所有用户列表，需要管理员权限"
    )
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "启用/禁用用户",
        description = "切换用户的激活状态，禁用后用户无法登录"
    )
    public ResponseEntity<Map<String, Object>> toggleUserActive(
            @Parameter(description = "用户ID")
            @PathVariable Long id) {
        User user = adminService.toggleUserActive(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "user", user,
                "message", user.getIsActive() ? "用户已启用" : "用户已禁用"
        ));
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "修改用户角色",
        description = "修改用户的系统角色（USER 或 ADMIN）"
    )
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @Parameter(description = "用户ID")
            @PathVariable Long id,
            @Parameter(description = "角色信息，包含 roleName 字段")
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
    @Operation(
        summary = "更新用户信息",
        description = "修改用户的基本信息（姓名、邮箱等）"
    )
    public ResponseEntity<Map<String, Object>> updateUser(
            @Parameter(description = "用户ID")
            @PathVariable Long id,
            @Parameter(description = "用户信息")
            @RequestBody Map<String, String> request) {
        User user = adminService.updateUserInfo(id,
                request.get("fullName"),
                request.get("email"));
        return ResponseEntity.ok(Map.of("success", true, "user", user));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "获取系统统计",
        description = "获取系统的关键统计数据（用户数、文档数、存储使用等）"
    )
    public ResponseEntity<Map<String, Object>> getSystemStatistics() {
        return ResponseEntity.ok(adminService.getSystemStatistics());
    }

    // ===== Document Management =====

    @GetMapping("/documents")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "获取所有文档",
        description = "分页获取系统中所有文档，支持关键词、标签、状态筛选和排序"
    )
    public ResponseEntity<Map<String, Object>> listAllDocuments(
            @Parameter(description = "页码，从0开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "关键词搜索（文件名、内容）")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "按标签筛选")
            @RequestParam(required = false) String tag,
            @Parameter(description = "按状态筛选（PROCESSING/COMPLETED/FAILED）")
            @RequestParam(required = false) String status,
            @Parameter(description = "排序字段（viewCount/uploadTime/fileName/fileSize）")
            @RequestParam(defaultValue = "uploadTime") String sortBy,
            @Parameter(description = "排序方向（asc/desc）")
            @RequestParam(defaultValue = "desc") String sortOrder) {

        // Whitelist sortable fields to prevent injection
        String sortField = switch (sortBy) {
            case "viewCount", "uploadTime", "fileName", "fileSize" -> sortBy;
            default -> "uploadTime";
        };
        Sort sort = Sort.by("asc".equalsIgnoreCase(sortOrder)
                ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
        PageRequest pageable = PageRequest.of(page, size, sort);

        // Build dynamic query using JPA Specification
        Specification<MultimodalDocument> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("fileName")), kw),
                    cb.like(cb.lower(root.get("extractedContent")), kw)));
            }
            if (tag != null && !tag.isBlank()) {
                predicates.add(cb.like(root.get("tags"), "%" + tag.trim() + "%"));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.trim().toUpperCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<MultimodalDocument> result = documentRepository.findAll(spec, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", result.getContent());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("number", result.getNumber());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/documents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "更新文档信息",
        description = "修改文档的标签、公开状态、提取内容等信息"
    )
    public ResponseEntity<MultimodalDocument> updateDocument(
            @Parameter(description = "文档ID")
            @PathVariable Long id,
            @Parameter(description = "文档信息，包含 tags/shared/extractedContent 等字段")
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

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "删除文档",
        description = "管理员删除指定文档，同时删除向量数据和文件"
    )
    public ResponseEntity<Void> deleteDocument(
            @Parameter(description = "文档ID")
            @PathVariable Long id) throws IOException {
        // Reuse KnowledgeService.deleteDocument which handles:
        // - permission check (admin bypasses)
        // - file deletion (OSS or local)
        // - vector store cleanup
        // - MySQL record removal
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        knowledgeService.deleteDocument(id, admin);
        return ResponseEntity.noContent().build();
    }

    // ===== Tag Management =====

    @GetMapping("/tags")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "获取所有标签",
        description = "获取系统中所有标签及其使用次数，按使用次数降序排列"
    )
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
    @Operation(
        summary = "重命名标签",
        description = "将所有使用旧标签的文档更新为新标签"
    )
    public ResponseEntity<Map<String, Object>> renameTag(
            @Parameter(description = "标签重命名信息，包含 oldName 和 newName 字段")
            @RequestBody Map<String, String> request) {
        String oldName = request.get("oldName");
        String newName = request.get("newName");
        int count = adminService.renameTag(oldName, newName);
        return ResponseEntity.ok(Map.of("success", true, "affected", count));
    }

    @DeleteMapping("/tags/{tagName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "删除标签",
        description = "从所有文档中移除指定标签"
    )
    public ResponseEntity<Map<String, Object>> deleteTag(
            @Parameter(description = "标签名称")
            @PathVariable String tagName) {
        int count = adminService.removeTag(tagName);
        return ResponseEntity.ok(Map.of("success", true, "affected", count));
    }

    // ===== Enhanced Statistics =====

    @GetMapping("/statistics/hot-keywords")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "获取热门关键词",
        description = "获取近期查询中最热门的关键词统计"
    )
    public ResponseEntity<List<Map<String, Object>>> hotKeywords(
            @Parameter(description = "统计天数")
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminService.getHotKeywords(days));
    }

    @GetMapping("/statistics/user-activity")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "获取用户活动统计",
        description = "获取近期用户活动数据（上传、查询等操作统计）"
    )
    public ResponseEntity<List<Map<String, Object>>> userActivity(
            @Parameter(description = "统计天数")
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminService.getUserActivity(days));
    }
}
