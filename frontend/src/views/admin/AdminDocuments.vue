<template>
  <div class="admin-documents">
    <div class="admin-page-header">
      <h2 class="admin-page-title">文档管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showUpload = !showUpload">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:4px"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          上传文档
        </el-button>
        <el-input v-model="keyword" placeholder="搜索文件名..." clearable style="width: 240px" @keydown.enter="resetAndFetch" @clear="resetAndFetch">
          <template #prefix>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          </template>
        </el-input>
      </div>
    </div>

    <div v-if="showUpload" class="admin-upload-panel">
      <el-upload ref="uploadRef" action="" :auto-upload="false" :on-change="handleFileChange" multiple drag :show-file-list="true" :file-list="fileList">
        <div class="upload-inner">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#4361ee" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          <span>拖拽文件或点击选择（支持批量上传）</span>
        </div>
      </el-upload>
      <div class="upload-footer">
        <el-input v-model="uploadTags" placeholder="标签（逗号分隔）" size="default" style="width: 300px" />
        <el-button type="primary" @click="doUpload" :loading="uploading">
          确认上传（{{ fileList.length }} 个文件）
        </el-button>
      </div>
    </div>

    <div class="admin-filter-bar" v-if="allTags.length > 0">
      <el-select v-model="filterTag" placeholder="按标签筛选" clearable style="width: 160px" @change="resetAndFetch">
        <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
      </el-select>
    </div>

    <div class="admin-card" style="padding: 0; overflow: hidden;">
      <el-table :data="documents" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div style="display:flex; align-items:center; gap:6px;">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--admin-text-muted)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              {{ row.fileName }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <span v-if="!row._editing">
              <span v-for="tag in parseTags(row.tags)" :key="tag" style="display:inline-block; padding:2px 8px; margin:2px; background:var(--admin-primary-light, #eef1ff); color:var(--admin-primary, #4361ee); border-radius:10px; font-size:12px; font-weight:500;">{{ tag }}</span>
              <span v-if="!row.tags" style="color: var(--admin-text-muted, #94a3b8); font-size:13px;">无</span>
            </span>
            <el-input v-else v-model="row._editTags" size="small" placeholder="逗号分隔" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="admin-doc-status" :class="statusClass(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="公开" width="75">
          <template #default="{ row }">
            <el-switch v-model="row._shared" size="small" @change="togglePublic(row)" />
          </template>
        </el-table-column>
        <el-table-column label="浏览" width="65" prop="viewCount" />
        <el-table-column label="上传时间" width="170">
          <template #default="{ row }">{{ formatTime(row.uploadTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="!row._editing">
              <el-button link type="primary" size="small" @click="startEdit(row)">编辑</el-button>
              <el-button link size="small" @click="previewDoc(row)">查看</el-button>
              <el-button link size="small" @click="editContent(row)">内容</el-button>
              <el-popconfirm title="确定要删除此文档吗？" @confirm="handleDeleteDoc(row.id)">
                <template #reference>
                  <el-button link type="danger" size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
            <template v-else>
              <el-button link type="success" size="small" @click="saveEdit(row)">保存</el-button>
              <el-button link size="small" @click="row._editing = false">取消</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="totalPages > 1" class="pagination-wrap">
      <el-pagination background layout="prev, pager, next"
        :total="totalElements"
        :page-size="pageSize"
        :current-page="currentPage + 1"
        @current-change="onPageChange"
      />
    </div>

    <!-- Preview Dialog -->
    <el-dialog v-model="previewVisible" :title="previewDoc_.fileName" width="700px" top="5vh">
      <div class="admin-preview-content" v-html="renderMd(previewDoc_.extractedContent || '暂无内容')"></div>
      <template #footer><el-button @click="previewVisible = false">关闭</el-button></template>
    </el-dialog>

    <!-- Edit Content Dialog -->
    <el-dialog v-model="editContentVisible" title="编辑文档内容" width="800px" top="5vh">
      <el-input v-model="editContentText" type="textarea" :rows="20" placeholder="文档内容" />
      <template #footer>
        <el-button @click="editContentVisible = false">取消</el-button>
        <el-button type="primary" @click="saveContent">保存内容</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import api from '../../api'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const renderMd = (s: string) => s ? md.render(s) : '<p>暂无内容</p>'

const documents = ref<any[]>([])
const keyword = ref('')
const filterTag = ref('')
const allTags = ref<string[]>([])
const currentPage = ref(0)
const totalElements = ref(0)

// --- Dynamic page size based on viewport height ---
const calcPageSize = (): number => {
  const h = window.innerHeight
  if (h >= 900) return 15
  if (h >= 700) return 12
  if (h >= 500) return 8
  return 5
}

const pageSize = ref(calcPageSize())
const totalPages = computed(() => Math.ceil(totalElements.value / pageSize.value) || 1)

const updatePageSize = () => {
  const newSize = calcPageSize()
  if (newSize === pageSize.value) return
  pageSize.value = newSize
  const maxPage = Math.max(0, totalPages.value - 1)
  if (currentPage.value > maxPage) {
    fetchDocs(maxPage)
  } else {
    fetchDocs(currentPage.value)
  }
}

let resizeTimer: ReturnType<typeof setTimeout> | null = null
const onResize = () => {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(updatePageSize, 150)
}

const parseTags = (t: string) => t ? t.split(',').map(s => s.trim()).filter(Boolean) : []
const statusClass = (s: string) => {
  if (s === 'COMPLETED') return 'completed'
  if (s === 'PROCESSING') return 'processing'
  if (s === 'FAILED') return 'failed'
  return ''
}
const formatTime = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : ''

const fetchDocs = async (page: number) => {
  try {
    const params: any = { page, size: pageSize.value }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (filterTag.value) params.tag = filterTag.value
    const res = await api.get('/admin/documents', { params })
    documents.value = res.data.content.map((d: any) => ({
      ...d, _shared: d.isPublic ?? d.shared ?? false, _editing: false, _editTags: d.tags || ''
    }))
    totalElements.value = res.data.totalElements
    currentPage.value = res.data.number
    // Collect all tags for filter
    const tagSet = new Set<string>()
    res.data.content.forEach((d: any) => parseTags(d.tags).forEach(t => tagSet.add(t)))
    allTags.value = Array.from(tagSet).sort()
  } catch (e) { console.error(e) }
}

/** Reset to page 0 and re-fetch (used when search/filter/upload changes) */
const resetAndFetch = () => fetchDocs(0)

const onPageChange = (p: number) => fetchDocs(p - 1)

const togglePublic = async (row: any) => {
  try {
    await api.put(`/admin/documents/${row.id}`, { shared: row._shared })
    ElMessage({ message: row._shared ? '已公开' : '已取消公开', type: 'success', duration: 1500 })
  } catch { ElMessage.error('操作失败') }
}

const startEdit = (row: any) => { row._editing = true; row._editTags = row.tags || '' }
const saveEdit = async (row: any) => {
  try {
    await api.put(`/admin/documents/${row.id}`, { tags: row._editTags })
    row.tags = row._editTags
    row._editing = false
    ElMessage.success('已保存')
  } catch { ElMessage.error('保存失败') }
}

const handleDeleteDoc = async (id: number) => {
  try {
    await api.delete(`/admin/documents/${id}`)
    ElMessage.success('已删除')
    fetchDocs(currentPage.value)
  } catch { ElMessage.error('删除失败') }
}

// Preview
const previewVisible = ref(false)
const previewDoc_ = ref<any>({})
const previewDoc = (doc: any) => { previewDoc_.value = doc; previewVisible.value = true }

// Edit content
const editContentVisible = ref(false)
const editContentText = ref('')
const editContentDocId = ref<number | null>(null)

const editContent = (doc: any) => {
  editContentDocId.value = doc.id
  editContentText.value = doc.extractedContent || ''
  editContentVisible.value = true
}

const saveContent = async () => {
  if (editContentDocId.value === null) return
  try {
    await api.put(`/admin/documents/${editContentDocId.value}`, { extractedContent: editContentText.value })
    editContentVisible.value = false
    ElMessage.success('内容已更新')
    fetchDocs(currentPage.value)
  } catch { ElMessage.error('保存失败') }
}

// Upload
const showUpload = ref(false)
const fileList = ref<any[]>([])
const uploadTags = ref('')
const uploading = ref(false)
const uploadRef = ref()

const handleFileChange = (_file: any, newFileList: any[]) => { fileList.value = newFileList }

const doUpload = async () => {
  if (fileList.value.length === 0) return
  uploading.value = true
  let success = 0
  for (const f of fileList.value) {
    const formData = new FormData()
    formData.append('file', f.raw)
    if (uploadTags.value.trim()) formData.append('tags', uploadTags.value.trim())
    try { await api.post('/knowledge/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } }); success++ } catch { /* ignore */ }
  }
  uploading.value = false
  fileList.value = []; uploadTags.value = ''; showUpload.value = false
  ElMessage.success(`成功上传 ${success} 个文件`)
  resetAndFetch()
}

onMounted(() => {
  fetchDocs(0)
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (resizeTimer) clearTimeout(resizeTimer)
})
</script>

<style scoped>
.upload-inner { display: flex; align-items: center; gap: 12px; justify-content: center; padding: 16px; color: var(--admin-text-secondary, #64748b); }
.upload-footer { display: flex; gap: 12px; align-items: center; margin-top: 12px; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; padding-top: 16px; }
</style>
