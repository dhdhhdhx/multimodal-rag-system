package com.multimodal.rag.service;

import com.multimodal.rag.model.*;
import com.multimodal.rag.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MultimodalDocumentRepository documentRepository;
    private final QueryLogRepository queryLogRepository;
    private final DocumentAccessLogRepository accessLogRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(!user.getIsActive());
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String normalizedRole = roleName == null ? "" : roleName.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("USER", "PREMIUM", "ADMIN").contains(normalizedRole)) {
            throw new RuntimeException("Unsupported role: " + roleName);
        }
        Role role = roleRepository.findByName(normalizedRole)
                .orElseThrow(() -> new RuntimeException("Role not found: " + normalizedRole));
        user.setRoles(new HashSet<>(Set.of(role)));
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserInfo(Long userId, String fullName, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (fullName != null) user.setFullName(fullName);
        if (email != null) user.setEmail(email);
        return userRepository.save(user);
    }

    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(User::getIsActive).count();

        long totalDocuments = documentRepository.count();
        long completedDocuments = documentRepository.findAll().stream()
                .filter(doc -> "COMPLETED".equals(doc.getStatus())).count();
        long publicDocuments = documentRepository.countBySharedTrueAndStatus("COMPLETED");

        long totalQueries = queryLogRepository.count();
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long queriesThisWeek = queryLogRepository.findAll().stream()
                .filter(log -> log.getCreatedAt().isAfter(weekAgo)).count();

        long totalAccesses = accessLogRepository.count();

        stats.put("users", Map.of(
                "total", totalUsers,
                "active", activeUsers,
                "inactive", totalUsers - activeUsers
        ));
        stats.put("documents", Map.of(
                "total", totalDocuments,
                "completed", completedDocuments,
                "processing", totalDocuments - completedDocuments,
                "public", publicDocuments
        ));
        stats.put("queries", Map.of(
                "total", totalQueries,
                "thisWeek", queriesThisWeek
        ));
        stats.put("accesses", Map.of(
                "total", totalAccesses
        ));

        return stats;
    }

    /** Rename a tag across all documents */
    @Transactional
    public int renameTag(String oldName, String newName) {
        List<MultimodalDocument> docs = documentRepository.findAll();
        int count = 0;
        for (MultimodalDocument doc : docs) {
            if (doc.getTags() == null) continue;
            List<String> tags = new ArrayList<>(Arrays.asList(doc.getTags().split(",")));
            boolean changed = false;
            for (int i = 0; i < tags.size(); i++) {
                if (tags.get(i).trim().equals(oldName)) {
                    tags.set(i, newName);
                    changed = true;
                }
            }
            if (changed) {
                doc.setTags(String.join(",", tags));
                documentRepository.save(doc);
                count++;
            }
        }
        return count;
    }

    /** Remove a tag from all documents */
    @Transactional
    public int removeTag(String tagName) {
        List<MultimodalDocument> docs = documentRepository.findAll();
        int count = 0;
        for (MultimodalDocument doc : docs) {
            if (doc.getTags() == null) continue;
            List<String> tags = Arrays.stream(doc.getTags().split(","))
                    .map(String::trim)
                    .filter(t -> !t.equals(tagName))
                    .collect(Collectors.toList());
            String newTags = String.join(",", tags);
            if (!newTags.equals(doc.getTags())) {
                doc.setTags(newTags.isEmpty() ? null : newTags);
                documentRepository.save(doc);
                count++;
            }
        }
        return count;
    }

    /** Get hot query keywords */
    public List<Map<String, Object>> getHotKeywords(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<QueryLog> logs = queryLogRepository.findAll().stream()
                .filter(l -> l.getCreatedAt().isAfter(since))
                .collect(Collectors.toList());

        // Simple word frequency analysis
        Map<String, Integer> wordCounts = new LinkedHashMap<>();
        for (QueryLog log : logs) {
            String query = log.getQueryText();
            if (query == null) continue;
            // Use the full query as a keyword unit
            wordCounts.merge(query.trim(), 1, Integer::sum);
        }

        return wordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("keyword", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** Get user activity stats */
    public List<Map<String, Object>> getUserActivity(int days) {
        log.info("===== 开始获取用户活跃度数据 (最近 {} 天) =====", days);

        // 使用 LocalDate 进行日期比较，避免时区问题
        LocalDate sinceDate = LocalDate.now().minusDays(days);
        log.info("筛选起始日期: {} (当天开始)", sinceDate);

        List<User> users = userRepository.findAll();
        log.info("系统中总用户数: {}", users.size());

        // 获取所有查询日志
        List<QueryLog> allQueries = queryLogRepository.findAll();
        log.info("数据库中总查询日志数: {}", allQueries.size());

        // 记录最近的几条日志时间，用于调试
        if (!allQueries.isEmpty()) {
            List<QueryLog> sampleLogs = allQueries.stream()
                .sorted(Comparator.comparing(QueryLog::getCreatedAt).reversed())
                .limit(5)
                .toList();
            log.info("最近5条查询日志时间:");
            for (QueryLog queryLog : sampleLogs) {
                String queryText = queryLog.getQueryText() != null
                    ? queryLog.getQueryText().substring(0, Math.min(50, queryLog.getQueryText().length()))
                    : "";
                log.info("  - 日志ID {}: 创建时间={}, 用户ID={}, 查询文本={}",
                    queryLog.getId(), queryLog.getCreatedAt(), queryLog.getUserId(), queryText);
            }
        }

        // 使用 LocalDate 比较而不是 LocalDateTime，更精确
        List<QueryLog> recentQueries = allQueries.stream()
                .filter(l -> {
                    LocalDate logDate = l.getCreatedAt().toLocalDate();
                    boolean isAfter = logDate.isAfter(sinceDate.minusDays(1)); // 包含当天
                    return isAfter;
                })
                .collect(Collectors.toList());

        log.info("筛选后的最近查询日志数: {}", recentQueries.size());

        // 获取所有访问日志
        List<DocumentAccessLog> allAccesses = accessLogRepository.findAll();
        log.info("数据库中总访问日志数: {}", allAccesses.size());

        List<DocumentAccessLog> recentAccesses = allAccesses.stream()
                .filter(l -> {
                    LocalDate logDate = l.getCreatedAt().toLocalDate();
                    boolean isAfter = logDate.isAfter(sinceDate.minusDays(1)); // 包含当天
                    return isAfter;
                })
                .collect(Collectors.toList());

        log.info("筛选后的最近访问日志数: {}", recentAccesses.size());

        // 按用户统计
        Map<Long, Long> queryCountByUser = recentQueries.stream()
                .filter(l -> l.getUserId() != null)
                .collect(Collectors.groupingBy(QueryLog::getUserId, Collectors.counting()));
        Map<Long, Long> accessCountByUser = recentAccesses.stream()
                .filter(l -> l.getUserId() != null)
                .collect(Collectors.groupingBy(DocumentAccessLog::getUserId, Collectors.counting()));

        log.info("有查询记录的用户数: {}", queryCountByUser.size());
        log.info("有访问记录的用户数: {}", accessCountByUser.size());

        // 构建返回结果
        List<Map<String, Object>> result = users.stream().map(user -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", user.getId());
            m.put("username", user.getUsername());
            m.put("fullName", user.getFullName());
            m.put("queryCount", queryCountByUser.getOrDefault(user.getId(), 0L));
            m.put("accessCount", accessCountByUser.getOrDefault(user.getId(), 0L));
            m.put("isActive", user.getIsActive());
            return m;
        }).sorted((a, b) -> Long.compare(
                (Long) b.get("queryCount") + (Long) b.get("accessCount"),
                (Long) a.get("queryCount") + (Long) a.get("accessCount")
        )).collect(Collectors.toList());

        log.info("===== 用户活跃度数据获取完成，返回 {} 条记录 =====", result.size());
        return result;
    }
}
