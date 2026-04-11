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
        <el-select v-model="uploadTags" multiple filterable allow-create default-first-option placeholder="选择或输入标签" size="default" style="width: 300px">
          <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
        </el-select>
        <el-button type="primary" @click="doUpload" :loading="uploading">
          确认上传（{{ fileList.length }} 个文件）
        </el-button>
      </div>
    </div>

    <div class="admin-filter-bar">
      <el-select v-model="filterTag" placeholder="按标签筛选" clearable style="width: 140px">
        <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="按状态筛选" clearable style="width: 140px">
        <el-option label="处理中" value="PROCESSING" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="失败" value="FAILED" />
      </el-select>
      <div class="filter-divider" />
      <el-select v-model="sortBy" style="width: 130px">
        <el-option label="按上传时间" value="uploadTime" />
        <el-option label="按浏览量" value="viewCount" />
      </el-select>
      <el-select v-model="sortOrder" style="width: 110px">
        <el-option label="降序" value="desc" />
        <el-option label="升序" value="asc" />
      </el-select>
      <el-button type="primary" size="default" @click="resetAndFetch">查询</el-button>
    </div>

    <div class="batch-actions" v-if="selectedRows.length > 0">
      <span class="batch-info">已选择 {{ selectedRows.length }} 项</span>
      <el-popconfirm title="确定要删除选中的文档吗？" @confirm="handleBatchDelete">
        <template #reference>
          <el-button type="danger" size="small">批量删除</el-button>
        </template>
      </el-popconfirm>
    </div>

    <div class="admin-card" style="padding: 0; overflow: hidden;">
      <el-table :data="documents" stripe style="width: 100%" @selection-change="handleSelectionChange" ref="tableRef">
        <el-table-column type="selection" width="45" fixed="left" />
        <el-table-column label="ID" width="60">
          <template #default="{ $index }">{{ currentPage * pageSize + $index + 1 }}</template>
        </el-table-column>
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
            <span v-for="tag in parseTags(row.tags)" :key="tag" style="display:inline-block; padding:2px 8px; margin:2px; background:var(--admin-primary-light, #eef1ff); color:var(--admin-primary, #4361ee); border-radius:10px; font-size:12px; font-weight:500;">{{ tag }}</span>
            <span v-if="!row.tags" style="color: var(--admin-text-muted, #94a3b8); font-size:13px;">无</span>
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
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button link size="small" @click="previewDoc(row)">查看</el-button>
            <el-popconfirm title="确定要删除此文档吗？" @confirm="handleDeleteDoc(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
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

    <!-- Edit Dialog -->
    <el-dialog v-model="editDialogVisible" title="编辑文档" width="700px" top="5vh">
      <el-form label-width="60px">
        <el-form-item label="文件名">
          <span style="color: var(--admin-text-primary, #1e293b); font-weight: 500;">{{ editingDoc.fileName }}</span>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="editingTags" multiple filterable allow-create default-first-option placeholder="选择或输入标签" style="width: 100%">
            <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="editingContent" type="textarea" :rows="12" placeholder="文档内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
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
const filterStatus = ref('')
const sortBy = ref('uploadTime')
const sortOrder = ref('desc')
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
    const params: any = { page, size: pageSize.value, sortBy: sortBy.value, sortOrder: sortOrder.value }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (filterTag.value) params.tag = filterTag.value
    if (filterStatus.value) params.status = filterStatus.value
    const res = await api.get('/admin/documents', { params })
    documents.value = res.data.content.map((d: any) => ({
      ...d, _shared: d.isPublic ?? d.shared ?? false
    }))
    totalElements.value = res.data.totalElements
    currentPage.value = res.data.number
  } catch (e) { console.error(e) }
}

const fetchAllTags = async () => {
  try {
    const res = await api.get('/admin/tags')
    allTags.value = res.data.map((t: any) => t.name as string).sort()
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

const openEditDialog = (row: any) => {
  editingDoc.value = row
  editingTags.value = parseTags(row.tags)
  editingContent.value = row.extractedContent || ''
  editDialogVisible.value = true
}

const saveEdit = async () => {
  try {
    await api.put(`/admin/documents/${editingDoc.value.id}`, {
      tags: editingTags.value.join(','),
      extractedContent: editingContent.value
    })
    editDialogVisible.value = false
    ElMessage.success('已保存')
    fetchDocs(currentPage.value)
    fetchAllTags()
  } catch { ElMessage.error('保存失败') }
}

const handleDeleteDoc = async (id: number) => {
  try {
    await api.delete(`/admin/documents/${id}`)
    ElMessage.success('已删除')
    fetchDocs(currentPage.value)
  } catch { ElMessage.error('删除失败') }
}

// Batch selection & delete
const tableRef = ref()
const selectedRows = ref<any[]>([])

const handleSelectionChange = (rows: any[]) => {
  selectedRows.value = rows
}

const handleBatchDelete = async () => {
  const ids = selectedRows.value.map(r => r.id)
  if (ids.length === 0) return
  try {
    await Promise.all(ids.map(id => api.delete(`/admin/documents/${id}`)))
    ElMessage.success(`已删除 ${ids.length} 个文档`)
    fetchDocs(currentPage.value)
  } catch { ElMessage.error('批量删除失败') }
}

// Preview
const previewVisible = ref(false)
const previewDoc_ = ref<any>({})
const previewDoc = (doc: any) => { previewDoc_.value = doc; previewVisible.value = true }

// Edit dialog
const editDialogVisible = ref(false)
const editingDoc = ref<any>({})
const editingTags = ref<string[]>([])
const editingContent = ref('')

// Upload
const showUpload = ref(false)
const fileList = ref<any[]>([])
const uploadTags = ref<string[]>([])
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
    if (uploadTags.value.length > 0) formData.append('tags', uploadTags.value.join(','))
    try { await api.post('/knowledge/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } }); success++ } catch { /* ignore */ }
  }
  uploading.value = false
  fileList.value = []; uploadTags.value = []; showUpload.value = false
  ElMessage.success(`成功上传 ${success} 个文件`)
  resetAndFetch()
  fetchAllTags()
}

onMounted(() => {
  fetchDocs(0)
  fetchAllTags()
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
.filter-divider { width: 1px; height: 28px; background: var(--admin-border, #e2e8f0); margin: 0 4px; flex-shrink: 0; }
.batch-actions { display: flex; align-items: center; gap: 12px; padding: 8px 12px; background: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; margin-bottom: 8px; }
.batch-info { font-size: 13px; color: #dc2626; font-weight: 500; }
</style>
