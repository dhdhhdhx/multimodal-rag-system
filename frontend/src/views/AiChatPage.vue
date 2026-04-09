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
              <!-- Source references -->
              <div v-if="msg.sources && msg.sources.length" class="msg-sources">
                <div class="sources-label">参考来源：</div>
                <div v-for="(src, j) in msg.sources" :key="j" class="source-item"
                  @click="viewSource(src)">
                  <el-icon><Document /></el-icon>
                  <span class="source-name">{{ src.fileName }}</span>
                  <span class="source-excerpt" v-html="highlightExcerpt(src.excerpt, msg)"></span>
                </div>
              </div>
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
import { ArrowLeft, Delete, Close, ChatLineSquare, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import api from '../api'

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

const viewSource = (src: any) => {
  if (src.docId) {
    // Navigate to article detail page
    window.open(`/article/${src.docId}`, '_blank')
  }
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

/* Source references */
.msg-sources {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--border-color);
}
.sources-label {
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 6px;
  font-weight: 600;
}
.source-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 6px;
  background: rgba(16, 185, 129, 0.06);
  margin-bottom: 4px;
  cursor: pointer;
  transition: background 0.2s;
  font-size: 12px;
}
.source-item:hover { background: rgba(16, 185, 129, 0.12); }
.source-name { font-weight: 600; color: var(--accent); white-space: nowrap; }
.source-excerpt {
  color: var(--text-muted); font-size: 11px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  flex: 1;
}
.source-excerpt :deep(mark) {
  background: #fef08a;
  color: #1e293b;
  padding: 0 2px;
  border-radius: 2px;
}

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
