<template>
  <div class="tags-page">
    <PageHeader badge="标签分类" title="标签归档" subtitle="按标签浏览知识库中的所有文章"
      :rightValue="String(tags.length)" rightLabel="标签总数" />
    <div class="tags-layout">
      <aside class="tag-cloud-panel">
        <h4>所有标签</h4>
        <div class="tag-list">
          <span v-for="tag in tags" :key="tag.name"
            :class="['tag-item', { active: selectedTag === tag.name }]"
            @click="selectTag(tag.name)">
            {{ tag.name }} <span class="tag-count">{{ tag.count }}</span>
          </span>
        </div>
      </aside>
      <div class="tag-articles">
        <div v-if="selectedTag" class="tag-title-row">
          <h3>标签：{{ selectedTag }}</h3>
          <span class="article-count">{{ totalCount }} 篇文章</span>
        </div>
        <div class="article-grid">
          <ArticleCard v-for="doc in documents" :key="doc.id" :doc="doc"
            @click="viewDoc(doc)" />
        </div>
        <div v-if="documents.length === 0 && selectedTag" class="empty-state">
          该标签下暂无文章
        </div>
        <div v-if="totalPages > 1" class="pagination-wrap">
          <el-pagination background layout="prev, pager, next"
            :total="totalCount" :page-size="6"
            :current-page="currentPage + 1"
            @current-change="(p: number) => loadArticles(p - 1)" />
        </div>
      </div>
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
const tags = ref<any[]>([])
const selectedTag = ref('')
const documents = ref<any[]>([])
const totalCount = ref(0)
const totalPages = ref(0)
const currentPage = ref(0)

const loadTags = async () => {
  try {
    const res = await api.get('/knowledge/public/tags')
    tags.value = res.data
    if (tags.value.length > 0) {
      selectTag(tags.value[0].name)
    }
  } catch (e) { console.error(e) }
}

const selectTag = (name: string) => {
  selectedTag.value = name
  loadArticles(0)
}

const loadArticles = async (page: number) => {
  try {
    const res = await api.get('/knowledge/public', { params: { tag: selectedTag.value, page, size: 6 } })
    documents.value = res.data.content
    totalCount.value = res.data.totalElements
    totalPages.value = res.data.totalPages
    currentPage.value = res.data.number
  } catch (e) { console.error(e) }
}

const viewDoc = (doc: any) => {
  router.push(`/article/${doc.id}`)
}

onMounted(loadTags)
</script>

<style scoped>
.tags-layout {
  display: flex;
  gap: 32px;
}
.tag-cloud-panel {
  width: 280px;
  flex-shrink: 0;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
  align-self: flex-start;
  position: sticky;
  top: 100px;
}
.tag-cloud-panel h4 {
  margin: 0 0 16px 0;
  font-size: 15px;
  color: var(--text-primary);
}
.tag-list { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  border: 1px solid var(--border-color);
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}
.tag-item:hover { border-color: var(--accent); color: var(--accent); }
.tag-item.active {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}
.tag-item.active .tag-count { background: rgba(255,255,255,0.3); color: #fff; }
.tag-count {
  font-size: 11px;
  background: var(--bg-secondary);
  padding: 1px 6px;
  border-radius: 10px;
  color: var(--text-muted);
}
.tag-articles { flex: 1; min-width: 0; }
.tag-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.tag-title-row h3 { margin: 0; font-size: 20px; color: var(--text-primary); }
.article-count { font-size: 13px; color: var(--accent); }
.article-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}
.empty-state {
  text-align: center;
  color: var(--text-muted);
  padding: 60px 0;
}
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
@media (max-width: 768px) {
  .tags-layout { flex-direction: column; }
  .tag-cloud-panel { width: 100%; position: static; }
  .article-grid { grid-template-columns: 1fr; }
}
</style>
