<template>
  <div class="article-detail">
    <div v-if="loading" class="loading-state">加载中...</div>
    <template v-else-if="article">
      <div class="detail-header">
        <router-link to="/" class="back-btn">
          <el-icon><ArrowLeft /></el-icon> 返回
        </router-link>
        <div class="article-tags">
          <el-tag v-for="tag in parseTags(article.tags)" :key="tag" size="small">{{ tag }}</el-tag>
        </div>
      </div>
      <h1 class="article-title">{{ article.fileName }}</h1>
      <div class="article-meta">
        <span>{{ formatTime(article.uploadTime) }}</span>
        <span>{{ article.viewCount || 0 }} 次浏览</span>
        <span>{{ article.fileType }}</span>
      </div>
      <div class="article-content">
        <div v-html="renderContent(article.extractedContent)" />
      </div>
      <div class="article-actions">
        <el-button type="primary" @click="viewOriginal">查看原始文件</el-button>
      </div>
    </template>
    <div v-else class="error-state">文章不存在或无权查看</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import api from '../api'

const route = useRoute()
const md = new MarkdownIt({ breaks: true })
const article = ref<any>(null)
const loading = ref(true)

const parseTags = (t: string) => t ? t.split(',').map(s => s.trim()).filter(Boolean) : []
const formatTime = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : ''

const renderContent = (content: string) => {
  if (!content) return '<p>暂无内容</p>'
  // If content starts with [xxx Content] markers, format nicely
  let formatted = content
    .replace(/\[([^\]]+) Content\]/g, '<h3>$1</h3>')
    .replace(/\[Audio Transcription\]/g, '<h3>音频转录</h3>')
    .replace(/\[([^\]]+) File\]/g, '<p><strong>文件类型：</strong>$1</p>')
  return md.render(formatted)
}

const viewOriginal = async () => {
  // Try public endpoint first (works for public docs without auth)
  const publicUrl = `/api/knowledge/public/view/${route.params.id}`
  try {
    const res = await fetch(publicUrl)
    if (res.ok) {
      // If redirected (OSS), open the redirect URL; otherwise download blob
      if (res.redirected) {
        window.open(res.url, '_blank')
      } else {
        const blob = await res.blob()
        const url = URL.createObjectURL(blob)
        window.open(url, '_blank')
      }
      return
    }
  } catch { /* fall through */ }

  // Fallback: use authenticated endpoint via fetch with token
  const token = localStorage.getItem('jwt_token')
  if (!token) { ElMessage.error('请先登录'); return }
  try {
    const res = await fetch(`/api/knowledge/view/${route.params.id}`, {
      headers: { 'Authorization': `Bearer ${token}` },
      redirect: 'follow'
    })
    if (res.ok) {
      if (res.redirected) {
        window.open(res.url, '_blank')
      } else {
        const blob = await res.blob()
        const url = URL.createObjectURL(blob)
        window.open(url, '_blank')
      }
    } else {
      ElMessage.error('无法查看原始文件')
    }
  } catch { ElMessage.error('请求失败') }
}

onMounted(async () => {
  const id = route.params.id
  try {
    // Try public endpoint first
    const res = await api.get(`/knowledge/public/detail/${id}`)
    article.value = res.data
  } catch {
    // Fall back to authenticated endpoint
    try {
      const res = await api.get(`/knowledge/detail/${id}`)
      article.value = res.data
    } catch { /* both failed */ }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.article-detail { max-width: 800px; margin: 0 auto; }
.detail-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.back-btn {
  display: flex; align-items: center; gap: 4px;
  color: var(--text-secondary); text-decoration: none; font-size: 14px;
}
.back-btn:hover { color: var(--accent); }
.article-tags { display: flex; gap: 6px; }
.article-title { font-size: 28px; font-weight: 700; color: var(--text-primary); margin: 0 0 12px; line-height: 1.3; }
.article-meta { font-size: 13px; color: var(--text-muted); display: flex; gap: 16px; margin-bottom: 32px; }
.article-content {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 32px;
  font-size: 15px;
  line-height: 1.8;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}
.article-content :deep(h1), .article-content :deep(h2), .article-content :deep(h3) {
  margin-top: 24px; margin-bottom: 12px;
}
.article-content :deep(p) { margin: 0 0 16px; }
.article-content :deep(pre) {
  background: #1e293b; color: #e2e8f0; padding: 16px; border-radius: 8px;
  overflow-x: auto; white-space: pre-wrap;
}
.article-content :deep(code) { background: rgba(0,0,0,0.06); padding: 2px 6px; border-radius: 4px; font-size: 14px; }
.article-content :deep(table) {
  border-collapse: collapse; width: 100%; margin: 16px 0;
}
.article-content :deep(th), .article-content :deep(td) {
  border: 1px solid var(--border-color); padding: 8px 12px; text-align: left;
}
.article-content :deep(th) { background: var(--bg-secondary); font-weight: 600; }
.article-actions { margin-top: 24px; display: flex; justify-content: center; }
.loading-state, .error-state { text-align: center; padding: 80px 0; color: var(--text-muted); font-size: 15px; }
</style>
