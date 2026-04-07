<template>
  <div class="discovery-container">
    <div class="header glass-card">
      <h3 class="primary-gradient-text">探索知识宇宙 <span>🚀</span></h3>
      <p class="subtitle">深度学习与多模态 RAG 技术前沿发现</p>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="10" animated />
    </div>

    <div v-else class="discovery-sections">
      <!-- Section 1: Featured / System Knowledge -->
      <div v-if="discoveryData.featured?.length" class="discovery-row">
        <div class="section-title">
          <el-icon><Star /></el-icon>
          <span>精选系统知识</span>
        </div>
        <div class="card-grid">
            <div v-for="doc in discoveryData.featured" :key="doc.id" class="recommend-card glass-card">
              <div class="card-badge featured">精选</div>
              <div class="card-click-area" @click="handleDocClick(doc)">
                <div class="card-icon">{{ getModalityIcon(doc.fileType) }}</div>
                <h4 class="doc-title">{{ doc.fileName }}</h4>
                <div class="doc-preview">{{ getShortPreview(doc.extractedContent) }}</div>
              </div>
              <div v-if="canDelete(doc)" class="card-actions">
                <el-popconfirm title="确定删除此文档吗？" @confirm="handleDelete(doc.id)">
                  <template #reference>
                    <el-button type="danger" link :icon="Delete" />
                  </template>
                </el-popconfirm>
              </div>
            </div>
        </div>
      </div>

      <!-- Section 2: Collaborative Filtering (People like you also viewed) -->
      <div v-if="discoveryData.collaborative?.length" class="discovery-row">
        <div class="section-title" style="border-left-color: #8b5cf6;">
          <el-icon><User /></el-icon>
          <span>大家都在看（协同推荐）</span>
        </div>
        <div class="card-grid">
          <div v-for="doc in discoveryData.collaborative" :key="doc.id" class="recommend-card glass-card">
            <div class="card-click-area" @click="handleDocClick(doc)">
              <div class="card-icon">{{ getModalityIcon(doc.fileType) }}</div>
              <h4 class="doc-title">{{ doc.fileName }}</h4>
              <div class="doc-preview">{{ getShortPreview(doc.extractedContent) }}</div>
            </div>
            <div v-if="canDelete(doc)" class="card-actions">
              <el-popconfirm title="确定删除此文档吗？" @confirm="handleDelete(doc.id)">
                <template #reference>
                  <el-button type="danger" link :icon="Delete" />
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>
      </div>

      <!-- Section 3: Semantic Recommend (Guess You Like) -->
      <div v-if="discoveryData.guessYouLike?.length" class="discovery-row">
        <div class="section-title">
          <el-icon><MagicStick /></el-icon>
          <span>智能语义推荐</span>
        </div>
        <div class="card-grid">
          <div v-for="doc in discoveryData.guessYouLike" :key="doc.id" class="recommend-card glass-card">
            <div class="card-click-area" @click="handleDocClick(doc)">
              <div class="card-icon">{{ getModalityIcon(doc.fileType) }}</div>
              <h4 class="doc-title">{{ doc.fileName }}</h4>
              <div class="doc-preview">{{ getShortPreview(doc.extractedContent) }}</div>
            </div>
            <div v-if="canDelete(doc)" class="card-actions">
              <el-popconfirm title="确定删除此文档吗？" @confirm="handleDelete(doc.id)">
                <template #reference>
                  <el-button type="danger" link :icon="Delete" />
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>
      </div>

      <!-- Section 3: New Arrivals -->
      <div v-if="discoveryData.newArrivals?.length" class="discovery-row">
        <div class="section-title">
          <el-icon><Download /></el-icon>
          <span>全网新鲜加入</span>
        </div>
        <div class="card-grid">
          <div v-for="doc in discoveryData.newArrivals" :key="doc.id" class="recommend-card glass-card">
            <div class="card-badge new">最新</div>
            <div class="card-click-area" @click="handleDocClick(doc)">
              <div class="card-icon">{{ getModalityIcon(doc.fileType) }}</div>
              <h4 class="doc-title">{{ doc.fileName }}</h4>
              <div class="doc-preview">{{ getShortPreview(doc.extractedContent) }}</div>
            </div>
            <div v-if="canDelete(doc)" class="card-actions">
              <el-popconfirm title="确定删除此文档吗？" @confirm="handleDelete(doc.id)">
                <template #reference>
                  <el-button type="danger" link :icon="Delete" />
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>
      </div>

      <!-- Section 4: Your Recent Uploads -->
      <div v-if="discoveryData.recentDocuments?.length" class="discovery-row">
        <div class="section-title" style="border-left-color: #10b981;">
          <el-icon><DocIcon /></el-icon>
          <span>您最近的上传</span>
        </div>
        <div class="card-grid">
          <div v-for="doc in discoveryData.recentDocuments" :key="doc.id" class="recommend-card glass-card">
            <div class="card-badge new">您的</div>
            <div class="card-click-area" @click="handleDocClick(doc)">
              <div class="card-icon">{{ getModalityIcon(doc.fileType) }}</div>
              <h4 class="doc-title">{{ doc.fileName }}</h4>
              <div class="doc-preview">{{ getShortPreview(doc.extractedContent) }}</div>
            </div>
            <div v-if="canDelete(doc)" class="card-actions">
              <el-popconfirm title="确定删除此文档吗？" @confirm="handleDelete(doc.id)">
                <template #reference>
                  <el-button type="danger" link :icon="Delete" />
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && !hasAnyData" class="empty-state">
      <el-empty description="暂时没有发现新知识" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import api from '../api'
