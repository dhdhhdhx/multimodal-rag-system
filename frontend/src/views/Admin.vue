<template>
  <div class="admin-container">
    <div class="admin-page-header">
      <div class="page-title-area">
        <h2>系统管理</h2>
        <p class="page-desc">管理用户、查看统计数据和系统日志</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="admin-tabs" type="border-card">
      <el-tab-pane label="系统统计" name="statistics">
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-card-header">
              <div class="stat-icon user-icon"><el-icon :size="20"><User /></el-icon></div>
              <span class="stat-title">用户统计</span>
            </div>
            <div class="stat-body">
              <div class="stat-row"><span class="label">总用户数</span><span class="value">{{ statistics.users?.total || 0 }}</span></div>
              <div class="stat-row"><span class="label">活跃用户</span><span class="value success">{{ statistics.users?.active || 0 }}</span></div>
              <div class="stat-row"><span class="label">禁用用户</span><span class="value danger">{{ statistics.users?.inactive || 0 }}</span></div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-card-header">
              <div class="stat-icon doc-icon"><el-icon :size="20"><Document /></el-icon></div>
              <span class="stat-title">文档统计</span>
            </div>
            <div class="stat-body">
              <div class="stat-row"><span class="label">总文档数</span><span class="value">{{ statistics.documents?.total || 0 }}</span></div>
              <div class="stat-row"><span class="label">已完成</span><span class="value success">{{ statistics.documents?.completed || 0 }}</span></div>
              <div class="stat-row"><span class="label">处理中</span><span class="value warning">{{ statistics.documents?.processing || 0 }}</span></div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-card-header">
              <div class="stat-icon query-icon"><el-icon :size="20"><ChatLineRound /></el-icon></div>
              <span class="stat-title">查询统计</span>
            </div>
            <div class="stat-body">
              <div class="stat-row"><span class="label">总查询数</span><span class="value">{{ statistics.queries?.total || 0 }}</span></div>
              <div class="stat-row"><span class="label">本周查询</span><span class="value primary">{{ statistics.queries?.thisWeek || 0 }}</span></div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="用户管理" name="users">
        <el-table :data="users" stripe style="width: 100%" class="admin-table">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" width="150" />
          <el-table-column prop="fullName" label="姓名" width="150" />
          <el-table-column prop="email" label="邮箱" width="200" />
          <el-table-column label="角色" width="150">
            <template #default="{ row }">
              <el-select v-model="row.roleToUpdate" placeholder="选择角色" @change="handleRoleChange(row)" size="small">
                <el-option label="管理员" value="ADMIN" />
                <el-option label="普通用户" value="USER" />
                <el-option label="访客" value="GUEST" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.isActive ? 'success' : 'danger'" size="small">{{ row.isActive ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button :type="row.isActive ? 'warning' : 'success'" size="small" @click="toggleUserActive(row)">{{ row.isActive ? '禁用' : '启用' }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="查询日志" name="logs">
        <el-table :data="queryLogs" stripe style="width: 100%" class="admin-table">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="query" label="查询内容" min-width="300" />
          <el-table-column prop="queryTime" label="查询时间" width="180">
            <template #default="{ row }">{{ formatDate(row.queryTime) }}</template>
          </el-table-column>
          <el-table-column label="用户" width="150">
            <template #default="{ row }">{{ row.user?.username || '匿名' }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'
import { User, ChatLineRound, Document } from '@element-plus/icons-vue'

const router = useRouter()
const activeTab = ref('statistics')
const statistics = ref<any>({})
const users = ref<any[]>([])
const queryLogs = ref<any[]>([])

const fetchStatistics = async () => {
  try { const res = await api.get('/admin/statistics'); statistics.value = res.data }
  catch (error) { console.error('Failed to fetch statistics', error) }
}

const fetchUsers = async () => {
  try {
    const res = await api.get('/admin/users')
    users.value = res.data.map((user: any) => ({ ...user, roleToUpdate: user.roles?.[0]?.name || 'USER' }))
  } catch (error) { ElMessage.error('获取用户列表失败') }
}

const fetchQueryLogs = async () => {
  try { const res = await api.get('/statistics/queries/recent?days=30'); queryLogs.value = res.data }
  catch (error) { console.error('Failed to fetch query logs', error) }
}

const toggleUserActive = async (user: any) => {
  try {
    const res = await api.put(`/admin/users/${user.id}/toggle-active`)
    ElMessage.success(res.data.message)
    await fetchUsers()
  } catch (error) { ElMessage.error('操作失败') }
}

const handleRoleChange = async (user: any) => {
  try {
    const res = await api.put(`/admin/users/${user.id}/role`, { roleName: user.roleToUpdate })
    ElMessage.success(res.data.message)
    await fetchUsers()
  } catch (error) { ElMessage.error('更新角色失败') }
}

const formatDate = (dateStr: string) => new Date(dateStr).toLocaleString('zh-CN')

onMounted(() => { fetchStatistics(); fetchUsers(); fetchQueryLogs() })
</script>

<style scoped>
.admin-container {
  padding: 24px 32px;
  min-height: calc(100vh - 56px);
  max-width: 1200px;
  margin: 0 auto;
}

.admin-page-header {
  margin-bottom: 24px;
}

.admin-page-header h2 {
  margin: 0 0 4px 0;
  color: #f1f5f9;
  font-size: 22px;
  font-weight: 700;
}

.page-desc {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.admin-tabs {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 8px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 20px;
}

.stat-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.user-icon { background: linear-gradient(135deg, #6366f1, #818cf8); }
.doc-icon { background: linear-gradient(135deg, #10b981, #34d399); }
.query-icon { background: linear-gradient(135deg, #f59e0b, #fbbf24); }

.stat-title {
  font-weight: 600;
  font-size: 15px;
  color: #e2e8f0;
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 8px;
}

.stat-row .label { color: #94a3b8; font-size: 13px; }
.stat-row .value { font-size: 20px; font-weight: 700; color: #e2e8f0; }
.stat-row .value.success { color: #22c55e; }
.stat-row .value.danger { color: #ef4444; }
.stat-row .value.warning { color: #f59e0b; }
.stat-row .value.primary { color: #6366f1; }
</style>