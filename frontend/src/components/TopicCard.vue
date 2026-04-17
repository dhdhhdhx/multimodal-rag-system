<template>
  <div class="topic-card" @click="$emit('click', topic)">
    <div class="card-header">
      <span class="topic-badge">话题</span>
      <span class="subscriber-count">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
          <circle cx="9" cy="7" r="4"/>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
        </svg>
        {{ topic.subscriberCount || 0 }}
      </span>
    </div>
    <h3 class="topic-name">{{ topic.name }}</h3>
    <p v-if="topic.description" class="topic-desc">{{ truncate(topic.description, 80) }}</p>
    <p v-else class="topic-desc topic-desc--empty">暂无描述</p>
    <div class="card-footer">
      <span class="doc-count">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
        </svg>
        {{ topic.documentCount || 0 }} 篇
      </span>
      <span class="owner-name">by {{ topic.ownerName || '未知用户' }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ topic: any }>()
defineEmits(['click'])

const truncate = (text: string, len: number) => {
  if (!text) return ''
  return text.length > len ? text.substring(0, len) + '...' : text
}
</script>

<style scoped>
.topic-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  min-height: 160px;
}

.topic-card:hover {
  border-color: var(--accent);
  box-shadow: 0 4px 20px rgba(16, 185, 129, 0.15);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.topic-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  background: var(--accent);
  color: #fff;
  font-size: 11px;
  font-weight: 500;
  border-radius: 12px;
}

.subscriber-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--text-muted);
  font-size: 12px;
}

.topic-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
  line-height: 1.4;
}

.topic-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0 0 auto;
  flex: 1;
}

.topic-desc--empty {
  color: var(--text-muted);
  font-style: italic;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.doc-count,
.owner-name {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.doc-count svg,
.owner-name svg {
  flex-shrink: 0;
}
</style>
