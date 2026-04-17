package com.multimodal.rag.service;

import com.multimodal.rag.repository.MultimodalDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 浏览计数缓冲服务
 * 使用 Redis 缓冲浏览增量，定时批量写入 MySQL，避免行锁竞争
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ViewCountBufferService {

    private final RedisTemplate<String, Long> redisTemplate;
    private final MultimodalDocumentRepository documentRepository;

    private static final String VIEW_COUNT_KEY_PREFIX = "doc:view:";
    private static final long BUFFER_FLUSH_INTERVAL_SECONDS = 60;

    /**
     * 记录文档浏览
     * 使用 Redis INCR 原子操作，无需担心并发问题
     */
    public void recordView(Long documentId) {
        try {
            String key = VIEW_COUNT_KEY_PREFIX + documentId;
            redisTemplate.opsForValue().increment(key);
            // 设置过期时间，防止冷数据永久占用内存
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
            log.debug("记录文档浏览: {}", documentId);
        } catch (Exception e) {
            log.warn("Redis 记录浏览失败，降级直接写 MySQL: documentId={}", documentId, e);
            // 降级方案：直接写 MySQL
            fallbackIncrementViewCount(documentId);
        }
    }

    /**
     * 定时任务：每 60 秒批量刷新缓冲的浏览计数到 MySQL
     */
    @Scheduled(fixedDelay = BUFFER_FLUSH_INTERVAL_SECONDS * 1000)
    @Transactional
    public void flushAllBufferedViewCounts() {
        try {
            Set<String> keys = redisTemplate.keys(VIEW_COUNT_KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }

            log.info("开始刷新浏览计数缓冲，共 {} 个文档", keys.size());
            int successCount = 0;
            int failureCount = 0;

            for (String key : keys) {
                try {
                    Long count = redisTemplate.opsForValue().get(key);
                    if (count != null && count > 0) {
                        String documentIdStr = key.substring(VIEW_COUNT_KEY_PREFIX.length());
                        Long documentId = Long.parseLong(documentIdStr);

                        // 原子更新 MySQL
                        int updated = documentRepository.incrementViewCountBy(documentId, count);
                        if (updated > 0) {
                            // 重置 Redis 缓冲
                            redisTemplate.opsForValue().set(key, 0L);
                            successCount++;
                        } else {
                            // 文档不存在，删除 Redis key 并记录
                            redisTemplate.delete(key);
                            log.warn("文档 {} 不存在，已清理 Redis key", documentId);
                            failureCount++;
                        }
                    }
                } catch (Exception e) {
                    log.error("刷新文档浏览计数失败: {}", key, e);
                    failureCount++;
                }
            }

            log.info("浏览计数刷新完成: 成功={}, 失败={}", successCount, failureCount);

        } catch (Exception e) {
            log.error("刷新浏览计数缓冲时发生错误", e);
        }
    }

    /**
     * 获取文档当前的缓冲浏览数（不包括已写入 MySQL 的）
     */
    public Long getBufferedViewCount(Long documentId) {
        try {
            String key = VIEW_COUNT_KEY_PREFIX + documentId;
            Long count = redisTemplate.opsForValue().get(key);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("获取缓冲浏览数失败: documentId={}", documentId, e);
            return 0L;
        }
    }

    /**
     * 降级方案：直接写 MySQL
     */
    @Transactional
    private void fallbackIncrementViewCount(Long documentId) {
        try {
            documentRepository.incrementViewCountBy(documentId, 1L);
        } catch (Exception e) {
            log.error("降级方案也失败: documentId={}", documentId, e);
        }
    }

    /**
     * 手动刷新指定文档的缓冲（用于测试或特殊场景）
     */
    @Transactional
    public boolean flushDocumentViewCount(Long documentId) {
        String key = VIEW_COUNT_KEY_PREFIX + documentId;

        try {
            // 检查文档是否存在
            boolean docExists = documentRepository.existsById(documentId);
            if (!docExists) {
                // 文档不存在，删除 Redis key 并记录
                redisTemplate.delete(key);
                log.warn("文档 {} 不存在，已清理 Redis key", documentId);
                return false;
            }

            // 文档存在，继续更新 view count
            Long count = redisTemplate.opsForValue().get(key);
            if (count != null && count > 0) {
                int updated = documentRepository.incrementViewCountBy(documentId, count);
                if (updated > 0) {
                    redisTemplate.opsForValue().set(key, 0L);
                    log.info("手动刷新文档浏览计数: documentId={}, count={}", documentId, count);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("手动刷新失败: documentId={}", documentId, e);
            return false;
        }
    }
}
