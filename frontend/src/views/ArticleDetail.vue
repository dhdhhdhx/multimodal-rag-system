<template>
  <div class="article-detail">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="0" animated>
        <template #template>
          <div class="skeleton-wrapper">
            <el-skeleton-item variant="h1" style="width: 65%; height: 32px; margin-bottom: 16px;" />
            <div style="display: flex; gap: 24px; margin-bottom: 32px;">
              <el-skeleton-item variant="text" style="width: 120px;" />
              <el-skeleton-item variant="text" style="width: 90px;" />
              <el-skeleton-item variant="text" style="width: 60px;" />
            </div>
            <div style="display: flex; flex-direction: column; gap: 14px;">
              <el-skeleton-item variant="text" style="width: 100%;" />
              <el-skeleton-item variant="text" style="width: 100%;" />
              <el-skeleton-item variant="text" style="width: 92%;" />
              <el-skeleton-item variant="text" style="width: 100%;" />
              <el-skeleton-item variant="text" style="width: 85%;" />
              <el-skeleton-item variant="text" style="width: 100%;" />
              <el-skeleton-item variant="text" style="width: 78%;" />
              <el-skeleton-item variant="text" style="width: 100%;" />
              <el-skeleton-item variant="text" style="width: 60%;" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>
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
        <span class="meta-item">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
          {{ formatTime(article.uploadTime) }}
        </span>
        <span class="meta-item">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
          {{ article.viewCount || 0 }} 次浏览
        </span>
        <span class="meta-item">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          {{ article.fileType }}
        </span>
      </div>
      <article class="article-content" v-html="renderContent(article.extractedContent)" />
      <div class="article-actions">
        <el-button type="primary" @click="viewOriginal">
          <el-icon style="margin-right: 4px;"><Document /></el-icon>查看原始文件
        </el-button>
      </div>
    </template>
    <div v-else class="error-state">文章不存在或无权查看</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import api, { getAccessToken } from '../api'

const route = useRoute()

// Enable HTML so that tags like <h3> in content are rendered as real headings
const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
  typographer: true,
})

const article = ref<any>(null)
const loading = ref(true)

const parseTags = (t: string) => t ? t.split(',').map(s => s.trim()).filter(Boolean) : []
const formatTime = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : ''

const renderContent = (content: string) => {
  if (!content) return '<p class="prose-empty">暂无内容</p>'

  let text = content

  // 1. Convert [xxx Content] style markers to Markdown headings (## level)
  text = text.replace(/\[([^\]]+)\s*Content\]/gi, '## $1')
  text = text.replace(/\[Audio\s+Transcription\]/gi, '## 音频转录')
  text = text.replace(/\[([^\]]+)\s*File\]/gi, '### 文件类型：$1')

  // 2. Sanitize: only allow safe HTML tags, strip everything else
  //    This prevents XSS while keeping formatting from extraction
  const ALLOWED_TAGS = [
    'br', 'hr', 'img', 'video', 'audio', 'source',
    'table', 'thead', 'tbody', 'tr', 'th', 'td',
    'div', 'span', 'p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'ul', 'ol', 'li', 'strong', 'b', 'em', 'i', 'u', 's',
    'blockquote', 'pre', 'code', 'a',
    'figure', 'figcaption',
    'sub', 'sup',
  ]

  // Remove script/style/iframe/event handlers for security
  text = text.replace(/<script[\s\S]*?<\/script>/gi, '')
  text = text.replace(/<style[\s\S]*?<\/style>/gi, '')
  text = text.replace(/<iframe[\s\S]*?<\/iframe>/gi, '')
  text = text.replace(/\son\w+\s*=\s*["'][^"']*["']/gi, '')
  text = text.replace(/<form[\s\S]*?<\/form>/gi, '')

  // Strip disallowed HTML tags (keep the inner text)
  text = text.replace(/<\/?([a-zA-Z][a-zA-Z0-9]*)[^>]*>/g, (match, tagName) => {
    return ALLOWED_TAGS.includes(tagName.toLowerCase()) ? match : ''
  })

  // 3. Render with markdown-it (html: true preserves the allowed HTML)
  let html = md.render(text)

  // 4. Post-process: wrap bare images/figures in responsive containers
  html = html.replace(/(<img[^>]*>)/g, '<figure class="prose-img-wrap">$1</figure>')

  return html
}

