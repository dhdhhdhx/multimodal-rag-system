<template>
  <div class="admin-stats">
    <div class="admin-page-header">
      <h2 class="admin-page-title">数据统计</h2>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
          value-format="YYYY-MM-DD"
          @change="fetchData"
        />
      </div>
    </div>

    <!-- Hot Keywords -->
    <div class="admin-card" style="margin-bottom: 20px;">
      <h3 class="admin-section-title">高频查询词</h3>
      <div v-if="hotKeywords.length" class="keyword-cloud">
        <span v-for="kw in hotKeywords" :key="kw.keyword" class="admin-keyword-pill"
          :style="{ fontSize: Math.min(12 + kw.count * 1.5, 22) + 'px' }">
          {{ kw.keyword }}
          <small>{{ kw.count }}</small>
        </span>
      </div>
      <div v-else class="admin-empty">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="margin-bottom:8px; opacity:0.4;"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        暂无查询数据
      </div>
    </div>

    <!-- User Activity -->
    <div class="admin-card">
      <h3 class="admin-section-title">用户活跃度（近30天）</h3>
      <div style="padding: 0; overflow: hidden;">
        <el-table :data="userActivity" stripe style="width: 100%">
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="fullName" label="姓名" width="120" />
          <el-table-column prop="queryCount" label="提问数" width="100" sortable />
          <el-table-column prop="accessCount" label="访问数" width="100" sortable />
          <el-table-column label="活跃度" min-width="200">
            <template #default="{ row }">
              <div style="display:flex; align-items:center; gap:10px;">
                <div class="admin-activity-bar" style="flex:1;">
                  <div class="bar-fill" :style="{ width: barWidth(row) + '%' }"></div>
                </div>
                <span style="font-size:12px; color:var(--admin-text-muted); min-width:36px;">{{ barWidth(row) }}%</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <span class="admin-status-dot" :class="row.isActive ? 'active' : 'inactive'">
                {{ row.isActive ? '正常' : '休眠' }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-if="userActivity.length === 0" class="admin-empty" style="padding: 32px 0;">
        No Data
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '../../api'

const dateRange = ref<string[]>([])
const hotKeywords = ref<any[]>([])
const userActivity = ref<any[]>([])

// maxKwCount reserved for chart opacity scaling
const maxActivity = computed(() => {
  if (!userActivity.value.length) return 1
  return Math.max(...userActivity.value.map(u => (u.queryCount || 0) + (u.accessCount || 0)), 1)
})

const barWidth = (row: any) => {
  const total = (row.queryCount || 0) + (row.accessCount || 0)
  return Math.round((total / maxActivity.value) * 100)
}

const fetchData = async () => {
  try {
    const params: any = { days: 30 }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const [kw, ua] = await Promise.all([
      api.get('/admin/statistics/hot-keywords', { params }),
      api.get('/admin/statistics/user-activity', { params })
    ])
    hotKeywords.value = kw.data
    userActivity.value = ua.data
  } catch (e) { console.error(e) }
}

onMounted(fetchData)
</script>

<style scoped>
.keyword-cloud { display: flex; flex-wrap: wrap; gap: 10px; align-items: baseline; }
</style>
