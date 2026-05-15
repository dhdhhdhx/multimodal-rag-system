<template>
  <div class="ai-chat-page">
    <div class="chat-layout">
      <!-- Sidebar: Session List -->
      <div class="session-sidebar" :class="{ collapsed: !showSidebar }">
        <div class="sidebar-top">
          <h3>会话记录</h3>
          <el-button size="small" type="primary" @click="createSession">新建会话</el-button>
        </div>
        <div class="session-list">
          <div v-for="s in sessions" :key="s.id"
            :class="['session-item', { active: currentSessionId === s.id }]"
            @click="selectSession(s.id)">
            <div class="session-title">{{ s.title || '新会话' }}</div>
            <div class="session-time">{{ formatTime(s.updatedAt) }}</div>
            <el-icon class="session-delete" @click.stop="deleteSession(s.id)"><Close /></el-icon>
          </div>
          <div v-if="sessions.length === 0" class="empty-sessions">暂无会话记录</div>
        </div>
      </div>

      <!-- Main Chat Area -->
      <div class="chat-main">
        <div class="chat-header">
          <el-button :icon="showSidebar ? ArrowLeft : ChatLineSquare" link @click="showSidebar = !showSidebar" />
          <span class="chat-title">AI 知识助手</span>
          <el-button :icon="Delete" link @click="clearChat">清除当前</el-button>
        </div>
        <div class="chat-messages" ref="messageBox">
          <div v-if="chatHistory.length === 0" class="welcome">
            <div class="welcome-icon">
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="1.5">
                <rect x="3" y="3" width="8" height="8" rx="2" opacity="0.4"/>
                <rect x="13" y="3" width="8" height="8" rx="2"/>
                <rect x="3" y="13" width="8" height="8" rx="2"/>
                <rect x="13" y="13" width="8" height="8" rx="2" opacity="0.4"/>
              </svg>
            </div>
            <h3>开始探索知识库</h3>
            <p>我可以基于知识库中的文档和文章回答你的问题</p>
          </div>
          <div v-for="(msg, i) in chatHistory" :key="i" :class="['message', msg.role]">
            <div class="msg-bubble">
              <div class="msg-role">{{ msg.role === 'user' ? '你' : 'AI' }}</div>
              <div class="msg-content" v-html="renderMd(msg.content)"></div>
              <!-- Source reference cards -->
              <div v-if="msg.sources && msg.sources.length" class="msg-sources">
                <div class="sources-label">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
                  参考来源（{{ msg.sources.length }}）
                </div>
                <div v-for="(src, j) in msg.sources" :key="j" class="source-card">
                  <div class="source-card-header">
                    <span class="source-card-icon" :class="getFileTypeClass(src.fileType)">{{ getFileTypeIcon(src.fileType) }}</span>
                    <div class="source-card-info">
                      <div class="source-card-name">{{ src.fileName || '未知文件' }}</div>
                      <div class="source-card-meta">
                        <span class="source-card-type">{{ src.fileType?.toUpperCase() || 'FILE' }}</span>
                        <span v-if="src.modality" class="source-card-modality">{{ src.modality }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="source-card-excerpt" v-html="highlightExcerpt(src.excerpt, msg)"></div>
                  <div class="source-card-footer">
                    <div class="source-card-score">
                      <span class="score-label">相关度</span>
                      <el-progress
                        :percentage="getScorePercent(src.score)"
                        :stroke-width="6"
                        :show-text="false"
                        :color="getScoreColor(src.score)"
                        style="width: 80px;"
                      />
                      <span class="score-value" :style="{ color: getScoreColor(src.score) }">{{ getScorePercent(src.score) }}%</span>
                    </div>
                    <div class="source-card-actions">
                      <button class="src-action-btn" title="预览引用" @click="previewSource(src, msg)">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                        <span>预览</span>
                      </button>
                      <button class="src-action-btn" title="跳转到文档" @click="viewSource(src)">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
                        <span>跳转</span>
                      </button>
                      <button class="src-action-btn" title="复制引用" @click="copyQuote(src)">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                        <span>引用</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Source preview dialog -->
              <el-dialog v-model="previewVisible" title="引用预览" width="560px" :modal="false" class="source-preview-dialog">
                <template #header>
                  <div class="preview-dialog-header">
                    <span class="preview-dialog-icon" :class="getFileTypeClass(previewSrc?.fileType)">{{ getFileTypeIcon(previewSrc?.fileType) }}</span>
                    <span>{{ previewSrc?.fileName || '未知文件' }}</span>
                  </div>
                </template>
                <div class="preview-dialog-content" v-html="renderMd(previewContent)"></div>
              </el-dialog>
            </div>
          </div>
          <div v-if="loading" class="message ai">
            <div class="msg-bubble loading-bubble">
              <div class="typing-dots"><span/><span/><span/></div>
            </div>
          </div>
        </div>
        <div class="chat-input-area">
          <div class="input-wrap">
            <textarea v-model="userInput" class="chat-input"
              placeholder="输入你的问题..."
              rows="1"
              @keydown="handleKeydown"
              @input="autoGrow" ref="chatInputRef" />
            <button class="send-btn" @click="sendMessage" :disabled="loading || !userInput.trim()">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
              </svg>
            </button>
          </div>
          <p class="input-hint">Enter 发送 · Shift + Enter 换行</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { ArrowLeft, Delete, Close, ChatLineSquare } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import api, { getAccessToken } from '../api'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

interface ChatMsg {
  role: string
  content: string
  sources?: any[]
}

const chatHistory = ref<ChatMsg[]>([])
const userInput = ref('')
const loading = ref(false)
const messageBox = ref<HTMLElement | null>(null)
const chatInputRef = ref<HTMLTextAreaElement | null>(null)
const showSidebar = ref(true)

// Session management
const sessions = ref<any[]>([])
const currentSessionId = ref<number | null>(null)

const renderMd = (content: string) => content ? md.render(content) : ''

const highlightExcerpt = (excerpt: string, msg: ChatMsg) => {
  if (!excerpt) return ''
  // Find the preceding user message to get the query keywords
  const idx = chatHistory.value.indexOf(msg)
  let query = ''
  for (let i = idx - 1; i >= 0; i--) {
    if (chatHistory.value[i].role === 'user') {
      query = chatHistory.value[i].content
      break
    }
  }
  if (!query) return excerpt
  // Highlight keywords from the query
  const words = query.split(/\s+/).filter(w => w.length >= 2)
  let result = excerpt
  for (const word of words) {
    const escaped = word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    result = result.replace(new RegExp(escaped, 'gi'), '<mark>$&</mark>')
  }
  return result
}

const formatTime = (t: string) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageBox.value) messageBox.value.scrollTop = messageBox.value.scrollHeight
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

