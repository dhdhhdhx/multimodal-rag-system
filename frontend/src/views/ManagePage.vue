<template>
  <div class="manage-page">
    <PageHeader badge="文件管理" title="知识库管理" subtitle="上传、管理和发布你的知识文件" />
    <div class="manage-layout">
      <div class="upload-card">
        <el-upload class="upload-area" ref="uploadRef" action="" :auto-upload="false"
          :on-change="handleFileChange" name="file" multiple drag :file-list="fileList"
          :show-file-list="true">
          <div class="upload-inner">
            <el-icon :size="36" color="#10b981"><Plus /></el-icon>
            <div class="upload-text">拖拽文件到此处，或<em>点击选择</em></div>
            <div class="upload-hint">支持 文本、图像、音频、视频</div>
          </div>
        </el-upload>
        <div class="tags-input">
          <label>标签（逗号分隔）</label>
          <el-input v-model="uploadTags" placeholder="例如：技术笔记,AI,RAG" size="small" />
        </div>
        <el-button type="primary" class="upload-btn" @click="doUpload" :loading="uploading"
          :disabled="fileList.length === 0">
          确认上传（{{ fileList.length }} 个文件）
        </el-button>
      </div>
      <div class="doc-list-card">
        <h3>我的文档 <span class="doc-count">{{ totalCount }} 个</span></h3>
        <div v-if="documents.length === 0" class="empty-hint">暂无文件，请上传知识文件</div>
        <div v-for="doc in documents" :key="doc.id" class="doc-row">
          <div class="doc-info">
            <span class="doc-name" @click="previewDoc(doc)">{{ doc.fileName }}</span>
            <el-tag size="small" :type="getStatusType(doc.status)">{{ doc.status }}</el-tag>
            <el-tag v-if="isDocPublic(doc)" size="small" type="success">公开</el-tag>
            <span v-if="doc.tags" class="doc-tags">
              <el-tag v-for="tag in parseTags(doc.tags)" :key="tag" size="small" type="info"
                style="margin-left: 4px">{{ tag }}</el-tag>
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
        <div v-if="totalPages > 1" class="pagination-wrap">
          <el-pagination background layout="prev, pager, next"
            :total="totalCount" :page-size="pageSize"
            :current-page="currentPage + 1"
            @current-change="(p: number) => fetchDocuments(p - 1)" />
        </div>
      </div>
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
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import api from '../api'
import PageHeader from '../components/PageHeader.vue'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const renderMd = (s: string) => s ? md.render(s) : '<p>暂无内容</p>'

const documents = ref<any[]>([])
const uploadTags = ref('')
const fileList = ref<any[]>([])
const uploading = ref(false)
const uploadRef = ref()
const pageSize = 15
const currentPage = ref(0)
const totalPages = ref(0)
const totalCount = ref(0)

const parseTags = (t: string) => t ? t.split(',').map(s => s.trim()).filter(Boolean) : []
const isDocPublic = (doc: any) => doc.isPublic ?? doc.shared ?? false
const getStatusType = (s: string) => {
  if (s === 'COMPLETED') return 'success'
  if (s === 'PROCESSING') return 'warning'
  if (s === 'FAILED') return 'danger'
  return 'info'
}

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
  uploadTags.value = ''
  ElMessage.success(`成功上传 ${success} 个文件`)
  fetchDocuments(0)
}

const fetchDocuments = async (page: number) => {
  try {
    const res = await api.get('/knowledge/documents')
    const allDocs = res.data || []
    totalCount.value = allDocs.length
    totalPages.value = Math.ceil(allDocs.length / pageSize)
    currentPage.value = page
    documents.value = allDocs.slice(page * pageSize, (page + 1) * pageSize)
  } catch (e) { console.error(e) }
}

const togglePublic = async (doc: any) => {
  try {
    await api.post(`/knowledge/${doc.id}/toggle-public`)
    ElMessage({ message: isDocPublic(doc) ? '已取消公开' : '已设为公开', type: 'success', duration: 1500 })
    fetchDocuments(currentPage.value)
  } catch { ElMessage.error('操作失败') }
}

const deleteDoc = async (id: number) => {
  try {
    await api.delete(`/knowledge/${id}`)
    ElMessage.success('已删除')
    fetchDocuments(currentPage.value)
  } catch { ElMessage.error('删除失败') }
}

// Preview
const previewVisible = ref(false)
const previewDoc_ = ref<any>({})

const previewDoc = (doc: any) => {
  previewDoc_.value = doc
  previewVisible.value = true
}

const viewOriginal = async (doc: any) => {
  const token = localStorage.getItem('jwt_token')
  if (!token) { ElMessage.error('请先登录'); return }
  try {
    const res = await fetch(`/api/knowledge/view/${doc.id}`, {
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
      ElMessage.error('无法查看文件')
    }
  } catch { ElMessage.error('请求失败') }
}

// Edit tags dialog
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
    fetchDocuments(currentPage.value)
  } catch {
    ElMessage.error('标签更新失败')
  }
}

onMounted(() => fetchDocuments(0))
</script>

<style scoped>
.manage-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 24px;
}
.upload-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
  align-self: flex-start;
}
.upload-inner { padding: 24px; text-align: center; }
.upload-text { font-size: 13px; color: var(--text-secondary); margin-top: 8px; }
.upload-text em { color: var(--accent); font-style: normal; }
.upload-hint { font-size: 11px; color: var(--text-muted); margin-top: 4px; }
.tags-input { margin-top: 16px; }
.tags-input label { font-size: 12px; color: var(--text-secondary); display: block; margin-bottom: 4px; }
.upload-btn {
  width: 100%; margin-top: 16px; border-radius: 8px;
  background: var(--accent) !important; border-color: var(--accent) !important;
}
.doc-list-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
}
.doc-list-card h3 { margin: 0 0 16px; font-size: 16px; color: var(--text-primary); }
.doc-count { font-size: 13px; color: var(--text-muted); font-weight: 400; }
.empty-hint { text-align: center; color: var(--text-muted); padding: 40px 0; font-size: 13px; }
.doc-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-color);
  gap: 8px;
}
.doc-row:last-child { border-bottom: none; }
.doc-info { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 0; flex-wrap: wrap; }
.doc-name {
  font-size: 13px; color: var(--accent); cursor: pointer;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 220px;
}
.doc-name:hover { text-decoration: underline; }
.doc-actions { display: flex; gap: 4px; flex-shrink: 0; flex-wrap: wrap; justify-content: flex-end; }
@media (max-width: 600px) {
  .doc-row { flex-direction: column; align-items: flex-start; }
  .doc-actions { width: 100%; justify-content: flex-start; }
}
.pagination-wrap { display: flex; justify-content: center; margin-top: 16px; }
.preview-content {
  max-height: 60vh; overflow-y: auto;
  font-size: 14px; line-height: 1.8; color: var(--text-primary);
}
.preview-content :deep(p) { margin: 0 0 12px; }
.preview-content :deep(pre) { background: #1e293b; color: #e2e8f0; padding: 12px; border-radius: 8px; overflow-x: auto; }
@media (max-width: 768px) { .manage-layout { grid-template-columns: 1fr; } }
</style>
