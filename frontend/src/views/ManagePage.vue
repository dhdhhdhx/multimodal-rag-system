<template>
  <div class="manage-page">
    <PageHeader badge="文件管理" title="知识库管理" subtitle="上传、管理和发布你的知识文件" />

    <div class="manage-layout">
      <!-- ===== Left Sidebar (sticky) ===== -->
      <aside class="sidebar">
        <!-- Upload Card -->
        <div class="upload-card">
          <h3 class="card-section-title">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
            上传文件
          </h3>
          <el-upload class="upload-area" ref="uploadRef" action="" :auto-upload="false"
            :on-change="handleFileChange" name="file" multiple drag :file-list="fileList"
            :show-file-list="false">
            <div class="upload-inner">
              <svg class="upload-icon" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>
              <div class="upload-text">拖拽文件到此处，或<em>点击选择</em></div>
              <div class="upload-hint">支持文本、图像、音频、视频</div>
            </div>
          </el-upload>

          <!-- File Preview Thumbnails -->
          <div v-if="filePreviews.length" class="preview-grid">
            <div v-for="(preview, idx) in filePreviews" :key="idx" class="preview-thumb"
              :class="{ 'preview-thumb--removable': true }">
              <!-- Image preview -->
              <img v-if="preview.type === 'image'" :src="preview.url" :alt="preview.name" />
              <!-- Video preview (first frame) -->
              <video v-else-if="preview.type === 'video'" :src="preview.url" preload="metadata"
                @loadeddata="captureVideoFrame($event, idx)" muted />
              <img v-if="preview.type === 'video' && preview.poster" :src="preview.poster" :alt="preview.name" />
              <div v-else-if="preview.type === 'video'" class="preview-placeholder preview-placeholder--video">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="4" width="20" height="16" rx="2"/><polygon points="10 9 15 12 10 15" fill="currentColor" stroke="none"/></svg>
              </div>
              <!-- Audio preview -->
              <div v-else-if="preview.type === 'audio'" class="preview-placeholder preview-placeholder--audio">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>
              </div>
              <!-- Document / other -->
              <div v-else class="preview-placeholder preview-placeholder--doc">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              </div>
              <span class="preview-name" :title="preview.name">{{ preview.name }}</span>
              <button class="preview-remove" @click="removeFile(idx)" aria-label="移除文件">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
          </div>

          <div class="tags-input">
            <label for="upload-tags-input">标签（逗号分隔）</label>
            <el-input id="upload-tags-input" v-model="uploadTags" placeholder="例如：技术笔记,AI,RAG" size="small" />
          </div>
          <el-button type="primary" class="upload-btn" @click="doUpload" :loading="uploading"
            :disabled="fileList.length === 0">
            确认上传（{{ fileList.length }} 个文件）
          </el-button>
        </div>

        <!-- Quick Stats -->
        <div class="stats-card">
          <div class="stat-row">
            <span class="stat-label">全部文档</span>
            <span class="stat-value">{{ totalCount }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">已公开</span>
            <span class="stat-value stat-value--green">{{ publicCount }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">处理中</span>
            <span class="stat-value stat-value--yellow">{{ processingCount }}</span>
          </div>
        </div>
      </aside>

      <!-- ===== Right Main Content (independent scroll) ===== -->
      <main class="main-content">
        <div class="doc-list-card">
          <!-- Header + Filter -->
          <div class="doc-list-header">
            <h3 class="doc-list-title">
              我的文档
              <span class="doc-count">{{ filteredTotal }} 个</span>
            </h3>
          </div>

          <!-- Tag Filter Bar -->
          <div v-if="allTags.length" class="tag-filter-bar">
            <button class="filter-chip" :class="{ 'filter-chip--active': activeTag === '' }"
              @click="activeTag = ''">全部</button>
            <button v-for="tag in allTags" :key="tag" class="filter-chip"
              :class="{ 'filter-chip--active': activeTag === tag }" @click="activeTag = tag">
              {{ tag }}
              <span class="filter-chip-count">{{ tagCounts[tag] || 0 }}</span>
            </button>
          </div>

          <!-- Document List -->
          <div v-if="pagedDocuments.length === 0" class="empty-hint">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--border-color)" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            <p>{{ activeTag ? `没有包含"${activeTag}"标签的文档` : '暂无文件，请上传知识文件' }}</p>
          </div>
          <template v-else>
            <div v-for="doc in pagedDocuments" :key="doc.id" class="doc-row">
              <div class="doc-info">
                <span class="doc-file-type-badge">{{ getFileTypeLabel(doc.fileType) }}</span>
                <span class="doc-name" @click="previewDoc(doc)">{{ doc.fileName }}</span>
                <el-tag size="small" :type="getStatusType(doc.status)">{{ doc.status }}</el-tag>
                <el-tag v-if="isDocPublic(doc)" size="small" type="success">公开</el-tag>
                <span v-if="doc.tags" class="doc-tags-inline">
                  <el-tag v-for="tag in parseTags(doc.tags)" :key="tag" size="small" type="info"
                    class="doc-tag-item">{{ tag }}</el-tag>
                </span>
              </div>
              <div class="doc-actions">
                <el-button link type="primary" size="small" @click="previewDoc(doc)">查看</el-button>
                <el-button link size="small" @click="editDocTags(doc)">编辑标签</el-button>
                <el-button link size="small" @click="togglePublic(doc)">
                  {{ isDocPublic(doc) ? '取消公开' : '设为公开' }}
                </el-button>
                <el-popconfirm title="确定要删除此文档吗？" @confirm="deleteDoc(doc.id)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </template>

          <div v-if="totalPages > 1" class="pagination-wrap">
            <el-pagination background layout="prev, pager, next"
              :total="filteredTotal" :page-size="pageSize"
              :current-page="currentPage + 1"
              @current-change="(p: number) => { currentPage = p - 1 }" />
          </div>
        </div>
      </main>
    </div>

    <!-- Edit Tags Dialog -->
    <el-dialog v-model="tagDialogVisible" title="编辑标签" width="400px">
      <el-input v-model="editingTags" placeholder="逗号分隔，例如：技术笔记,AI" />
      <template #footer>
        <el-button @click="tagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDocTags">保存</el-button>
      </template>
    </el-dialog>

    <!-- Preview Dialog -->
    <el-dialog v-model="previewVisible" :title="previewDoc_.fileName" width="700px" top="5vh">
      <div class="preview-content" v-html="renderMd(previewDoc_.extractedContent || '暂无内容')"></div>
      <template #footer>
        <el-button @click="viewOriginal(previewDoc_)">查看原始文件</el-button>
        <el-button type="primary" @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import api, { getAccessToken } from '../api'
import PageHeader from '../components/PageHeader.vue'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const renderMd = (s: string) => s ? md.render(s) : '<p>暂无内容</p>'

// --- State ---
const allDocuments = ref<any[]>([])
const uploadTags = ref('')
const fileList = ref<any[]>([])
const filePreviews = ref<any[]>([])
const uploading = ref(false)
const uploadRef = ref()
const activeTag = ref('')
const currentPage = ref(0)

// --- Responsive page size based on viewport height ---
const DOC_ROW_HEIGHT = 48   // approximate height of one doc-row (padding + content + border)
const HEADER_HEIGHT = 160    // doc-list-card header + filter bar + card padding
const PAGINATION_HEIGHT = 60 // pagination bar

const calcPageSize = (): number => {
  const h = window.innerHeight
  if (h >= 900) return 12
  if (h >= 700) return 9
  if (h >= 500) return 6
  return 4
}

const pageSize = ref(calcPageSize())

const updatePageSize = () => {
  const newSize = calcPageSize()
  if (newSize === pageSize.value) return
  pageSize.value = newSize
  // Clamp current page if it exceeds new total pages
  const maxPage = Math.max(0, Math.ceil(filteredList.value.length / newSize) - 1)
  if (currentPage.value > maxPage) {
    currentPage.value = maxPage
  }
}

let resizeTimer: ReturnType<typeof setTimeout> | null = null
const onResize = () => {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(updatePageSize, 150)
}

// --- Computed ---
const totalCount = computed(() => allDocuments.value.length)
const publicCount = computed(() => allDocuments.value.filter(d => isDocPublic(d)).length)
const processingCount = computed(() => allDocuments.value.filter(d => d.status === 'PROCESSING').length)

const allTags = computed(() => {
  const tagSet = new Set<string>()
  for (const doc of allDocuments.value) {
    if (doc.tags) {
      for (const t of doc.tags.split(',')) {
        const trimmed = t.trim()
        if (trimmed) tagSet.add(trimmed)
      }
    }
  }
  return Array.from(tagSet).sort()
})

const tagCounts = computed(() => {
  const counts: Record<string, number> = {}
  for (const doc of allDocuments.value) {
    if (doc.tags) {
      for (const t of doc.tags.split(',')) {
        const trimmed = t.trim()
        if (trimmed) counts[trimmed] = (counts[trimmed] || 0) + 1
      }
    }
  }
  return counts
})

// Filtered list WITHOUT pagination (for counts)
const filteredList = computed(() => {
  if (!activeTag.value) return allDocuments.value
  return allDocuments.value.filter(d => {
    if (!d.tags) return false
    return d.tags.split(',').map(s => s.trim()).includes(activeTag.value)
  })
})

const filteredTotal = computed(() => filteredList.value.length)

// Total pages based on current filter + page size
const totalPages = computed(() => Math.ceil(filteredTotal.value / pageSize.value) || 1)

// Actual page slice for display
const pagedDocuments = computed(() => {
  const start = currentPage.value * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

// --- Helpers ---
const parseTags = (t: string) => t ? t.split(',').map(s => s.trim()).filter(Boolean) : []
const isDocPublic = (doc: any) => doc.isPublic ?? doc.shared ?? false
const getStatusType = (s: string) => {
  if (s === 'COMPLETED') return 'success'
  if (s === 'PROCESSING') return 'warning'
  if (s === 'FAILED') return 'danger'
  return 'info'
}

const getFileCategory = (type: string) => {
  const t = (type || '').toLowerCase()
  if (['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp', 'svg'].includes(t)) return 'image'
  if (['mp4', 'avi', 'mov', 'mkv', 'webm', 'flv'].includes(t)) return 'video'
  if (['mp3', 'wav', 'ogg', 'flac', 'aac', 'm4a'].includes(t)) return 'audio'
  return 'document'
}

const getFileTypeLabel = (type: string) => {
  const t = (type || '').toUpperCase()
  if (!t) return 'FILE'
  return t.length > 5 ? t.slice(0, 4) : t
}

// --- File Upload & Preview ---
const handleFileChange = (_file: any, newFileList: any[]) => {
  fileList.value = newFileList
  buildPreviews(newFileList)
}

const buildPreviews = (files: any[]) => {
  // Revoke old URLs to prevent memory leak
  for (const p of filePreviews.value) {
    if (p.url) URL.revokeObjectURL(p.url)
    if (p.poster) URL.revokeObjectURL(p.poster)
  }
  filePreviews.value = files.map(f => {
    const category = getFileCategory(f.raw?.type || f.name || '')
    return {
      name: f.name,
      type: category,
      url: (category === 'image' || category === 'video' || category === 'audio') && f.raw
        ? URL.createObjectURL(f.raw) : null,
      poster: null,
    }
  })
}

const captureVideoFrame = (event: Event, idx: number) => {
  const video = event.target as HTMLVideoElement
  if (video.videoWidth > 0) {
    try {
      const canvas = document.createElement('canvas')
      canvas.width = Math.min(video.videoWidth, 320)
      canvas.height = Math.min(video.videoHeight, 180)
      const ctx = canvas.getContext('2d')
      if (ctx) {
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
        const posterUrl = canvas.toDataURL('image/jpeg', 0.7)
        filePreviews.value[idx].poster = posterUrl
      }
    } catch { /* cross-origin or other error */ }
  }
}

const removeFile = (idx: number) => {
  fileList.value.splice(idx, 1)
  buildPreviews(fileList.value)
}

const doUpload = async () => {
  if (fileList.value.length === 0) return
  uploading.value = true
  let success = 0
  for (const f of fileList.value) {
    const formData = new FormData()
    formData.append('file', f.raw)
    if (uploadTags.value.trim()) {
      formData.append('tags', uploadTags.value.trim())
    }
    try {
      await api.post('/knowledge/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      success++
    } catch (e) { console.error('Upload failed:', f.name, e) }
  }
  uploading.value = false
  fileList.value = []
  filePreviews.value = []
  uploadTags.value = ''
  ElMessage.success(`成功上传 ${success} 个文件`)
  activeTag.value = ''
  currentPage.value = 0
  await fetchDocuments()
}

// --- Document CRUD ---
const fetchDocuments = async () => {
  try {
    const res = await api.get('/knowledge/documents')
    allDocuments.value = res.data || []
  } catch (e) { console.error(e) }
}

const togglePublic = async (doc: any) => {
  try {
    await api.post(`/knowledge/${doc.id}/toggle-public`)
    ElMessage({ message: isDocPublic(doc) ? '已取消公开' : '已设为公开', type: 'success', duration: 1500 })
    await fetchDocuments()
  } catch { ElMessage.error('操作失败') }
}

const deleteDoc = async (id: number) => {
  try {
    await api.delete(`/knowledge/${id}`)
    ElMessage.success('已删除')
    await fetchDocuments()
  } catch { ElMessage.error('删除失败') }
}

// --- Preview Dialog ---
const previewVisible = ref(false)
const previewDoc_ = ref<any>({})

const previewDoc = (doc: any) => {
  previewDoc_.value = doc
  previewVisible.value = true
}

/**
 * View original file — simplest, most reliable approach.
 *
 * 1. Ask backend for a fresh presigned URL (redirect: 'manual' to capture 302).
 * 2. In the same click context, open the URL via <a> element.
 * 3. If the file has no real file (text/plain fallback), show in a new tab.
 * 4. If window.open is blocked, offer a copy-link button.
 */
const viewOriginal = async (doc: any) => {
  const id = doc.id
  const fileName = doc.fileName || '文件'
  const token = getAccessToken()
  if (!token) {
    ElMessage.error('请先登录')
    return
  }

  try {
    // Try redirect mode first (OSS presigned URL)
    // Use 'manual' redirect to capture 302 responses and Location headers
    let res
    try {
      res = await fetch(`/api/knowledge/view/${id}`, {
        headers: { 'Authorization': `Bearer ${token}` },
        redirect: 'manual'
      })
    } catch (fetchErr) {
      // Network error or CORS issue with manual redirect
      console.warn('Redirect mode failed, falling back to proxy mode:', fetchErr)
      // Fall back to proxy mode which avoids CORS issues
      res = await fetch(`/api/knowledge/view/${id}?proxy=true`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
    }

    // Handle 302/301/307 redirect responses from manual redirect mode
    if (res.status === 302 || res.status === 301 || res.status === 307) {
      const location = res.headers.get('Location')
      if (location) {
        openFileLink(location, fileName)
        return
      }
      // If no Location header, fall through to proxy mode
      console.warn('Redirect response without Location header, falling back to proxy mode')
      res = await fetch(`/api/knowledge/view/${id}?proxy=true`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
    }

    // Handle opaque responses (status 0) from CORS errors
    if (res.status === 0) {
      console.warn('Opaque response detected, falling back to proxy mode')
      res = await fetch(`/api/knowledge/view/${id}?proxy=true`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
    }

    // 200 OK — check content type
    if (res.ok) {
      const contentType = res.headers.get('Content-Type') || ''

      // text/plain fallback: backend returned extracted content instead of a real file
      if (contentType.includes('text/plain')) {
        const text = await res.text()
        if (text.length < 200) {
          ElMessage.warning('该文档没有原始文件，仅支持在线预览')
          return
        }
        const escaped = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        const html = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>${fileName}</title>
<style>body{font-family:system-ui,-apple-system,sans-serif;max-width:800px;margin:40px auto;padding:0 20px;line-height:1.8;white-space:pre-wrap;word-break:break-word;color:#1e293b;background:#f8fafc;}</style></head>
<body>${escaped}</body></html>`
        const blobUrl = URL.createObjectURL(new Blob([html], { type: 'text/html' }))
        openFileLink(blobUrl, fileName)
        return
      }

      // Real binary file (local file or proxied OSS file) — use blob URL
      const blob = await res.blob()
      const blobUrl = URL.createObjectURL(blob)
      openFileLink(blobUrl, fileName)
      setTimeout(() => URL.revokeObjectURL(blobUrl), 120000)
      return
    }

    // Non-ok status
    if (res.status === 403) {
      ElMessage.error('无权查看此文件')
    } else if (res.status === 404) {
      ElMessage.error('文件不存在，可能已被删除')
    } else {
      ElMessage.error(`无法查看文件 (${res.status})`)
    }
  } catch (err) {
    console.error('viewOriginal error:', err)
    ElMessage.error('请求失败，请检查网络后重试')
  }
}

/**
 * Open a file URL using an invisible <a> element.
 * This must be called from a user-initiated synchronous context (click handler)
 * to avoid popup blockers.
 */
const openFileLink = (url: string, fileName: string) => {
  const a = document.createElement('a')
  a.href = url
  a.target = '_blank'
  a.rel = 'noopener noreferrer'
  a.style.display = 'none'

  // For blob URLs, use download attribute to force download
  // For external URLs (OSS presigned URLs), let browser decide
  if (url.startsWith('blob:')) {
    a.download = fileName
  }

  document.body.appendChild(a)
  a.click()
  setTimeout(() => document.body.removeChild(a), 100)
}

// --- Edit Tags Dialog ---
const tagDialogVisible = ref(false)
const editingTags = ref('')
const editingDocId = ref<number | null>(null)

const editDocTags = (doc: any) => {
  editingDocId.value = doc.id
  editingTags.value = doc.tags || ''
  tagDialogVisible.value = true
}

const saveDocTags = async () => {
  if (editingDocId.value === null) return
  try {
    await api.put(`/knowledge/${editingDocId.value}/tags`, { tags: editingTags.value })
    ElMessage.success('标签已更新')
    tagDialogVisible.value = false
    await fetchDocuments()
  } catch {
    ElMessage.error('标签更新失败')
  }
}

// --- Watch filter reset pagination ---
watch(activeTag, () => { currentPage.value = 0 })

onMounted(() => {
  fetchDocuments()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (resizeTimer) clearTimeout(resizeTimer)
})
</script>

<style scoped>
/* ===== Layout ===== */
.manage-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* Left sidebar: sticky, fixed width */
.sidebar {
  width: 260px;
  min-width: 260px;
  position: sticky;
  top: 80px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

/* Right main: flex-1 with independent scroll */
.main-content {
  flex: 1;
  min-width: 0;
}

/* ===== Upload Card ===== */
.upload-card,
.stats-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.card-section-title {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.upload-area :deep(.el-upload-dragger) {
  border: 1.5px dashed var(--border-color);
  border-radius: var(--radius);
  padding: 20px 12px;
  transition: border-color 0.2s, background 0.2s;
  background: var(--bg-secondary);
}
.upload-area :deep(.el-upload-dragger:hover) {
  border-color: var(--accent);
  background: var(--accent-light);
}
.upload-inner { text-align: center; }
.upload-icon { margin-bottom: 8px; }
.upload-text { font-size: 13px; color: var(--text-secondary); margin-top: 6px; }
.upload-text em { color: var(--accent); font-style: normal; font-weight: 500; }
.upload-hint { font-size: 11px; color: var(--text-muted); margin-top: 4px; }

/* ===== File Preview Thumbnails ===== */
.preview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 12px;
}
.preview-thumb {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  aspect-ratio: 1;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  cursor: default;
}
.preview-thumb img,
.preview-thumb video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.preview-thumb video {
  position: absolute;
  top: 0; left: 0;
  opacity: 0;
  pointer-events: none;
}
.preview-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}
.preview-placeholder--video { background: #fef3c7; color: #d97706; }
.preview-placeholder--audio { background: #ede9fe; color: #7c3aed; }
.preview-placeholder--doc { background: #f1f5f9; color: #64748b; }

.preview-name {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 3px 4px;
  font-size: 10px;
  color: #fff;
  background: rgba(0, 0, 0, 0.6);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}
.preview-remove {
  position: absolute;
  top: 3px;
  right: 3px;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s;
}
.preview-thumb--removable:hover .preview-remove { opacity: 1; }

/* ===== Tags Input ===== */
.tags-input { margin-top: 14px; }
.tags-input label {
  font-size: 12px;
  color: var(--text-secondary);
  display: block;
  margin-bottom: 4px;
  font-weight: 500;
}
.upload-btn {
  width: 100%;
  margin-top: 14px;
  border-radius: var(--radius);
  background: var(--accent) !important;
  border-color: var(--accent) !important;
  font-weight: 500;
}
.upload-btn:hover {
  background: var(--accent-hover) !important;
  border-color: var(--accent-hover) !important;
}

/* ===== Stats Card ===== */
.stats-card { padding: 16px 20px; }
.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
}
.stat-row + .stat-row { border-top: 1px solid var(--border-color); }
.stat-label { font-size: 13px; color: var(--text-secondary); }
.stat-value { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.stat-value--green { color: var(--accent); }
.stat-value--yellow { color: #d97706; }

/* ===== Doc List Card ===== */
.doc-list-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
}
.doc-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.doc-list-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}
.doc-count { font-size: 13px; color: var(--text-muted); font-weight: 400; margin-left: 6px; }

/* ===== Tag Filter Bar ===== */
.tag-filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 12px 0;
  margin-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}
.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border-radius: 20px;
  border: 1px solid var(--border-color);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  line-height: 1.4;
}
.filter-chip:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-light);
}
.filter-chip--active {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}
.filter-chip--active:hover {
  background: var(--accent-hover);
  border-color: var(--accent-hover);
  color: #fff;
}
.filter-chip-count {
  font-size: 10px;
  opacity: 0.7;
}

/* ===== Empty State ===== */
.empty-hint {
  text-align: center;
  color: var(--text-muted);
  padding: 48px 0;
  font-size: 13px;
}
.empty-hint svg { margin-bottom: 12px; }
.empty-hint p { margin: 0; }

/* ===== Document Rows ===== */
.doc-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
  gap: 8px;
  transition: background 0.15s;
}
.doc-row:last-child { border-bottom: none; }
.doc-row:hover { background: var(--bg-secondary); margin: 0 -12px; padding-left: 12px; padding-right: 12px; border-radius: 6px; }

.doc-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
}

.doc-file-type-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: 22px;
  padding: 0 6px;
  border-radius: 4px;
  background: var(--bg-secondary);
  color: var(--text-muted);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.02em;
  flex-shrink: 0;
}

.doc-name {
  font-size: 13px;
  color: var(--accent-blue);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
  transition: color 0.15s;
}
.doc-name:hover { color: var(--accent); text-decoration: underline; }

.doc-tags-inline {
  display: inline-flex;
  gap: 4px;
}
.doc-tag-item { cursor: default; }

.doc-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
  flex-wrap: wrap;
  justify-content: flex-end;
}

/* ===== Pagination ===== */
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--border-color); }

/* ===== Preview Dialog Content ===== */
.preview-content {
  max-height: 60vh;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-primary);
}
.preview-content :deep(p) { margin: 0 0 12px; }
.preview-content :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
}

/* ===== Responsive ===== */
@media (max-width: 900px) {
  .manage-layout {
    flex-direction: column;
  }
  .sidebar {
    width: 100%;
    min-width: 0;
    position: static;
    max-height: none;
  }
  .doc-name { max-width: 160px; }
}

@media (max-width: 600px) {
  .doc-row {
    flex-direction: column;
    align-items: flex-start;
  }
  .doc-actions {
    width: 100%;
    justify-content: flex-start;
    padding-left: 48px;
  }
  .preview-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
