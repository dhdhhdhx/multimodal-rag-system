<template>
  <div class="topic-detail">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="0" animated>
        <template #template>
          <div class="skeleton-wrapper">
            <el-skeleton-item variant="h1" style="width: 50%; height: 32px; margin-bottom: 16px;" />
            <div style="display: flex; gap: 24px; margin-bottom: 32px;">
              <el-skeleton-item variant="text" style="width: 120px;" />
              <el-skeleton-item variant="text" style="width: 90px;" />
              <el-skeleton-item variant="text" style="width: 60px;" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>

    <template v-else-if="topic">
      <!-- Topic Header -->
      <div class="topic-header">
        <router-link to="/" class="back-btn">
          <el-icon><ArrowLeft /></el-icon> 返回首页
        </router-link>

        <h1 class="topic-title">
          <span class="topic-badge">话题</span>
          {{ topic.name }}
        </h1>

        <p v-if="topic.description" class="topic-description">{{ topic.description }}</p>

        <div class="topic-meta">
          <span class="meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            创建者: {{ topic.ownerName || '未知用户' }}
          </span>
          <span class="meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
            {{ topic.documentCount || 0 }} 篇文档
          </span>
          <span class="meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            {{ topic.subscriberCount || 0 }} 位订阅者
          </span>
          <span class="meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            {{ formatTime(topic.createdAt) }}
          </span>
        </div>

        <div class="topic-actions">
          <el-button
            v-if="isLoggedIn"
            :type="topic.subscribed ? 'default' : 'primary'"
            @click="toggleSubscribe"
            :loading="subscribeLoading"
          >
            {{ topic.subscribed ? '已订阅' : '订阅话题' }}
          </el-button>
          <el-button v-else @click="goToLogin">登录后可订阅</el-button>
        </div>
      </div>

      <!-- Documents Section -->
      <div class="documents-section">
        <h2 class="section-title">话题文档</h2>

        <!-- Search Bar -->
        <div class="search-bar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文档..."
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div v-if="!loading && documents.length === 0" class="empty-state">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--border-color)" stroke-width="1.5">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
          </svg>
          <p>{{ searchKeyword ? '未找到匹配的文档' : '该话题暂无文档' }}</p>
        </div>

        <div v-else class="article-grid">
          <ArticleCard
            v-for="doc in documents"
            :key="doc.id"
            :doc="doc"
            @click="viewDoc(doc)"
          />
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="pagination-wrap">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="totalCount"
            :page-size="pageSize"
            :current-page="currentPage + 1"
            @current-change="(p: number) => loadPage(p - 1)"
          />
        </div>
      </div>
    </template>

    <div v-else class="error-state">话题不存在或无权查看</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api, { getAccessToken } from '../api'
import ArticleCard from '../components/ArticleCard.vue'

const route = useRoute()
const router = useRouter()

const topic = ref<any>(null)
const documents = ref<any[]>([])
const loading = ref(true)
const subscribeLoading = ref(false)
const searchKeyword = ref('')
const totalCount = ref(0)
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = 9

const isLoggedIn = computed(() => !!getAccessToken())

const formatTime = (t: string) => {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN')
}

const fetchTopicDetail = async () => {
  try {
    const res = await api.get(`/topics/${route.params.id}`)
    topic.value = res.data
  } catch (e) {
    console.error('获取话题详情失败:', e)
  }
}

const loadPage = async (page: number, keyword?: string) => {
  try {
    const res = await api.get(`/topics/${route.params.id}/documents/paged`, {
      params: {
        page,
        size: pageSize,
        keyword: keyword ?? searchKeyword.value
      }
    })
    documents.value = res.data.content || []
    totalCount.value = res.data.totalElements
    totalPages.value = res.data.totalPages
    currentPage.value = res.data.number
  } catch (e) {
    console.error('获取话题文档失败:', e)
  }
}

const handleSearch = () => {
  currentPage.value = 0
  loadPage(0)
}

const toggleSubscribe = async () => {
  if (!topic.value) return

  subscribeLoading.value = true
  try {
    const topicId = route.params.id as string

    if (topic.value.subscribed) {
      // 取消订阅
      await api.delete(`/topics/${topicId}/subscribe`)
      topic.value.subscribed = false
      topic.value.subscriberCount = Math.max(0, (topic.value.subscriberCount || 0) - 1)
      ElMessage.success('已取消订阅')
    } else {
      // 订阅
      const res = await api.post(`/topics/${topicId}/subscribe`)
      topic.value.subscribed = true
      topic.value.subscriberCount = res.data.subscriberCount
      ElMessage.success('订阅成功')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    subscribeLoading.value = false
  }
}

const viewDoc = (doc: any) => {
  router.push(`/article/${doc.id}`)
}

const goToLogin = () => {
  router.push('/login')
}

onMounted(async () => {
  loading.value = true
  await Promise.all([fetchTopicDetail(), loadPage(0)])
  loading.value = false
})
</script>

<style scoped>
.topic-detail {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px;
}

.loading-state {
  padding: 40px 0;
}

.skeleton-wrapper {
  padding: 20px;
}

.topic-header {
  margin-bottom: 40px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  margin-bottom: 20px;
  transition: color 0.15s;
}

.back-btn:hover {
  color: var(--accent);
}

.topic-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.topic-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  background: var(--accent);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  border-radius: 20px;
}

.topic-description {
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 20px;
}

.topic-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 24px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: 13px;
}

.meta-item svg {
  flex-shrink: 0;
}

.topic-actions {
  display: flex;
  gap: 12px;
}

.documents-section {
  border-top: 1px solid var(--border-color);
  padding-top: 32px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 24px;
}

.search-bar {
  margin-bottom: 24px;
}

.search-bar :deep(.el-input__wrapper) {
  border-radius: 24px;
  padding: 8px 16px;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

.empty-state {
  text-align: center;
  color: var(--text-muted);
  padding: 60px 0;
}

.empty-state svg {
  margin-bottom: 16px;
}

.empty-state p {
  margin: 0;
  font-size: 15px;
}

.error-state {
  text-align: center;
  color: var(--text-muted);
  padding: 80px 0;
  font-size: 16px;
}

@media (max-width: 900px) {
  .topic-detail {
    padding: 24px 16px;
  }

  .article-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .article-grid {
    grid-template-columns: 1fr;
  }

  .topic-title {
    font-size: 22px;
  }

  .topic-meta {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
