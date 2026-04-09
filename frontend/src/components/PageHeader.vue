<template>
  <div class="page-header">
    <div class="header-left">
      <span class="badge" :style="{ background: badgeColor || 'var(--accent-light)', color: badgeTextColor || 'var(--accent)' }">
        <span class="badge-dot"></span> {{ badge }}
      </span>
      <h1 class="header-title">{{ title }}</h1>
      <p v-if="subtitle" class="header-subtitle">{{ subtitle }}</p>
    </div>
    <div v-if="rightValue" class="header-right">
      <span class="right-value">{{ rightValue }}</span>
      <span class="right-label">{{ rightLabel }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  badge: string
  title: string
  subtitle?: string
  badgeColor?: string
  badgeTextColor?: string
  rightValue?: string | number
  rightLabel?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
}
.badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
  margin-bottom: 12px;
}
.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  display: inline-block;
}
.header-title {
  font-size: 32px;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  line-height: 1.2;
}
.header-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}
.header-right {
  text-align: right;
  flex-shrink: 0;
}
.right-value {
  display: block;
  font-size: 36px;
  font-weight: 800;
  color: var(--accent);
  line-height: 1;
}
.right-label {
  font-size: 13px;
  color: var(--text-muted);
}
</style>
