<template>
  <div class="user-dashboard">
    <div v-if="error" class="error-hint">{{ error }}</div>
    <template v-else>
      <div class="stat-card glass-card" style="margin-bottom: 20px;">
        <h4>存储使用情况</h4>
        <div ref="storageChart" style="width: 100%; height: 180px;"></div>
        <p class="stat-info">
          {{ formatSize(stats.storage?.used) }} / {{ formatSize(stats.storage?.total) }}
        </p>
      </div>

      <div class="stat-card glass-card" style="margin-bottom: 20px;">
        <h4>内容细分</h4>
        <div ref="modalityChart" style="width: 100%; height: 180px;"></div>
      </div>

      <div class="stat-card glass-card">
        <h4>系统状态</h4>
        <div ref="statusChart" style="width: 100%; height: 150px;"></div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'
import api from '../api'

const props = defineProps({
  refreshTrigger: Number
})

const storageChart = ref(null)
const modalityChart = ref(null)
const statusChart = ref(null)
const stats = ref<any>({})
const error = ref('')

const formatSize = (bytes: number) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const fetchStats = async () => {
  try {
    error.value = ''
    const res = await api.get('/dashboard/stats')
    stats.value = res.data
    initCharts()
  } catch (err: any) {
    console.error('Failed to fetch stats', err)
    error.value = err.response?.data?.message || '获取统计数据失败'
  }
}

const initCharts = () => {
  let sChart: any = null
  let mChart: any = null
  let stChart: any = null

  // Storage Chart
  if (storageChart.value && stats.value.storage) {
    sChart = echarts.init(storageChart.value)
    sChart.setOption({
      series: [{
        type: 'pie',
        radius: ['70%', '90%'],
        avoidLabelOverlap: false,
        label: { show: false },
        data: [
          { value: stats.value.storage.used, name: 'Used', itemStyle: { color: '#3b82f6' } },
          { value: stats.value.storage.total - stats.value.storage.used, name: 'Free', itemStyle: { color: 'rgba(255,255,255,0.05)' } }
        ]
      }]
    })
  }

  // Modality Chart
  if (modalityChart.value) {
    mChart = echarts.init(modalityChart.value)
    const modalityData = Object.entries(stats.value.modalities || {}).map(([name, value]) => ({ name, value }))
    mChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'horizontal', bottom: 0, textStyle: { color: '#94a3b8', fontSize: 10 } },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '40%'],
        avoidLabelOverlap: true,
        label: { show: true, position: 'outside', formatter: '{d}%', color: '#94a3b8', fontSize: 10 },
        data: modalityData.length > 0 ? modalityData : [{ name: '无数据', value: 0, itemStyle: { color: 'rgba(255,255,255,0.1)' } }]
      }]
    })
  }

  // Status Chart
  if (statusChart.value) {
    stChart = echarts.init(statusChart.value)
    const systemData = [
      { name: 'Python AI 服务', value: stats.value.pythonService ? 100 : 0, color: stats.value.pythonService ? '#10b981' : '#ef4444' },
      { name: '文档处理引擎', value: 100, color: '#10b981' }
    ]
    stChart.setOption({
      grid: { left: '3%', right: '15%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: { type: 'value', show: false, max: 100 },
      yAxis: {
        type: 'category',
        data: systemData.map(d => d.name),
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#94a3b8', fontSize: 11 }
      },
      series: [{
        type: 'bar',
        data: systemData.map(d => ({ value: d.value, itemStyle: { color: d.color } })),
        barWidth: 15,
        itemStyle: { borderRadius: 5 },
        label: { show: true, position: 'right', color: '#fff', formatter: (params: any) => params.value === 100 ? '正常' : '异常' }
      }]
    })
  }

  // Resize handler
  const resizeHandler = () => {
    sChart?.resize()
    mChart?.resize()
    stChart?.resize()
  }
  window.addEventListener('resize', resizeHandler)
}

onMounted(fetchStats)
watch(() => props.refreshTrigger, fetchStats)
</script>

<style scoped>
.user-dashboard {
  padding: 10px 0;
}
.stat-card {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.stat-card h4 {
  margin: 0 0 15px 0;
  color: #e2e8f0;
  font-size: 1rem;
  font-weight: 600;
}
.stat-info {
  text-align: center;
  margin-top: 10px;
  font-weight: bold;
  font-size: 0.9rem;
  color: #cbd5e1;
}
.error-hint {
  text-align: center;
  color: #f87171;
  font-size: 0.85rem;
  padding: 20px 0;
}
</style>
