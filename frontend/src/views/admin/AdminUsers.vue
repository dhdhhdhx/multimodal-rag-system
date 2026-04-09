<template>
  <div class="admin-users">
    <div class="admin-page-header">
      <h2 class="admin-page-title">账号管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showAddDialog = true">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:4px"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新增用户
        </el-button>
      </div>
    </div>

    <div class="admin-filter-bar">
      <el-input v-model="searchKeyword" placeholder="搜索用户名 / 邮箱" clearable style="width: 240px" @keydown.enter="fetchUsers" @clear="fetchUsers">
        <template #prefix>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        </template>
      </el-input>
      <el-select v-model="filterRole" placeholder="角色" clearable style="width: 120px" @change="fetchUsers">
        <el-option label="ADMIN" value="ADMIN" />
        <el-option label="USER" value="USER" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 120px" @change="fetchUsers">
        <el-option label="正常" :value="true" />
        <el-option label="禁用" :value="false" />
      </el-select>
    </div>

    <div class="admin-card" style="padding: 0; overflow: hidden;">
      <el-table :data="users" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="65" />
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column label="姓名" width="130">
          <template #default="{ row }">
            <span v-if="!row._editing">{{ row.fullName || '-' }}</span>
            <el-input v-else v-model="row._editFullName" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="190">
          <template #default="{ row }">
            <span v-if="!row._editing">{{ row.email }}</span>
            <el-input v-else v-model="row._editEmail" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <span v-if="!row._editing" class="admin-role-tag" :class="row._role === 'ADMIN' ? 'role-admin' : 'role-user'">{{ row._role }}</span>
            <el-select v-else v-model="row._role" size="small">
              <el-option label="ADMIN" value="ADMIN" />
              <el-option label="USER" value="USER" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="admin-status-dot" :class="row.isActive ? 'active' : 'inactive'">
              {{ row.isActive ? '正常' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170" sortable="custom">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="!row._editing">
              <el-button link type="primary" size="small" @click="startEdit(row)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:2px"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                编辑
              </el-button>
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

    <!-- Add User Dialog -->
    <el-dialog v-model="showAddDialog" title="新增用户" width="460px">
      <el-form label-width="70px">
        <el-form-item label="用户名">
          <el-input v-model="newUser.username" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="newUser.fullName" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="newUser.email" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="newUser.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="addUser" :loading="addingUser">确认新增</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const users = ref<any[]>([])
const searchKeyword = ref('')
const filterRole = ref('')
const filterStatus = ref<boolean | ''>('')
const showAddDialog = ref(false)
const addingUser = ref(false)
const newUser = ref({ username: '', fullName: '', email: '', password: '' })

const formatTime = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : ''

const fetchUsers = async () => {
  try {
    const res = await api.get('/admin/users')
    let data = res.data.map((u: any) => ({
      ...u,
      _role: u.roles?.[0]?.name || (typeof u.roles?.[0] === 'string' ? u.roles[0] : 'USER'),
      _editing: false,
      _editFullName: u.fullName,
      _editEmail: u.email
    }))
    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.trim().toLowerCase()
      data = data.filter((u: any) => (u.username || '').toLowerCase().includes(kw) || (u.email || '').toLowerCase().includes(kw))
    }
    if (filterRole.value) data = data.filter((u: any) => u._role === filterRole.value)
    if (filterStatus.value !== '') data = data.filter((u: any) => u.isActive === filterStatus.value)
    users.value = data
  } catch { ElMessage.error('获取用户列表失败') }
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

const startEdit = (row: any) => { row._editing = true; row._editFullName = row.fullName; row._editEmail = row.email }
const cancelEdit = (row: any) => { row._editing = false }
const saveEdit = async (row: any) => {
  try {
    await api.put(`/admin/users/${row.id}`, { fullName: row._editFullName, email: row._editEmail })
    if (row._role !== (row.roles?.[0]?.name || (typeof row.roles?.[0] === 'string' ? row.roles[0] : 'USER'))) {
      await changeRole(row)
    }
    row.fullName = row._editFullName
    row.email = row._editEmail
    row._editing = false
    ElMessage.success('用户信息已更新')
  } catch { ElMessage.error('保存失败') }
}

const addUser = async () => {
  if (!newUser.value.username || !newUser.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  addingUser.value = true
  try {
    await api.post('/admin/users', newUser.value)
    ElMessage.success('用户已创建')
    showAddDialog.value = false
    newUser.value = { username: '', fullName: '', email: '', password: '' }
    fetchUsers()
  } catch { ElMessage.error('创建失败') }
  finally { addingUser.value = false }
}

onMounted(fetchUsers)
</script>

<style scoped>
</style>
