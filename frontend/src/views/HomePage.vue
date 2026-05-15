<template>
  <div class="home-page">
    <PageHeader badge="知识库" title="最新文章" subtitle="探索知识节点，构建你的第二大脑"
      :rightValue="String(totalCount)" rightLabel="文章总数" />
    <div v-if="loading" class="skeleton-grid">
      <el-skeleton animated :count="9" :throttle="0">
        <template #template>
          <div class="skeleton-card">
            <div class="skeleton-meta">
              <el-skeleton-item variant="text" style="width: 72px; height: 22px;" />
              <el-skeleton-item variant="text" style="width: 96px; height: 16px;" />
            </div>
            <el-skeleton-item variant="h3" style="width: 80%; height: 22px; margin-bottom: 8px;" />
            <el-skeleton-item variant="text" style="width: 100%; height: 14px; margin-bottom: 4px;" />
            <el-skeleton-item variant="text" style="width: 100%; height: 14px; margin-bottom: 4px;" />
            <el-skeleton-item variant="text" style="width: 60%; height: 14px; margin-bottom: 16px;" />
            <div class="skeleton-footer">
              <el-skeleton-item variant="text" style="width: 48px; height: 14px;" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>
    <div v-else class="article-grid">
      <ArticleCard v-for="doc in documents" :key="doc.id" :doc="doc"
        @click="viewDoc(doc)" />
    </div>
    <div v-if="documents.length === 0 && !loading" class="empty-state">
      暂无公开文章
    </div>
    <div v-if="totalPages > 1" class="pagination-wrap">
      <el-pagination background layout="prev, pager, next"
        :total="totalCount" :page-size="pageSize"
        :current-page="currentPage + 1"
        @current-change="(p: number) => loadPage(p - 1)" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import PageHeader from '../components/PageHeader.vue'
import ArticleCard from '../components/ArticleCard.vue'

const router = useRouter()
const documents = ref<any[]>([])
const totalCount = ref(0)
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = 9
const loading = ref(false)

const loadPage = async (page: number) => {
  loading.value = true
  try {
    const res = await api.get('/knowledge/public', { params: { page, size: pageSize } })
    documents.value = res.data.content
    totalCount.value = res.data.totalElements
    totalPages.value = res.data.totalPages
    currentPage.value = res.data.number
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const viewDoc = (doc: any) => {
  router.push(`/article/${doc.id}`)
}

onMounted(() => loadPage(0))
</script>

<style scoped>
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.skeleton-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
  min-height: 180px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.skeleton-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.skeleton-footer {
  display: flex;
  align-items: center;
}
.article-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.empty-state {
  text-align: center;
  color: var(--text-muted);
  padding: 80px 0;
  font-size: 15px;
}
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}
@media (max-width: 900px) {
  .article-grid { grid-template-columns: repeat(2, 1fr); }
  .skeleton-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 600px) {
  .article-grid { grid-template-columns: 1fr; }
  .skeleton-grid { grid-template-columns: 1fr; }
}
</style>
