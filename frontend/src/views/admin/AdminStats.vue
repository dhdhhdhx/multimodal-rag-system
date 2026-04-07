<template>
  <div class="admin-stats">
    <h2 class="page-title">数据统计</h2>
    <div class="section">
      <h3>高频查询词</h3>
      <div class="keyword-cloud" v-if="hotKeywords.length">
        <span v-for="kw in hotKeywords" :key="kw.keyword" class="keyword-pill"
          :style="{ fontSize: Math.min(12 + kw.count * 2, 24) + 'px', opacity: 0.6 + Math.min(kw.count / maxKwCount, 1) * 0.4 }">
          {{ kw.keyword }}
          <small>{{ kw.count }}</small>
        </span>
      </div>
      <div v-else class="empty-hint">暂无查询数据</div>
    </div>
    <div class="section">
      <h3>用户活跃度（近30天）</h3>
      <el-table :data="userActivity" stripe size="small">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="fullName" label="姓名" width="120" />
        <el-table-column prop="queryCount" label="提问数" width="100" sortable />
        <el-table-column prop="accessCount" label="访问数" width="100" sortable />
        <el-table-column label="活跃度" min-width="200">
          <template #default="{ row }">
            <div class="activity-bar">
              <div class="bar-fill" :style="{ width: barWidth(row) + '%' }"></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'" size="small">
              {{ row.isActive ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '../../api'

const hotKeywords = ref<any[]>([])
const userActivity = ref<any[]>([])

const maxKwCount = computed(() =>
  hotKeywords.value.length ? Math.max(...hotKeywords.value.map(k => k.count)) : 1
)

const maxActivity = computed(() => {
  if (!userActivity.value.length) return 1
  return Math.max(...userActivity.value.map(u => (u.queryCount || 0) + (u.accessCount || 0)), 1)
})

const barWidth = (row: any) => {
  const total = (row.queryCount || 0) + (row.accessCount || 0)
  return Math.round((total / maxActivity.value) * 100)
}

onMounted(async () => {
  try {
    const [kw, ua] = await Promise.all([
      api.get('/admin/statistics/hot-keywords', { params: { days: 30 } }),
      api.get('/admin/statistics/user-activity', { params: { days: 30 } })
    ])
    hotKeywords.value = kw.data
    userActivity.value = ua.data
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
.page-title { margin: 0 0 24px; font-size: 22px; color: #1e293b; }
.section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.section h3 { margin: 0 0 16px; font-size: 16px; color: #1e293b; }
.keyword-cloud { display: flex; flex-wrap: wrap; gap: 10px; align-items: baseline; }
.keyword-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 20px;
  color: #166534;
  font-weight: 500;
}
.keyword-pill small { font-size: 10px; opacity: 0.6; }
.empty-hint { color: #94a3b8; text-align: center; padding: 40px 0; }
.activity-bar {
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981, #3b82f6);
  border-radius: 4px;
  transition: width 0.3s;
}
</style>