const autoGrow = () => {
  const el = chatInputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 5 * parseFloat(getComputedStyle(el).lineHeight)) + 'px'
}

const fetchSessions = async () => {
  try {
    const res = await api.get('/chat/sessions')
    sessions.value = res.data
  } catch { /* ignore */ }
}

const selectSession = async (id: number) => {
  currentSessionId.value = id
  try {
    const res = await api.get(`/chat/sessions/${id}/messages`)
    chatHistory.value = res.data.map((m: any) => ({
      role: m.role,
      content: m.content,
      sources: m.sources ? JSON.parse(m.sources) : undefined
    }))
    await scrollToBottom()
  } catch { chatHistory.value = [] }
}

const createSession = async () => {
  currentSessionId.value = null
  chatHistory.value = []
}

const deleteSession = async (id: number) => {
  try {
    await api.delete(`/chat/sessions/${id}`)
    if (currentSessionId.value === id) {
      currentSessionId.value = null
      chatHistory.value = []
    }
    fetchSessions()
    ElMessage.success('会话已删除')
  } catch { ElMessage.error('删除失败') }
}

const sendMessage = async () => {
  if (!userInput.value.trim() || loading.value) return
  const query = userInput.value
  chatHistory.value.push({ role: 'user', content: query })
  userInput.value = ''
  if (chatInputRef.value) chatInputRef.value.style.height = 'auto'
  loading.value = true
  await scrollToBottom()
  try {
    const body: any = { query }
    if (currentSessionId.value) body.sessionId = currentSessionId.value
    const res = await api.post('/chat', body)

    const sources = res.data.sources || []
    chatHistory.value.push({ role: 'ai', content: res.data.answer, sources })

    // Update session id
    if (res.data.sessionId) {
      currentSessionId.value = res.data.sessionId
      fetchSessions()
    }
  } catch {
    chatHistory.value.push({ role: 'ai', content: '抱歉，遇到了错误，请稍后重试。' })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

const clearChat = () => {
  chatHistory.value = []
  currentSessionId.value = null
  ElMessage.success('会话已清除')
}

// Source card helpers
const previewVisible = ref(false)
const previewSrc = ref<any>(null)
const previewContent = ref('')

const getFileTypeIcon = (type: string) => {
  const t = (type || '').toLowerCase()
  if (t === 'pdf') return 'PDF'
  if (['doc', 'docx'].includes(t)) return 'DOC'
  if (['xls', 'xlsx'].includes(t)) return 'XLS'
  if (['ppt', 'pptx'].includes(t)) return 'PPT'
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg'].includes(t)) return 'IMG'
  if (['mp4', 'avi', 'mov', 'webm'].includes(t)) return 'VID'
  if (['mp3', 'wav', 'ogg'].includes(t)) return 'AUD'
  if (t === 'txt' || t === 'md') return 'TXT'
  return 'FILE'
}

const getFileTypeClass = (type: string) => {
  const t = (type || '').toLowerCase()
  if (t === 'pdf') return 'ft-pdf'
  if (['doc', 'docx'].includes(t)) return 'ft-doc'
  if (['xls', 'xlsx'].includes(t)) return 'ft-xls'
  if (['ppt', 'pptx'].includes(t)) return 'ft-ppt'
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg'].includes(t)) return 'ft-img'
  if (['mp4', 'avi', 'mov', 'webm'].includes(t)) return 'ft-vid'
  if (['mp3', 'wav', 'ogg'].includes(t)) return 'ft-aud'
  return 'ft-default'
}

const getScorePercent = (score: number) => Math.min(Math.round((score || 0.5) * 100), 99)

const getScoreColor = (score: number) => {
  const s = score || 0.5
  if (s >= 0.8) return '#10b981'
  if (s >= 0.6) return '#f59e0b'
  return '#ef4444'
}

const previewSource = (src: any, _msg: ChatMsg) => {
  previewSrc.value = src
  previewContent.value = src.excerpt || '暂无引用内容'
  previewVisible.value = true
}

const viewSource = async (src: any) => {
  if (!src.docId) return
  const token = getAccessToken()
  if (!token) {
    ElMessage.error('请先登录')
    return
  }
  try {
    const res = await fetch(`/api/knowledge/view/${src.docId}?proxy=true`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const contentType = res.headers.get('Content-Type') || ''
    if (contentType.includes('text/plain')) {
      const text = await res.text()
      if (text.length < 200) {
        ElMessage.warning('该文档没有原始文件')
        return
      }
      const blobUrl = URL.createObjectURL(new Blob([text], { type: 'text/html' }))
      window.open(blobUrl, '_blank')
      setTimeout(() => URL.revokeObjectURL(blobUrl), 120000)
    } else {
      const blob = await res.blob()
      const blobUrl = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = blobUrl
      a.target = '_blank'
      a.download = src.fileName || 'file'
      a.click()
      setTimeout(() => URL.revokeObjectURL(blobUrl), 120000)
    }
  } catch (e: any) {
    ElMessage.error('无法打开文档')
    console.error('viewSource error:', e)
  }
}

const copyQuote = (src: any) => {
  const text = `[${src.fileName || '未知文件'}] ${src.excerpt || ''}`
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('引用已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

onMounted(fetchSessions)
</script>

<style scoped>
.ai-chat-page { height: calc(100vh - 140px); }
.chat-layout { display: flex; height: 100%; gap: 0; }

/* Sidebar */
.session-sidebar {
  width: 260px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg) 0 0 var(--radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.2s;
}
.session-sidebar.collapsed { width: 0; border: none; }
.sidebar-top {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sidebar-top h3 { margin: 0; font-size: 14px; color: var(--text-primary); }
.session-list { flex: 1; overflow-y: auto; padding: 8px; }
.session-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
}
.session-item:hover { background: var(--bg-secondary); }
.session-item.active { background: #ecfdf5; border-left: 3px solid var(--accent); }
.session-title {
  font-size: 13px; color: var(--text-primary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  padding-right: 20px;
}
.session-time { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.session-delete {
  position: absolute; right: 8px; top: 12px;
  color: var(--text-muted); cursor: pointer; font-size: 12px;
  opacity: 0.6; transition: opacity 0.2s;
}
.session-item:hover .session-delete { opacity: 1; }
@media (max-width: 767px) {
  .session-delete { opacity: 0.6; }
  .session-item:hover .session-delete { opacity: 1; }
}
.empty-sessions { text-align: center; color: var(--text-muted); padding: 40px 0; font-size: 13px; }

/* Main chat */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-left: none;
  border-radius: 0 var(--radius-lg) var(--radius-lg) 0;
  overflow: hidden;
}
.session-sidebar.collapsed + .chat-main {
  border-left: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
}
.chat-header {
  padding: 12px 20px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 12px;
}
.chat-title { flex: 1; font-size: 15px; font-weight: 600; color: var(--text-primary); }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}
.welcome-icon { margin-bottom: 16px; }
.welcome h3 { margin: 0 0 8px; font-size: 20px; color: var(--text-primary); font-weight: 700; }
.welcome p { margin: 0; font-size: 14px; }
.message { display: flex; width: 100%; }
.message.user { justify-content: flex-end; }
.msg-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
}
.user .msg-bubble {
  background: linear-gradient(135deg, #10b981, #059669);
  border: none;
  color: #fff;
}
.msg-role { font-size: 11px; font-weight: 600; opacity: 0.6; margin-bottom: 4px; text-transform: uppercase; }
.msg-content { font-size: 14px; line-height: 1.6; }
.msg-content :deep(p) { margin: 0 0 8px; }
.msg-content :deep(p:last-child) { margin: 0; }
.msg-content :deep(code) { background: rgba(0,0,0,0.06); padding: 2px 6px; border-radius: 4px; font-size: 13px; }
.msg-content :deep(pre) { background: #1e293b; color: #e2e8f0; padding: 12px; border-radius: 8px; overflow-x: auto; }

/* Source reference cards */
.msg-sources {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--border-color);
}
.sources-label {
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 8px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 5px;
}
.sources-label svg { opacity: 0.6; }
.source-card {
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  margin-bottom: 6px;
  overflow: hidden;
  transition: box-shadow 0.2s, transform 0.2s;
}
.source-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}
.source-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
}
.source-card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 800;
  flex-shrink: 0;
  letter-spacing: -0.5px;
}
.ft-pdf { background: #fef2f2; color: #dc2626; }
.ft-doc { background: #eff6ff; color: #2563eb; }
.ft-xls { background: #f0fdf4; color: #16a34a; }
.ft-ppt { background: #fff7ed; color: #ea580c; }
.ft-img { background: #faf5ff; color: #9333ea; }
.ft-vid { background: #fefce8; color: #ca8a04; }
.ft-aud { background: #fdf2f8; color: #db2777; }
.ft-default { background: #f1f5f9; color: #64748b; }
.source-card-info { flex: 1; min-width: 0; }
.source-card-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.source-card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}
.source-card-type {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 3px;
  background: var(--bg-secondary);
  color: var(--text-muted);
  font-weight: 600;
}
.source-card-modality {
  font-size: 10px;
  color: var(--text-muted);
  opacity: 0.7;
}
.source-card-excerpt {
  padding: 0 10px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-secondary, #64748b);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.source-card-excerpt :deep(mark) {
  background: #fef08a;
  color: #1e293b;
  padding: 0 2px;
  border-radius: 2px;
}
.source-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px 8px;
  gap: 8px;
}
.source-card-score {
  display: flex;
  align-items: center;
  gap: 6px;
}
.score-label {
  font-size: 10px;
  color: var(--text-muted);
  white-space: nowrap;
}
.score-value {
  font-size: 11px;
  font-weight: 700;
  min-width: 32px;
  text-align: right;
}
.source-card-actions {
  display: flex;
  gap: 2px;
}
.src-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 11px;
  transition: all 0.15s;
  white-space: nowrap;
}
.src-action-btn:hover {
  color: var(--accent);
  border-color: var(--accent);
  background: rgba(16, 185, 129, 0.06);
}
.src-action-btn svg { flex-shrink: 0; }

/* Source preview dialog */
.preview-dialog-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}
.preview-dialog-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 5px;
  font-size: 9px;
  font-weight: 800;
}
.preview-dialog-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-secondary, #475569);
  max-height: 400px;
  overflow-y: auto;
}
.preview-dialog-content :deep(p) { margin: 0 0 8px; }
.preview-dialog-content :deep(p:last-child) { margin: 0; }

.loading-bubble { min-width: 80px; }
.typing-dots { display: flex; gap: 4px; padding: 4px 0; }
.typing-dots span {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--text-muted);
  animation: bounce 1.4s infinite ease-in-out;
}
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}
.chat-input-area {
  padding: 16px 24px 16px;
  border-top: 1px solid var(--border-color);
}
.input-wrap {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 50px;
  padding: 6px 6px 6px 20px;
}
.input-wrap:focus-within { border-color: var(--accent); }
.chat-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  background: transparent;
  color: var(--text-primary);
  resize: none;
  overflow-y: auto;
  line-height: 1.5;
  padding: 5px 0;
  max-height: calc(5 * 1.5em);
}
.chat-input::placeholder { color: var(--text-muted); }
.send-btn {
  width: 40px; height: 40px;
  border-radius: 50%;
  border: none;
  background: var(--accent);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  flex-shrink: 0;
}
.send-btn:hover { background: var(--accent-hover); }
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.input-hint {
  text-align: center;
  font-size: 11px;
  color: var(--text-muted);
  margin: 8px 0 0;
}
</style>
