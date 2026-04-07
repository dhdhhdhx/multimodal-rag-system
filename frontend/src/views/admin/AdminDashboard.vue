<template>
  <div class="admin-dashboard">
    <h2 class="page-title">仪表盘</h2>
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon users-icon"><el-icon :size="24"><User /></el-icon></div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.users?.total || 0 }}</div>
          <div class="stat-label">注册用户</div>
          <div class="stat-detail">活跃 {{ stats.users?.active || 0 }} / 禁用 {{ stats.users?.inactive || 0 }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon docs-icon"><el-icon :size="24"><Document /></el-icon></div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.documents?.total || 0 }}</div>
          <div class="stat-label">知识文档</div>
          <div class="stat-detail">公开 {{ stats.documents?.public || 0 }} / 处理中 {{ stats.documents?.processing || 0 }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon query-icon"><el-icon :size="24"><ChatDotRound /></el-icon></div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.queries?.total || 0 }}</div>
          <div class="stat-label">AI 问答</div>
          <div class="stat-detail">本周 {{ stats.queries?.thisWeek || 0 }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon access-icon"><el-icon :size="24"><View /></el-icon></div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.accesses?.total || 0 }}</div>
          <div class="stat-label">文档访问</div>
        </div>
      </div>
    </div>
    <div class="recent-section">
      <h3>最近查询</h3>
      <el-table :data="recentQueries" stripe size="small" max-height="400">
        <el-table-column prop="queryText" label="查询内容" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { User, Document, ChatDotRound, View } from '@element-plus/icons-vue'
import api from '../../api'

const stats = ref<any>({})
const recentQueries = ref<any[]>([])

const formatTime = (t: string) => {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN')
}

onMounted(async () => {
  try {
    const [s, q] = await Promise.all([
      api.get('/admin/statistics'),
      api.get('/statistics/queries/recent', { params: { days: 30 } })
    ])
    stats.value = s.data
    recentQueries.value = q.data
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
.page-title { margin: 0 0 24px; font-size: 22px; color: #1e293b; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 32px; }
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.stat-icon {
  width: 48px; height: 48px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
}
.users-icon { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.docs-icon { background: linear-gradient(135deg, #10b981, #059669); }
.query-icon { background: linear-gradient(135deg, #f59e0b, #d97706); }
.access-icon { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }
.stat-value { font-size: 28px; font-weight: 700; color: #1e293b; }
.stat-label { font-size: 13px; color: #64748b; margin-top: 2px; }
.stat-detail { font-size: 11px; color: #94a3b8; margin-top: 4px; }
.recent-section { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.recent-section h3 { margin: 0 0 16px; font-size: 16px; color: #1e293b; }
@media (max-width: 1000px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
