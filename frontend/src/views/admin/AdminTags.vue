<template>
  <div class="admin-tags">
    <h2 class="page-title">标签管理</h2>
    <div class="tags-grid">
      <div v-for="tag in tags" :key="tag.name" class="tag-card">
        <div class="tag-header">
          <span v-if="!tag._editing" class="tag-name">{{ tag.name }}</span>
          <el-input v-else v-model="tag._newName" size="small" style="width: 120px" />
          <el-tag size="small" type="info">{{ tag.count }} 篇</el-tag>
        </div>
        <div class="tag-actions">
          <template v-if="!tag._editing">
            <el-button link type="primary" size="small" @click="startRename(tag)">重命名</el-button>
            <el-popconfirm title="确定要删除此标签？" @confirm="deleteTag(tag.name)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
          <template v-else>
            <el-button link type="success" size="small" @click="confirmRename(tag)">确定</el-button>
            <el-button link size="small" @click="tag._editing = false">取消</el-button>
          </template>
        </div>
      </div>
    </div>
    <div v-if="tags.length === 0" class="empty-hint">暂无标签</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const tags = ref<any[]>([])

const fetchTags = async () => {
  try {
    const res = await api.get('/admin/tags')
    tags.value = res.data.map((t: any) => ({ ...t, _editing: false, _newName: t.name }))
  } catch (e) { console.error(e) }
}

const startRename = (tag: any) => {
  tag._editing = true
  tag._newName = tag.name
}

const confirmRename = async (tag: any) => {
  if (!tag._newName.trim() || tag._newName.trim() === tag.name) {
    tag._editing = false
    return
  }
  try {
    await api.put('/admin/tags/rename', { oldName: tag.name, newName: tag._newName.trim() })
    ElMessage.success('标签已重命名')
    fetchTags()
  } catch { ElMessage.error('操作失败') }
}

const deleteTag = async (name: string) => {
  try {
    await api.delete(`/admin/tags/${encodeURIComponent(name)}`)
    ElMessage.success('标签已删除')
    fetchTags()
  } catch { ElMessage.error('操作失败') }
}

onMounted(fetchTags)
</script>

<style scoped>
.page-title { margin: 0 0 24px; font-size: 22px; color: #1e293b; }
.tags-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px; }
.tag-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.tag-header { display: flex; align-items: center; justify-content: space-between; }
.tag-name { font-size: 15px; font-weight: 600; color: #1e293b; }
.tag-actions { display: flex; gap: 8px; }
.empty-hint { text-align: center; color: #94a3b8; padding: 60px 0; }
</style>
