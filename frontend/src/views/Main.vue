<template>
  <el-container class="layout-container">
    <el-aside width="300px" class="sidebar">
      <div class="upload-section">
        <el-upload class="upload-demo" action="/api/knowledge/upload" :headers="uploadHeaders" :on-success="handleUploadSuccess" :on-error="handleUploadError" :auto-upload="true" name="file" multiple drag>
          <div class="upload-inner">
            <el-icon class="upload-icon"><Plus /></el-icon>
            <div class="upload-text">拖拽文件到此处，或<em>点击上传</em></div>
            <div class="upload-hint">支持 文本、图像、音频、视频</div>
          </div>
        </el-upload>
      </div>
      <el-tabs v-model="activeTab" class="sidebar-tabs">
        <el-tab-pane label="文件管理" name="docs">
          <div class="doc-list-section">
            <el-scrollbar max-height="calc(100vh - 300px)">
              <div v-if="documents.length === 0" class="empty-hint">暂无文件，请上传知识文件</div>
              <div v-for="doc in documents" :key="doc.id" class="doc-item">
                <el-icon class="doc-icon"><DocIcon /></el-icon>
                <span class="doc-name" :title="doc.fileName">{{ doc.fileName }}</span>
                <el-tag size="small" :type="getStatusType(doc.status)" class="status-tag">{{ doc.status }}</el-tag>
                <div class="doc-actions">
                  <el-tag v-if="isDocPublic(doc)" size="small" type="success" effect="dark" class="public-tag">公开</el-tag>
                  <el-button link type="primary" :icon="View" @click="viewDocument(doc.id)" />
                  <el-dropdown trigger="click">
                    <el-button link :icon="More" />
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item v-if="canManage(doc)" :icon="isDocPublic(doc) ? Lock : Share" @click="handleTogglePublic(doc)">{{ isDocPublic(doc) ? '取消公开' : '设为公开' }}</el-dropdown-item>
                        <el-dropdown-item divided type="danger" :icon="Delete" @click="deleteDocument(doc.id)">删除文档</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
            </el-scrollbar>
          </div>
        </el-tab-pane>
        <el-tab-pane label="探索发现" name="discovery">
          <p class="tab-hint">探索发现已开启，详情见右侧面板</p>
        </el-tab-pane>
        <el-tab-pane label="个人仪表盘" name="stats">
          <UserDashboard :refreshTrigger="refreshCounter" />
        </el-tab-pane>
      </el-tabs>
    </el-aside>

    <el-main class="main-content">
      <div v-if="activeTab !== 'discovery'" class="chat-container">
        <div class="chat-header">
          <h2>AI 智能助手</h2>
          <p class="chat-desc">基于您的知识库进行智能问答</p>
        </div>
        <div class="chat-messages" ref="messageBox">
          <div v-if="chatHistory.length === 0" class="chat-welcome">
            <div class="welcome-icon">
              <el-icon :size="40"><Promotion /></el-icon>
            </div>
            <h3>开始对话</h3>
            <p>上传知识文件后，向 AI 提问获取精准答案</p>
          </div>
          <div v-for="(msg, index) in chatHistory" :key="index" :class="['message', msg.role]">
            <div class="message-bubble">
              <div class="role-tag">{{ msg.role === 'user' ? '您' : 'AI' }}</div>
              <div class="content" v-html="renderMarkdown(msg.content)"></div>
            </div>
          </div>
          <div v-if="loading" class="message ai">
            <div class="message-bubble loading-bubble">
              <el-skeleton :rows="2" animated />
            </div>
          </div>
        </div>
        <div class="chat-input-area">
          <el-input v-model="userInput" placeholder="关于您的知识库，想问点什么？..." @keyup.enter="sendMessage" size="large" clearable>
            <template #append>
              <el-button @click="sendMessage" type="primary" :loading="loading">
                <el-icon><Promotion /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
      <div v-else class="discovery-view-area">
        <DiscoverySection />
      </div>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import MarkdownIt from 'markdown-it'
import UserDashboard from '../components/UserDashboard.vue'
import DiscoverySection from '../components/DiscoverySection.vue'
import { Plus, Document as DocIcon, View, Delete, More, Lock, Share, Promotion, Setting } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const documents = ref<any[]>([])
const chatHistory = ref<{role: string, content: string}[]>([])
const userInput = ref('')
const loading = ref(false)
const messageBox = ref<any>(null)
const activeTab = ref('docs')
const refreshCounter = ref(0)

const isAdminUser = computed(() => {
  const userInfoStr = localStorage.getItem('user_info')
  if (!userInfoStr) return false
  try {
    const user = JSON.parse(userInfoStr)
    if (!user || !user.roles) return false
    return user.roles.some((role: any) => {
      if (typeof role === 'string') return role === 'ADMIN'
      return role.name === 'ADMIN'
    })
  } catch (e) { return false }
})

const uploadHeaders = ref({
  Authorization: `Bearer ${localStorage.getItem('jwt_token') || ''}`
})

const fetchDocuments = async () => {
  try {
    const res = await api.get('/knowledge/documents')
    documents.value = res.data
  } catch (error) { console.error('Failed to fetch documents', error) }
}

const handleUploadSuccess = () => {
  fetchDocuments()
  refreshCounter.value++
}