const viewOriginal = async () => {
  const id = route.params.id as string
  const fileName = article.value?.fileName || '文件'

  // Try authenticated endpoint with proxy mode (backend returns file directly, no redirect)
  const token = getAccessToken()
  const headers: Record<string, string> = {}
  if (token) headers['Authorization'] = `Bearer ${token}`

  try {
    const res = await fetch(`/api/knowledge/view/${id}?proxy=true`, { headers })
    if (res.ok) {
      const contentType = res.headers.get('Content-Type') || ''
      if (contentType.includes('text/plain')) {
        const text = await res.text()
        if (text.length < 200) {
          ElMessage.warning('该文档没有原始文件，仅支持在线预览')
        } else {
          const escaped = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
          const html = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>${fileName}</title>
            <style>body{font-family:system-ui,-apple-system,sans-serif;max-width:800px;margin:40px auto;padding:0 20px;line-height:1.8;white-space:pre-wrap;word-break:break-word;color:#1e293b;background:#f8fafc;}</style></head>
            <body>${escaped}</body></html>`
          window.open(URL.createObjectURL(new Blob([html], { type: 'text/html' })), '_blank')
        }
      } else {
        // Binary file (PDF, image, etc.) — download via blob
        const blob = await res.blob()
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = fileName
        a.click()
        URL.revokeObjectURL(url)
      }
      return
    }
    if (res.status === 403) { ElMessage.error('无权查看此文件'); return }
    if (res.status === 404) { ElMessage.error('文件不存在，可能已被删除'); return }
  } catch { /* fall through */ }

  // Fallback: public endpoint (open in new tab, browser handles download)
  window.open(`/api/knowledge/public/view/${id}?proxy=true`, '_blank')
}

onMounted(async () => {
  const id = route.params.id
  try {
    const res = await api.get(`/knowledge/public/detail/${id}`)
    article.value = res.data
  } catch {
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
.article-detail {
  max-width: 800px;
  margin: 0 auto;
  padding-bottom: 60px;
}

/* --- Header --- */
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.2s;
}
.back-btn:hover {
  color: var(--accent);
  background: var(--accent-light);
}
.article-tags { display: flex; gap: 6px; }

.article-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0 0 12px;
  line-height: 1.3;
  letter-spacing: -0.01em;
}

.article-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: var(--text-muted);
}

/* --- Article Content (Prose) --- */
.article-content {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 40px;
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);
  word-break: break-word;
  overflow-wrap: break-word;
}

/* Typography */
.article-content :deep(h1) {
  font-size: 1.75em;
  font-weight: 800;
  color: var(--text-primary);
  margin: 2em 0 0.6em;
  line-height: 1.25;
  letter-spacing: -0.02em;
  padding-bottom: 0.3em;
  border-bottom: 2px solid var(--border-color);
}
.article-content :deep(h2) {
  font-size: 1.4em;
  font-weight: 700;
  color: var(--text-primary);
  margin: 1.8em 0 0.5em;
  line-height: 1.3;
  letter-spacing: -0.01em;
}
.article-content :deep(h3) {
  font-size: 1.2em;
  font-weight: 700;
  color: var(--text-primary);
  margin: 1.5em 0 0.4em;
  line-height: 1.4;
}
.article-content :deep(h4),
.article-content :deep(h5),
.article-content :deep(h6) {
  font-weight: 600;
  color: var(--text-primary);
  margin: 1.2em 0 0.4em;
  line-height: 1.4;
}

/* Paragraphs */
.article-content :deep(p) {
  margin: 0 0 1.2em;
  line-height: 1.8;
}
.article-content :deep(p:last-child) {
  margin-bottom: 0;
}

/* Links */
.article-content :deep(a) {
  color: var(--accent-blue);
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: border-color 0.2s;
}
.article-content :deep(a:hover) {
  border-bottom-color: var(--accent-blue);
}

/* Strong / emphasis */
.article-content :deep(strong),
.article-content :deep(b) {
  font-weight: 700;
  color: var(--text-primary);
}
.article-content :deep(em),
.article-content :deep(i) {
  font-style: italic;
}

/* Inline code */
.article-content :deep(code) {
  background: var(--bg-secondary);
  color: var(--text-primary);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
  font-family: 'Menlo', 'Monaco', 'Consolas', monospace;
  word-break: break-all;
}

/* Code blocks */
.article-content :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 20px;
  border-radius: 10px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 1.5em 0;
  line-height: 1.6;
  font-size: 14px;
}
.article-content :deep(pre code) {
  background: none;
  padding: 0;
  font-size: inherit;
  color: inherit;
  border-radius: 0;
}

/* Blockquotes */
.article-content :deep(blockquote) {
  border-left: 4px solid var(--accent);
  margin: 1.5em 0;
  padding: 12px 20px;
  background: var(--accent-light);
  border-radius: 0 8px 8px 0;
  color: var(--text-secondary);
  font-style: italic;
}
.article-content :deep(blockquote p:last-child) {
  margin-bottom: 0;
}

/* Lists */
.article-content :deep(ul),
.article-content :deep(ol) {
  margin: 0 0 1.2em;
  padding-left: 1.6em;
}
.article-content :deep(li) {
  margin-bottom: 0.4em;
  line-height: 1.7;
}
.article-content :deep(li:last-child) {
  margin-bottom: 0;
}

/* Horizontal rule */
.article-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color);
  margin: 2em 0;
}

/* Tables */
.article-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1.5em 0;
  font-size: 0.95em;
  display: block;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
.article-content :deep(th),
.article-content :deep(td) {
  border: 1px solid var(--border-color);
  padding: 10px 14px;
  text-align: left;
}
.article-content :deep(thead th) {
  background: var(--bg-secondary);
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}
.article-content :deep(tbody tr:hover) {
  background: var(--bg-secondary);
}

/* Images */
.article-content :deep(.prose-img-wrap) {
  margin: 1.5em 0;
  text-align: center;
}
.article-content :deep(.prose-img-wrap img) {
  max-width: 100%;
  height: auto;
  border-radius: 10px;
  box-shadow: var(--shadow-md);
}

/* Standalone images not wrapped in figure */
.article-content :deep(img):not(.prose-img-wrap img) {
  max-width: 100%;
  height: auto;
  border-radius: 10px;
}

/* Video / Audio */
.article-content :deep(video),
.article-content :deep(audio) {
  max-width: 100%;
  border-radius: 10px;
  margin: 1.5em 0;
}
.article-content :deep(video) {
  background: #000;
  aspect-ratio: 16 / 9;
}

/* Figures */
.article-content :deep(figure) {
  margin: 1.5em 0;
}
.article-content :deep(figcaption) {
  font-size: 0.875em;
  color: var(--text-muted);
  text-align: center;
  margin-top: 8px;
}

/* Empty state */
.article-content :deep(.prose-empty) {
  text-align: center;
  color: var(--text-muted);
  padding: 40px 0;
}

/* --- Actions --- */
.article-actions {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

/* --- Loading / Error --- */
.loading-state,
.error-state {
  text-align: center;
  padding: 80px 0;
  color: var(--text-muted);
  font-size: 15px;
}

/* --- Responsive --- */
@media (max-width: 768px) {
  .article-content {
    padding: 24px 20px;
    font-size: 15px;
  }
  .article-title { font-size: 24px; }
  .article-meta { flex-direction: column; gap: 8px; }
  .detail-header { flex-direction: column; align-items: flex-start; gap: 8px; }
}
</style>
