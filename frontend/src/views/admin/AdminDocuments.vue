<template>
  <div class="admin-documents">
    <div class="page-header-row">
      <h2 class="page-title">文档管理</h2>
      <div class="header-actions">
        <el-button type="primary" size="small" @click="showUpload = !showUpload">上传文档</el-button>
        <el-input v-model="keyword" placeholder="搜索文档..." style="width: 220px" clearable size="small"
          @keydown.enter="fetchDocs(0)" @clear="fetchDocs(0)">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
    </div>

    <!-- Upload Area -->
    <div v-if="showUpload" class="upload-panel">
      <el-upload ref="uploadRef" action="" :auto-upload="false"
        :on-change="handleFileChange" multiple drag :show-file-list="true" :file-list="fileList">
        <div class="upload-inner">
          <el-icon :size="28" color="#10b981"><Plus /></el-icon>
          <span>拖拽文件或点击选择（支持批量上传）</span>
        </div>
      </el-upload>
      <div class="upload-footer">
        <el-input v-model="uploadTags" placeholder="标签（逗号分隔）" size="small" style="width: 300px" />
        <el-button type="primary" size="small" @click="doUpload" :loading="uploading">
          确认上传（{{ fileList.length }} 个文件）
        </el-button>
      </div>
    </div>

    <el-table :data="documents" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
      <el-table-column label="标签" min-width="180">
        <template #default="{ row }">
          <span v-if="!row._editing">
            <el-tag v-for="tag in parseTags(row.tags)" :key="tag" size="small" style="margin: 2px">{{ tag }}</el-tag>
            <span v-if="!row.tags" style="color: #94a3b8">无</span>
          </span>
          <el-input v-else v-model="row._editTags" size="small" placeholder="逗号分隔" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="公开" width="80">
        <template #default="{ row }">
          <el-switch v-model="row._shared" size="small" @change="togglePublic(row)" />
        </template>
      </el-table-column>
      <el-table-column label="浏览" width="70" prop="viewCount" />
      <el-table-column label="上传时间" width="170">
        <template #default="{ row }">{{ formatTime(row.uploadTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="!row._editing">
            <el-button link type="primary" size="small" @click="startEdit(row)">编辑</el-button>
            <el-button link size="small" @click="previewDoc(row)">查看</el-button>
            <el-button link size="small" @click="editContent(row)">编辑内容</el-button>
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
    <div class="pagination-wrap" v-if="totalPages > 1">
      <el-pagination background layout="prev, pager, next"
        :total="totalElements" :page-size="pageSize"
        :current-page="currentPage + 1"
        @current-change="(p: number) => fetchDocs(p - 1)" />
    </div>

    <!-- Preview Dialog -->
    <el-dialog v-model="previewVisible" :title="previewDoc_.fileName" width="700px" top="5vh">
      <div class="preview-content" v-html="renderMd(previewDoc_.extractedContent || '暂无内容')"></div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
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
import { ref, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import api from '../../api'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const renderMd = (s: string) => s ? md.render(s) : '<p>暂无内容</p>'

const documents = ref<any[]>([])
const keyword = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = 20

const parseTags = (t: string) => t ? t.split(',').map(s => s.trim()).filter(Boolean) : []
const statusType = (s: string) => {
  if (s === 'COMPLETED') return 'success'
  if (s === 'PROCESSING') return 'warning'
  if (s === 'FAILED') return 'danger'
  return 'info'
}
const formatTime = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : ''

const fetchDocs = async (page: number) => {
  try {
    const params: any = { page, size: pageSize }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const res = await api.get('/admin/documents', { params })
    documents.value = res.data.content.map((d: any) => ({
      ...d,
      _shared: d.isPublic ?? d.shared ?? false,
      _editing: false,
      _editTags: d.tags || ''
    }))
    totalPages.value = res.data.totalPages
    totalElements.value = res.data.totalElements
    currentPage.value = res.data.number
  } catch (e) { console.error(e) }
}

const togglePublic = async (row: any) => {
  try {
    await api.put(`/admin/documents/${row.id}`, { shared: row._shared })
    ElMessage({ message: row._shared ? '已公开' : '已取消公开', type: 'success', duration: 1500 })
  } catch { ElMessage.error('操作失败') }
}

const startEdit = (row: any) => {
  row._editing = true
  row._editTags = row.tags || ''
}

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
const previewDoc = (doc: any) => {
  previewDoc_.value = doc
  previewVisible.value = true
}

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
    await api.put(`/admin/documents/${editContentDocId.value}`, {
      extractedContent: editContentText.value
    })
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

const handleFileChange = (_file: any, newFileList: any[]) => {
  fileList.value = newFileList
}

const doUpload = async () => {
  if (fileList.value.length === 0) return
  uploading.value = true
  let success = 0
  for (const f of fileList.value) {
    const formData = new FormData()
    formData.append('file', f.raw)
    if (uploadTags.value.trim()) formData.append('tags', uploadTags.value.trim())
    try {
      await api.post('/knowledge/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      success++
    } catch { /* ignore */ }
  }
  uploading.value = false
  fileList.value = []
  uploadTags.value = ''
  showUpload.value = false
  ElMessage.success(`成功上传 ${success} 个文件`)
  fetchDocs(0)
}

onMounted(() => fetchDocs(0))
</script>

<style scoped>
.page-title { margin: 0; font-size: 22px; color: #1e293b; }
.page-header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header-actions { display: flex; gap: 10px; align-items: center; }
.upload-panel {
  background: #fff; border-radius: 12px; padding: 20px; margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.upload-inner { display: flex; align-items: center; gap: 12px; justify-content: center; padding: 16px; }
.upload-footer { display: flex; gap: 12px; align-items: center; margin-top: 12px; }
:deep(.el-table) { border-radius: 12px; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
.preview-content {
  max-height: 60vh; overflow-y: auto; font-size: 14px; line-height: 1.8;
}
.preview-content :deep(p) { margin: 0 0 12px; }
.preview-content :deep(pre) { background: #1e293b; color: #e2e8f0; padding: 12px; border-radius: 8px; overflow-x: auto; }
</style>