const handleUploadError = (error: any) => {
  console.error('Upload failed:', error)
  ElMessage.error('上传失败，请检查后端日志')
}

const viewDocument = (docId: number) => {
  const doc = documents.value.find(d => d.id === docId)
  if (!doc) { ElMessage.error('文档未找到'); return }
  if (doc.status !== 'COMPLETED') { ElMessage.warning('文档仍在处理中或处理失败'); return }
  window.open(`/api/knowledge/view/${docId}`, '_blank')
}

const canManage = (doc: any) => {
  const userInfoStr = localStorage.getItem('user_info')
  if (!userInfoStr || userInfoStr === 'undefined') return false
  try {
    const user = JSON.parse(userInfoStr)
    if (!user || !user.roles) return false
    const isAdmin = user.roles?.some((r: any) => {
      if (typeof r === 'string') return r === 'ADMIN' || r === 'ROLE_ADMIN'
      return r.name === 'ADMIN' || r.name === 'ROLE_ADMIN'
    })
    return isAdmin || doc.userId === user.id
  } catch (e) { return false }
}

const isDocPublic = (doc: any) => {
  if (!doc) return false
  const val = doc.isPublic ?? doc.public ?? doc.shared ?? doc.is_public
  return val === true || val === 1 || val === 'true'
}

const handleTogglePublic = async (doc: any) => {
  try {
    const currentlyPublic = isDocPublic(doc)
    await api.post(`/knowledge/${doc.id}/toggle-public`)
    ElMessage.success(currentlyPublic ? '已取消公开' : '已设为公开')
    fetchDocuments()
  } catch (error) { ElMessage.error('操作失败') }
}

const deleteDocument = async (id: number) => {
  try {
    await api.delete(`/knowledge/${id}`)
    ElMessage.success('文档已删除')
    fetchDocuments()
    refreshCounter.value++
  } catch (error) { ElMessage.error('删除失败') }
}

const sendMessage = async () => {
  if (!userInput.value.trim() || loading.value) return
  const query = userInput.value
  chatHistory.value.push({ role: 'user', content: query })
  userInput.value = ''
  loading.value = true
  await scrollToBottom()
  try {
    const res = await api.post('/chat', { query })
    chatHistory.value.push({ role: 'ai', content: res.data.answer })
  } catch (error) {
    chatHistory.value.push({ role: 'ai', content: '抱歉，遇到了错误，请检查后端日志。' })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

const renderMarkdown = (content: string) => content ? md.render(content) : ''

const getStatusType = (status: string) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'PROCESSING': return 'warning'
    case 'FAILED': return 'danger'
    default: return 'info'
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageBox.value) messageBox.value.scrollTop = messageBox.value.scrollHeight
}

onMounted(() => { fetchDocuments() })
</script>

<style scoped>
.layout-container {
  height: calc(100vh - 56px);
  padding: 16px;
  box-sizing: border-box;
  gap: 16px;
}

.sidebar {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  height: 100%;
}

.upload-section { margin-bottom: 12px; }

.upload-inner {
  padding: 20px 16px;
  text-align: center;
}

.upload-icon {
  font-size: 28px;
  color: #818cf8;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 13px;
  color: #94a3b8;
}

.upload-text em {
  color: #818cf8;
  font-style: normal;
}

.upload-hint {
  font-size: 11px;
  color: #475569;
  margin-top: 4px;
}

.sidebar-tabs { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.sidebar-tabs :deep(.el-tabs__content) { flex: 1; overflow-y: auto; }

.tab-hint {
  color: #94a3b8;
  font-size: 0.8rem;
  margin: 10px 0;
}

.empty-hint {
  text-align: center;
  color: #475569;
  font-size: 13px;
  padding: 30px 0;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  margin-bottom: 4px;
  font-size: 0.85rem;
  transition: background 0.2s;
}

.doc-item:hover { background: rgba(255, 255, 255, 0.05); }

.doc-icon { color: #64748b; flex-shrink: 0; }

.doc-name {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-tag { zoom: 0.8; }

.public-tag { margin-right: 4px; font-size: 10px; padding: 0 4px; }

.doc-actions { display: flex; align-items: center; gap: 2px; }

/* Main content */
.main-content {
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  overflow: hidden;
}

.chat-header {
  padding: 20px 24px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.chat-header h2 {
  font-size: 1.3rem;
  margin: 0 0 4px 0;
  color: #f1f5f9;
}

.chat-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #475569;
  text-align: center;
}

.welcome-icon {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: rgba(129, 140, 248, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #818cf8;
  margin-bottom: 16px;
}

.chat-welcome h3 {
  margin: 0 0 8px 0;
  color: #94a3b8;
  font-size: 18px;
}

.chat-welcome p {
  margin: 0;
  font-size: 14px;
}

.message { display: flex; width: 100%; }
.message.user { justify-content: flex-end; }

.message-bubble {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.user .message-bubble {
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
  border: none;
  color: white;
}

.loading-bubble {
  min-width: 200px;
}

.role-tag {
  font-size: 0.7rem;
  font-weight: 600;
  margin-bottom: 4px;
  opacity: 0.7;
  text-transform: uppercase;
}

.chat-input-area {
  padding: 16px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.discovery-view-area {
  flex: 1;
  overflow-y: auto;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
}

::-webkit-scrollbar { width: 5px; }
::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.08); border-radius: 10px; }
</style>
