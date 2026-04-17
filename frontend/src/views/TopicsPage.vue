<template>
  <div class="topics-page">
    <PageHeader badge="话题广场" title="发现话题" subtitle="探索感兴趣的话题，发现更多知识" />

    <!-- Search Bar -->
    <div class="search-section">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索话题..."
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
        size="large"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="skeleton-grid">
      <el-skeleton animated :count="9" :throttle="0">
        <template #template>
          <div class="skeleton-card">
            <div class="skeleton-header">
              <el-skeleton-item variant="text" style="width: 60px; height: 22px;" />
              <el-skeleton-item variant="text" style="width: 80px; height: 16px;" />
            </div>
            <el-skeleton-item variant="h3" style="width: 70%; height: 24px; margin-bottom: 12px;" />
            <el-skeleton-item variant="text" style="width: 100%; height: 14px; margin-bottom: 6px;" />
            <el-skeleton-item variant="text" style="width: 85%; height: 14px; margin-bottom: 16px;" />
            <div class="skeleton-footer">
              <el-skeleton-item variant="text" style="width: 60px; height: 14px;" />
              <el-skeleton-item variant="text" style="width: 50px; height: 14px;" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>

    <!-- Topics Grid -->
    <div v-else-if="topics.length > 0" class="topics-grid">
      <TopicCard
        v-for="topic in topics"
        :key="topic.id"
        :topic="topic"
        @click="goToTopic(topic)"
      />
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--border-color)" stroke-width="1.5">
        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
      </svg>
      <p>{{ searchKeyword ? '未找到匹配的话题' : '暂无公开话题' }}</p>
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

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import api from '../api'
import PageHeader from '../components/PageHeader.vue'
import TopicCard from '../components/TopicCard.vue'

const router = useRouter()

const topics = ref<any[]>([])
const searchKeyword = ref('')
const loading = ref(false)
const totalCount = ref(0)
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = 12

const loadPage = async (page: number, keyword?: string) => {
  loading.value = true
  try {
    const res = await api.get('/topics/public/paged', {
      params: {
        page,
        size: pageSize,
        keyword: keyword ?? searchKeyword.value
      }
    })
    topics.value = res.data.content || []
    totalCount.value = res.data.totalElements
    totalPages.value = res.data.totalPages
    currentPage.value = res.data.number
  } catch (e) {
    console.error('获取话题列表失败:', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 0
  loadPage(0)
}

const goToTopic = (topic: any) => {
  router.push(`/topic/${topic.id}`)
}

onMounted(() => {
  loadPage(0)
})
</script>

<style scoped>
.topics-page {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 24px;
}

.search-section {
  margin-bottom: 32px;
}

.search-section :deep(.el-input__wrapper) {
  border-radius: 24px;
  padding: 10px 20px;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.skeleton-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px;
  min-height: 160px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.skeleton-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.skeleton-footer {
  display: flex;
  gap: 16px;
  margin-top: auto;
}

.topics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.empty-state {
  text-align: center;
  color: var(--text-muted);
  padding: 80px 0;
}

.empty-state svg {
  margin-bottom: 20px;
}

.empty-state p {
  margin: 0;
  font-size: 16px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding-bottom: 20px;
}

@media (max-width: 1200px) {
  .topics-grid,
  .skeleton-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .topics-grid,
  .skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .topics-grid,
  .skeleton-grid {
    grid-template-columns: 1fr;
  }

  .topics-page {
    padding: 0 16px;
  }
}
</style>
