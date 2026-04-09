<template>
  <div class="admin-dashboard">
    <!-- Stat Cards Row 1 -->
    <div class="stat-grid admin-stat-grid-4">
      <div class="admin-stat-card" v-for="card in statCards" :key="card.label">
        <div class="admin-stat-icon" :style="{ background: card.gradient }">
          <span v-html="card.icon"></span>
        </div>
        <div>
          <div class="admin-stat-value">{{ card.value }}</div>
          <div class="admin-stat-label">{{ card.label }}</div>
          <div class="admin-stat-detail">{{ card.detail }}</div>
        </div>
      </div>
    </div>

    <!-- Mini Stat Cards Row 2 -->
    <div class="mini-stat-grid">
      <div class="admin-card mini-stat-row">
        <div class="mini-stat-item">
          <div class="mini-stat-dot active"></div>
          <div>
            <div class="mini-stat-val">{{ stats.users?.active || 0 }}</div>
            <div class="mini-stat-lbl">活跃用户</div>
          </div>
        </div>
        <div class="mini-stat-divider"></div>
        <div class="mini-stat-item">
          <div class="mini-stat-dot inactive"></div>
          <div>
            <div class="mini-stat-val">{{ stats.users?.inactive || 0 }}</div>
            <div class="mini-stat-lbl">禁用用户</div>
          </div>
        </div>
      </div>
      <div class="admin-card mini-stat-row">
        <div class="mini-stat-item">
          <div class="mini-stat-badge completed">公开</div>
          <div>
            <div class="mini-stat-val">{{ stats.documents?.public || 0 }}</div>
            <div class="mini-stat-lbl">公开文档</div>
          </div>
        </div>
        <div class="mini-stat-divider"></div>
        <div class="mini-stat-item">
          <div class="mini-stat-badge processing">处理中</div>
          <div>
            <div class="mini-stat-val">{{ stats.documents?.processing || 0 }}</div>
            <div class="mini-stat-lbl">处理中文档</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom Row: Recent Queries + Chart Placeholder -->
    <div class="bottom-grid">
      <div class="admin-card bottom-card">
        <h3 class="admin-section-title">最近查询</h3>
        <el-table :data="recentQueries" stripe size="small" max-height="360" style="width: 100%">
          <el-table-column prop="queryText" label="查询内容" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="时间" width="180">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <div class="admin-card bottom-card chart-card">
        <h3 class="admin-section-title">访问趋势</h3>
        <div class="chart-placeholder">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
          <span>图表数据加载中</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '../../api'

const stats = ref<any>({})
const recentQueries = ref<any[]>([])

const formatTime = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : ''

const statCards = computed(() => [
  {
    label: '注册用户',
    value: stats.value.users?.total || 0,
    detail: '本周新增 --',
    gradient: 'linear-gradient(135deg, #4361ee, #3a56d4)',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>'
  },
  {
    label: '知识文档',
    value: stats.value.documents?.total || 0,
    detail: '公开 ' + (stats.value.documents?.public || 0) + ' / 处理中 ' + (stats.value.documents?.processing || 0),
    gradient: 'linear-gradient(135deg, #22c55e, #16a34a)',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>'
  },
  {
    label: 'AI 问答',
    value: stats.value.queries?.total || 0,
    detail: '本周 ' + (stats.value.queries?.thisWeek || 0),
    gradient: 'linear-gradient(135deg, #f59e0b, #d97706)',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>'
  },
  {
    label: '文档访问',
    value: stats.value.accesses?.total || 0,
    detail: '累计访问量',
    gradient: 'linear-gradient(135deg, #7c3aed, #6d28d9)',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>'
  }
])

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
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 20px; }
.mini-stat-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; margin-bottom: 24px; }
.mini-stat-row { display: flex; align-items: center; justify-content: center; gap: 32px; padding: 20px 24px; }
.mini-stat-item { display: flex; align-items: center; gap: 12px; }
.mini-stat-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.mini-stat-dot.active { background: var(--admin-success, #22c55e); }
.mini-stat-dot.inactive { background: var(--admin-danger, #ef4444); }
.mini-stat-badge { padding: 3px 10px; border-radius: 10px; font-size: 11px; font-weight: 600; }
.mini-stat-badge.completed { background: var(--admin-success-light, #f0fdf4); color: var(--admin-success, #22c55e); }
.mini-stat-badge.processing { background: var(--admin-warning-light, #fffbeb); color: var(--admin-warning, #f59e0b); }
.mini-stat-val { font-size: 24px; font-weight: 700; color: var(--admin-text, #1a1a2e); line-height: 1.2; }
.mini-stat-lbl { font-size: 12px; color: var(--admin-text-muted, #94a3b8); margin-top: 2px; }
.mini-stat-divider { width: 1px; height: 40px; background: var(--admin-border, #e8ecf1); }
.bottom-grid { display: grid; grid-template-columns: 1.4fr 1fr; gap: 20px; }
.bottom-card { min-height: 200px; }
.chart-card { display: flex; flex-direction: column; }
.chart-placeholder { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: var(--admin-text-muted, #94a3b8); font-size: 14px; }
@media (max-width: 1024px) {
  .bottom-grid { grid-template-columns: 1fr; }
}
</style>
