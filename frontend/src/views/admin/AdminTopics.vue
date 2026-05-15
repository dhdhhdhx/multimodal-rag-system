<template>
  <div class="admin-topics">
    <div class="admin-page-header">
      <h2 class="admin-page-title">话题管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showCreateDialog">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 4px">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          创建话题
        </el-button>
      </div>
    </div>

    <div class="admin-filter-bar">
      <el-input v-model="searchKeyword" placeholder="搜索话题..." clearable style="width: 220px" @input="handleSearch">
        <template #prefix>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
        </template>
      </el-input>
      <el-select v-model="filterStatus" placeholder="状态" style="width: 120px" @change="handleSearch">
        <el-option label="全部" value="" />
        <el-option label="公开" value="public" />
        <el-option label="私密" value="private" />
      </el-select>
      <el-radio-group v-model="sortBy" size="small" @change="sortTopics">
        <el-radio-button value="created">按时间</el-radio-button>
        <el-radio-button value="docs">按文档数</el-radio-button>
        <el-radio-button value="subscribers">按订阅数</el-radio-button>
      </el-radio-group>
    </div>

    <div class="topics-table-wrapper" v-if="topics.length > 0">
      <el-table ref="tableRef" :data="paginatedTopics" stripe row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-docs">
              <div class="expand-docs-header">
                <span class="expand-docs-title">话题文档</span>
                <div class="expand-docs-actions">
                  <el-button type="primary" size="small" @click="showAddDocDialog(row)">添加文档</el-button>
                  <el-button link type="primary" size="small" :loading="expandLoading[row.id]" @click="refreshExpandDocs(row.id)">
                    刷新
                  </el-button>
                </div>
              </div>
              <div v-if="expandLoading[row.id] && !expandDocs[row.id]" class="expand-docs-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>加载中...</span>
              </div>
              <div v-else-if="!expandDocs[row.id] || expandDocs[row.id].length === 0" class="expand-docs-empty">
                暂无文档
              </div>
              <el-table v-else :data="expandDocs[row.id]" size="small" class="expand-docs-table">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="fileName" label="文件名" min-width="200">
                  <template #default="{ row: doc }">
                    <span class="doc-file-name">{{ doc.fileName }}</span>
                    <el-tag size="small" type="info" style="margin-left: 6px">{{ (doc.fileType || '').toUpperCase() }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="fileSize" label="大小" width="100" align="center">
                  <template #default="{ row: doc }">
                    {{ formatFileSize(doc.fileSize) }}
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="90" align="center">
                  <template #default="{ row: doc }">
                    <el-tag size="small" :type="doc.status === 'COMPLETED' ? 'success' : doc.status === 'PROCESSING' ? 'warning' : 'info'">
                      {{ doc.status }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" align="center">
                  <template #default="{ row: doc }">
                    <el-popconfirm title="确定从话题中移除该文档？" @confirm="removeDocFromTopic(row.id, doc.id)">
                      <template #reference>
                        <el-button link type="danger" size="small">移除</el-button>
                      </template>
                    </el-popconfirm>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="话题名称" min-width="180">
          <template #default="{ row }">
            <span>{{ row.name }}</span>
            <el-tag v-if="row.isPublic" size="small" type="success" style="margin-left: 8px">公开</el-tag>
            <el-tag v-else size="small" type="info" style="margin-left: 8px">私密</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200">
          <template #default="{ row }">
            <span class="desc-text">{{ row.description || '暂无描述' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ownerName" label="创建者" width="120" />
        <el-table-column prop="documentCount" label="文档数" width="100" align="center" />
        <el-table-column prop="subscriberCount" label="订阅数" width="100" align="center" />
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="editTopic(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="toggleExpand(row)">查看</el-button>
            <el-popconfirm title="确定要删除此话题吗？" @confirm="deleteTopic(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredTopics.length"
          :page-size="pageSize"
          :current-page="currentPage + 1"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="(size: number) => { pageSize = size; currentPage = 0 }"
          @current-change="(p: number) => currentPage = p - 1"
        />
      </div>
    </div>

    <div v-else class="admin-empty">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="margin-bottom:8px; opacity:0.4;">
        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
      </svg>
      <span>暂无话题数据</span>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingTopic ? '编辑话题' : '创建话题'"
      width="500px"
    >
      <el-form :model="topicForm" label-width="80px">
        <el-form-item label="话题名称">
          <el-input v-model="topicForm.name" placeholder="请输入话题名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="topicForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入话题描述（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="公开设置">
          <el-switch v-model="topicForm.isPublic" active-text="公开" inactive-text="私密" />
          <span class="form-hint">公开后所有用户可见和订阅</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTopic" :loading="saving">
          {{ editingTopic ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Add Document Dialog -->
    <el-dialog v-model="addDocDialogVisible" title="添加文档到话题" width="700px">
      <div class="add-doc-filters">
        <el-select v-model="addDocTagFilter" placeholder="按标签筛选" clearable style="width: 180px">
          <el-option label="全部标签" value="" />
          <el-option v-for="tag in availableTags" :key="tag" :label="tag" :value="tag" />
        </el-select>
        <el-input v-model="addDocSearch" placeholder="搜索文档名称..." clearable style="flex: 1">
          <template #prefix>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
          </template>
        </el-input>
      </div>
      <div v-if="addDocLoading" class="add-doc-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载可用文档...</span>
      </div>
      <el-table v-else :data="filteredAvailableDocs" size="small" max-height="400" row-key="id" @selection-change="handleAddDocSelection">
        <el-table-column type="selection" width="50" :reserve-selection="true" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="fileType" label="类型" width="70" align="center">
          <template #default="{ row }">{{ (row.fileType || '').toUpperCase() }}</template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="90" align="center">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="addDocDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addDocsToTopic" :loading="addDocSaving" :disabled="selectedAddDocs.length === 0">
          添加 ({{ selectedAddDocs.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import api from '../../api'

const topics = ref<any[]>([])
const searchKeyword = ref('')
const filterStatus = ref('')
const sortBy = ref('created')
const currentPage = ref(0)
const pageSize = ref(20)
const dialogVisible = ref(false)
const editingTopic = ref<any>(null)
const saving = ref(false)

// Expand row state: topicId -> documents[]
const expandDocs = ref<Record<number, any[]>>({})
const expandLoading = ref<Record<number, boolean>>({})
const tableRef = ref<any>(null)

// Add document dialog state
const addDocDialogVisible = ref(false)
const addDocTopicId = ref<number | null>(null)
const allAvailableDocs = ref<any[]>([])
const addDocSearch = ref('')
const selectedAddDocs = ref<any[]>([])
const addDocLoading = ref(false)
const addDocSaving = ref(false)
const availableTags = ref<string[]>([])
const addDocTagFilter = ref('')

const topicForm = ref({
  name: '',
  description: '',
  isPublic: false
})

const filteredTopics = computed(() => {
  let result = topics.value

  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    result = result.filter(t =>
      t.name.toLowerCase().includes(kw) ||
      (t.description && t.description.toLowerCase().includes(kw))
    )
  }

  if (filterStatus.value === 'public') {
    result = result.filter(t => t.isPublic)
  } else if (filterStatus.value === 'private') {
    result = result.filter(t => !t.isPublic)
  }

  return result
})

const paginatedTopics = computed(() => {
  const start = currentPage.value * pageSize.value
  return filteredTopics.value.slice(start, start + pageSize.value)
})

const formatTime = (t: string) => {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN')
}

const formatFileSize = (size: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}

const sortTopics = () => {
  if (sortBy.value === 'docs') {
    topics.value.sort((a, b) => (b.documentCount || 0) - (a.documentCount || 0))
  } else if (sortBy.value === 'subscribers') {
    topics.value.sort((a, b) => (b.subscriberCount || 0) - (a.subscriberCount || 0))
  } else {
    topics.value.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  }
}

const handleSearch = () => {
  currentPage.value = 0
}

const fetchTopics = async () => {
  try {
    const res = await api.get('/topics')
    topics.value = res.data || []
    sortTopics()
  } catch (e) {
    console.error('获取话题列表失败:', e)
    ElMessage.error('获取话题列表失败')
  }
}

const fetchTopicDocuments = async (topicId: number) => {
  expandLoading.value[topicId] = true
  try {
    const res = await api.get(`/topics/${topicId}/documents`)
    expandDocs.value[topicId] = res.data || []
  } catch (e) {
    console.error('获取话题文档失败:', e)
    expandDocs.value[topicId] = []
  } finally {
    expandLoading.value[topicId] = false
  }
}

const refreshExpandDocs = async (topicId: number) => {
  await fetchTopicDocuments(topicId)
}

const toggleExpand = async (row: any) => {
  // 先确保行展开
  if (tableRef.value) {
    tableRef.value.toggleRowExpansion(row, true)
  }
  // 加载文档
  await fetchTopicDocuments(row.id)
}

const removeDocFromTopic = async (topicId: number, docId: number) => {
  try {
    await api.delete(`/topics/${topicId}/documents/${docId}`)
    ElMessage.success('已从话题中移除')
    await fetchTopicDocuments(topicId)
    await fetchTopics()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '移除失败')
  }
}

// --- Add Document to Topic ---
const filteredAvailableDocs = computed(() => {
  let result = allAvailableDocs.value
  // 标签筛选
  if (addDocTagFilter.value) {
    result = result.filter(d => {
      if (!d.tags) return false
      return d.tags.split(',').map((t: string) => t.trim()).includes(addDocTagFilter.value)
    })
  }
  // 文件名搜索
  if (addDocSearch.value.trim()) {
    const kw = addDocSearch.value.trim().toLowerCase()
    result = result.filter(d => d.fileName.toLowerCase().includes(kw))
  }
  return result
})

const showAddDocDialog = async (topic: any) => {
  addDocTopicId.value = topic.id
  addDocSearch.value = ''
  selectedAddDocs.value = []
  addDocDialogVisible.value = true
  addDocLoading.value = true
  addDocTagFilter.value = ''
  try {
    // 获取当前话题已有文档ID，用于过滤
    const existingDocs = expandDocs.value[topic.id] || []
    const existingIds = new Set(existingDocs.map((d: any) => d.id))
    // 获取所有文档
    const res = await api.get('/knowledge/documents')
    allAvailableDocs.value = (res.data || []).filter((d: any) => !existingIds.has(d.id))
    // 提取所有标签
    const tagSet = new Set<string>()
    for (const d of allAvailableDocs.value) {
      if (d.tags) {
        for (const t of d.tags.split(',')) {
          const trimmed = t.trim()
          if (trimmed) tagSet.add(trimmed)
        }
      }
    }
    availableTags.value = Array.from(tagSet).sort()
  } catch (e) {
    console.error('获取文档列表失败:', e)
    ElMessage.error('获取文档列表失败')
    allAvailableDocs.value = []
  } finally {
    addDocLoading.value = false
  }
}

const handleAddDocSelection = (selection: any[]) => {
  selectedAddDocs.value = selection
}

const addDocsToTopic = async () => {
  if (!addDocTopicId.value || selectedAddDocs.value.length === 0) return
  addDocSaving.value = true
  let success = 0
  let failed = 0
  for (const doc of selectedAddDocs.value) {
    try {
      await api.post(`/topics/${addDocTopicId.value}/documents/${doc.id}`)
      success++
    } catch (e: any) {
      failed++
      console.error(`添加文档 ${doc.id} 失败:`, e)
    }
  }
  addDocSaving.value = false
  addDocDialogVisible.value = false
  if (success > 0) ElMessage.success(`成功添加 ${success} 个文档`)
  if (failed > 0) ElMessage.warning(`${failed} 个文档添加失败`)
  // 刷新
  await fetchTopicDocuments(addDocTopicId.value)
  await fetchTopics()
}

const showCreateDialog = () => {
  editingTopic.value = null
  topicForm.value = { name: '', description: '', isPublic: false }
  dialogVisible.value = true
}

const editTopic = (topic: any) => {
  editingTopic.value = topic
  topicForm.value = {
    name: topic.name,
    description: topic.description || '',
    isPublic: topic.isPublic || false
  }
  dialogVisible.value = true
}

const saveTopic = async () => {
  if (!topicForm.value.name.trim()) {
    ElMessage.warning('请输入话题名称')
    return
  }

  saving.value = true
  try {
    if (editingTopic.value) {
      await api.put(`/topics/${editingTopic.value.id}`, topicForm.value)
      ElMessage.success('话题已更新')
    } else {
      await api.post('/topics', topicForm.value)
      ElMessage.success('话题已创建')
    }
    dialogVisible.value = false
    await fetchTopics()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

const deleteTopic = async (id: number) => {
  try {
    await api.delete(`/topics/${id}`)
    ElMessage.success('话题已删除')
    delete expandDocs.value[id]
    await fetchTopics()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  fetchTopics()
})
</script>

<style scoped>
.admin-topics {
  max-width: 1400px;
  margin: 0 auto;
}

.admin-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.admin-page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.admin-filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.topics-table-wrapper {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.desc-text {
  color: var(--text-secondary);
  font-size: 13px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

.form-hint {
  margin-left: 12px;
  font-size: 12px;
  color: var(--text-muted);
}

.admin-empty {
  text-align: center;
  padding: 80px 0;
  color: var(--text-muted);
}

.admin-empty svg {
  margin-bottom: 12px;
}

/* Expand row docs */
.expand-docs {
  padding: 16px 20px;
}

.expand-docs-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.expand-docs-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.expand-docs-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.add-doc-filters {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.add-doc-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 13px;
  padding: 20px 0;
}

.expand-docs-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 13px;
  padding: 20px 0;
}

.expand-docs-empty {
  color: var(--text-muted);
  font-size: 13px;
  padding: 12px 0;
}

.expand-docs-table {
  width: 100%;
}

.doc-file-name {
  color: var(--text-primary);
  font-size: 13px;
}
</style>
