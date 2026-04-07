<template>
  <div class="admin-users">
    <h2 class="page-title">账号管理</h2>
    <el-table :data="users" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="fullName" label="姓名" width="120">
        <template #default="{ row }">
          <span v-if="!row._editing">{{ row.fullName || '-' }}</span>
          <el-input v-else v-model="row._editFullName" size="small" />
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="180">
        <template #default="{ row }">
          <span v-if="!row._editing">{{ row.email }}</span>
          <el-input v-else v-model="row._editEmail" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-select v-model="row._role" size="small" @change="changeRole(row)">
            <el-option label="ADMIN" value="ADMIN" />
            <el-option label="USER" value="USER" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'danger'" size="small">
            {{ row.isActive ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="!row._editing">
            <el-button link type="primary" size="small" @click="startEdit(row)">编辑</el-button>
            <el-button link :type="row.isActive ? 'danger' : 'success'" size="small" @click="toggleActive(row)">
              {{ row.isActive ? '禁用' : '启用' }}
            </el-button>
          </template>
          <template v-else>
            <el-button link type="success" size="small" @click="saveEdit(row)">保存</el-button>
            <el-button link size="small" @click="cancelEdit(row)">取消</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const users = ref<any[]>([])

const formatTime = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : ''

const fetchUsers = async () => {
  const res = await api.get('/admin/users')
  users.value = res.data.map((u: any) => ({
    ...u,
    _role: u.roles?.[0]?.name || (typeof u.roles?.[0] === 'string' ? u.roles[0] : 'USER'),
    _editing: false,
    _editFullName: u.fullName,
    _editEmail: u.email
  }))
}

const changeRole = async (row: any) => {
  try {
    await api.put(`/admin/users/${row.id}/role`, { roleName: row._role })
    ElMessage.success('角色已更新')
  } catch { ElMessage.error('更新失败') }
}

const toggleActive = async (row: any) => {
  try {
    await api.put(`/admin/users/${row.id}/toggle-active`)
    ElMessage.success(row.isActive ? '已禁用' : '已启用')
    fetchUsers()
  } catch { ElMessage.error('操作失败') }
}

const startEdit = (row: any) => {
  row._editing = true
  row._editFullName = row.fullName
  row._editEmail = row.email
}
const cancelEdit = (row: any) => { row._editing = false }
const saveEdit = async (row: any) => {
  try {
    await api.put(`/admin/users/${row.id}`, {
      fullName: row._editFullName,
      email: row._editEmail
    })
    row.fullName = row._editFullName
    row.email = row._editEmail
    row._editing = false
    ElMessage.success('用户信息已更新')
  } catch { ElMessage.error('保存失败') }
}

onMounted(fetchUsers)
</script>

<style scoped>
.page-title { margin: 0 0 20px; font-size: 22px; color: #1e293b; }
:deep(.el-table) { border-radius: 12px; }
</style>