import { Star, MagicStick, Download, User, Delete, Document as DocIcon } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface Recommendation {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  extractedContent: string;
  isPublic: boolean;
  userId?: number;
}

interface DiscoveryData {
  featured: Recommendation[];
  guessYouLike: Recommendation[];
  collaborative: Recommendation[];
  newArrivals: Recommendation[];
  recentDocuments: Recommendation[];
}

const discoveryData = ref<DiscoveryData>({
  featured: [],
  guessYouLike: [],
  collaborative: [],
  newArrivals: [],
  recentDocuments: []
})
const loading = ref(false)

const hasAnyData = computed(() => {
  return (discoveryData.value.featured?.length || 0) > 0 || 
         (discoveryData.value.guessYouLike?.length || 0) > 0 || 
         (discoveryData.value.collaborative?.length || 0) > 0 ||
         (discoveryData.value.newArrivals?.length || 0) > 0 ||
         (discoveryData.value.recentDocuments?.length || 0) > 0
})

const fetchDiscovery = async () => {
  loading.value = true
  try {
    const res = await api.get('/recommendations/discovery?limit=6')
    discoveryData.value = res.data
  } catch (err: any) {
    console.error('Failed to fetch discovery', err)
    ElMessage.error(err.response?.data?.message || '获取推荐数据失败')
  } finally {
    loading.value = false
  }
}

const getModalityIcon = (type: string) => {
  const t = type?.toLowerCase() || ''
  if (['png', 'jpg', 'jpeg'].includes(t)) return '🖼️'
  if (['mp4', 'avi'].includes(t)) return '🎬'
  if (['mp3', 'wav'].includes(t)) return '🎵'
  if (['pdf'].includes(t)) return '📕'
  return '📄'
}

const getShortPreview = (content: string) => {
  if (!content) return '暂无预览内容'
  const clean = content.replace(/\[.*?\]/g, '').trim()
  return clean.length > 60 ? clean.substring(0, 60) + '...' : clean
}

const canDelete = (doc: Recommendation) => {
  const userInfoStr = localStorage.getItem('user_info')
  if (!userInfoStr) return false
  try {
    const userInfo = JSON.parse(userInfoStr)
    return userInfo && doc.userId === userInfo.id
  } catch (e) {
    return false
  }
}

const handleDelete = async (id: number) => {
  try {
    await api.delete(`/knowledge/${id}`)
    ElMessage.success('删除成功')
    fetchDiscovery()
  } catch (err) {
    ElMessage.error('删除失败')
  }
}

const handleDocClick = (doc: Recommendation) => {
  window.open(`/api/knowledge/view/${doc.id}`, '_blank')
}

onMounted(fetchDiscovery)
</script>

<style scoped>
.discovery-container {
  padding: 30px;
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.header {
  margin-bottom: 40px;
  padding: 20px 30px;
  border-radius: 16px;
  background: rgba(30, 41, 59, 0.5);
}

.header h3 {
  font-size: 1.8rem;
  margin: 0 0 10px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.subtitle {
  color: #94a3b8;
  font-size: 1rem;
}

.discovery-row {
  margin-bottom: 40px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.2rem;
  font-weight: bold;
  color: #e2e8f0;
  margin-bottom: 20px;
  border-left: 4px solid #3b82f6;
  padding-left: 15px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.recommend-card {
  position: relative;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: rgba(30, 41, 59, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.05);
  height: 220px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 12px;
}

.recommend-card:hover {
  transform: translateY(-8px) scale(1.02);
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(96, 165, 250, 0.4);
  box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.5);
}

.card-badge {
  position: absolute;
  top: 15px;
  right: 15px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.7rem;
  font-weight: bold;
  text-transform: uppercase;
  z-index: 10;
}

.card-badge.featured {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
}

.card-badge.new {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.card-icon {
  font-size: 2.5rem;
  margin-bottom: 15px;
}

.doc-title {
  font-size: 1.1rem;
  color: #f1f5f9;
  margin: 0 0 10px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doc-preview {
  font-size: 0.85rem;
  color: #94a3b8;
  line-height: 1.6;
  height: 4.8em;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.recommend-card:hover .card-actions {
  opacity: 1;
}

.card-actions {
  position: absolute;
  bottom: 15px;
  right: 15px;
  opacity: 0;
  transition: opacity 0.3s;
  display: flex;
  gap: 8px;
  z-index: 20;
}

.card-click-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.loading-state, .empty-state {
  padding: 100px 0;
  display: flex;
  justify-content: center;
}
</style>
