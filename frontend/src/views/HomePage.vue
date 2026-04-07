<template>
  <div class="home-page">
    <PageHeader badge="知识库" title="最新文章" subtitle="探索知识节点，构建你的第二大脑"
      :rightValue="String(totalCount)" rightLabel="文章总数" />
    <div class="article-grid">
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
}
@media (max-width: 600px) {
  .article-grid { grid-template-columns: 1fr; }
}
</style>
