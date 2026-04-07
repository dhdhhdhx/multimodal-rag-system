<template>
  <div class="search-page">
    <PageHeader badge="全文检索" title="搜索知识库" subtitle="在所有文章的标题、摘要和正文中搜索关键词" />
    <div class="search-bar">
      <div class="search-input-wrap">
        <el-icon class="search-icon"><Search /></el-icon>
        <input v-model="keyword" class="search-input" placeholder="输入关键词搜索..."
          @keyup.enter="doSearch" />
        <el-button type="primary" class="search-btn" @click="doSearch">搜索</el-button>
      </div>
    </div>
    <div v-if="searched" class="results-info">
      <span>找到 <strong>{{ totalCount }}</strong> 篇相关文章</span>
      <span class="keyword-hint">关键词：{{ searchedKeyword }}</span>
    </div>
    <div class="article-grid">
      <ArticleCard v-for="doc in documents" :key="doc.id" :doc="doc"
        @click="viewDoc(doc)" />
    </div>
    <div v-if="searched && documents.length === 0" class="empty-state">
      未找到相关文章
    </div>
    <div v-if="totalPages > 1" class="pagination-wrap">
      <el-pagination background layout="prev, pager, next"
        :total="totalCount" :page-size="9"
        :current-page="currentPage + 1"
        @current-change="(p: number) => loadResults(p - 1)" />
    </div>

    <!-- Recommended section -->
    <div v-if="hotDocs.length" class="recommend-section">
      <h3 class="section-title">热门推荐</h3>
      <div class="article-grid">
        <ArticleCard v-for="doc in hotDocs" :key="'hot-' + doc.id" :doc="doc"
          @click="viewDoc(doc)" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import api from '../api'
import PageHeader from '../components/PageHeader.vue'
import ArticleCard from '../components/ArticleCard.vue'

const router = useRouter()
const keyword = ref('')
const searchedKeyword = ref('')
const searched = ref(false)
const documents = ref<any[]>([])
const totalCount = ref(0)
const totalPages = ref(0)
const currentPage = ref(0)
const hotDocs = ref<any[]>([])

const doSearch = () => {
  if (!keyword.value.trim()) return
  searchedKeyword.value = keyword.value.trim()
  searched.value = true
  loadResults(0)
}

const loadResults = async (page: number) => {
  try {
    const res = await api.get('/knowledge/public', {
      params: { keyword: searchedKeyword.value, page, size: 9 }
    })
    documents.value = res.data.content
    totalCount.value = res.data.totalElements
    totalPages.value = res.data.totalPages
    currentPage.value = res.data.number
  } catch (e) { console.error(e) }
}

const viewDoc = (doc: any) => {
  router.push(`/article/${doc.id}`)
}

const loadHot = async () => {
  try {
    const res = await api.get('/knowledge/public/hot', { params: { limit: 6 } })
    hotDocs.value = res.data
  } catch { /* ignore */ }
}

onMounted(loadHot)
</script>

<style scoped>
.search-bar { margin-bottom: 24px; }
.search-input-wrap {
  display: flex;
  align-items: center;
  background: var(--bg-card);
  border: 2px solid var(--border-color);
  border-radius: 50px;
  padding: 6px 6px 6px 20px;
  transition: border-color 0.2s;
}
.search-input-wrap:focus-within { border-color: var(--accent); }
.search-icon { font-size: 18px; color: var(--text-muted); margin-right: 12px; }
.search-input {
  flex: 1; border: none; outline: none; font-size: 15px;
  color: var(--text-primary); background: transparent;
}
.search-input::placeholder { color: var(--text-muted); }
.search-btn {
  border-radius: 50px !important; padding: 10px 28px !important;
  font-size: 14px !important; font-weight: 600 !important;
  background: var(--accent) !important; border-color: var(--accent) !important;
}
.results-info {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px; font-size: 13px; color: var(--text-secondary);
}
.keyword-hint { color: var(--text-muted); }
.article-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.empty-state { text-align: center; color: var(--text-muted); padding: 80px 0; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 40px; }
.recommend-section { margin-top: 60px; padding-top: 40px; border-top: 1px solid var(--border-color); }
.section-title { font-size: 20px; font-weight: 700; color: var(--text-primary); margin: 0 0 20px; }
@media (max-width: 900px) { .article-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 600px) { .article-grid { grid-template-columns: 1fr; } }
</style>
