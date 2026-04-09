<template>
  <div class="article-card" role="button" tabindex="0"
    @click="$emit('click', doc)" @keydown.enter="$emit('click', doc)">
    <div class="card-meta">
      <span class="tag-badge">{{ primaryTag }}</span>
      <span class="date">{{ formatDate(doc.uploadTime) }}</span>
    </div>
    <h3 class="card-title">{{ doc.fileName }}</h3>
    <p class="card-excerpt">{{ truncate(doc.extractedContent, 120) }}</p>
    <div class="card-footer">
      <span class="view-count">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
          <circle cx="12" cy="12" r="3"/>
        </svg>
        {{ doc.viewCount || 0 }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ doc: any }>()
defineEmits(['click'])

const primaryTag = computed(() => {
  const tags = props.doc.tags
  if (!tags) return '技术笔记'
  return tags.split(',')[0].trim() || '技术笔记'
})

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

const truncate = (text: string, len: number) => {
  if (!text) return '暂无内容摘要'
  const clean = text.replace(/\[.*?\]\s*/g, '').replace(/<[^>]*>/g, '')
  return clean.length > len ? clean.substring(0, len) + '...' : clean
}
</script>

<style scoped>
.article-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  outline: none;
}
.article-card:hover, .article-card:focus-visible {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.article-card:focus-visible {
  border-color: var(--accent);
}
.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.tag-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 4px;
  background: var(--accent-light);
  color: var(--accent);
}
.date { font-size: 12px; color: var(--text-muted); }
.card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-excerpt {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 16px 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-footer { display: flex; align-items: center; }
.view-count {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
