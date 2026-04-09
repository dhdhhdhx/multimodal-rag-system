<template>
  <div class="admin-tags">
    <div class="admin-page-header">
      <h2 class="admin-page-title">标签管理</h2>
      <div class="header-actions">
        <el-input v-model="newTagName" placeholder="新标签名称" clearable style="width: 200px" @keydown.enter="addTag">
          <template #prefix>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          </template>
        </el-input>
        <el-button type="primary" @click="addTag">添加标签</el-button>
      </div>
    </div>

    <div class="admin-filter-bar">
      <el-input v-model="tagSearch" placeholder="搜索标签..." clearable style="width: 220px">
        <template #prefix>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        </template>
      </el-input>
      <el-radio-group v-model="sortBy" size="small" @change="sortTags">
        <el-radio-button value="count">按数量</el-radio-button>
        <el-radio-button value="name">按名称</el-radio-button>
      </el-radio-group>
    </div>

    <div class="tags-grid" v-if="filteredTags.length > 0">
      <div v-for="tag in filteredTags" :key="tag.name" class="admin-card tag-card">
        <div class="tag-card-header">
          <span v-if="!tag._editing" class="tag-name">{{ tag.name }}</span>
          <el-input v-else v-model="tag._newName" size="small" style="width:140px" @keydown.enter="confirmRename(tag)" />
          <span class="tag-count">{{ tag.count }} 篇</span>
        </div>
        <div class="tag-card-actions">
          <template v-if="!tag._editing">
            <el-button link type="primary" size="small" @click="startRename(tag)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:3px"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
              重命名
            </el-button>
            <el-popconfirm title="确定要删除此标签吗？" @confirm="deleteTag(tag.name)">
              <template #reference>
                <el-button link type="danger" size="small">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:3px"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                  删除
                </el-button>
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
    <div v-else class="admin-empty">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="margin-bottom:8px; opacity:0.4;"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
      <span>暂无标签数据</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const tags = ref<any[]>([])
const tagSearch = ref('')
const sortBy = ref('count')
const newTagName = ref('')

const sortTags = () => {
  if (sortBy.value === 'count') {
    tags.value.sort((a, b) => b.count - a.count)
  } else {
    tags.value.sort((a, b) => a.name.localeCompare(b.name))
  }
}

const filteredTags = computed(() => {
  if (!tagSearch.value.trim()) return tags.value
  const kw = tagSearch.value.trim().toLowerCase()
  return tags.value.filter(t => t.name.toLowerCase().includes(kw))
})

const fetchTags = async () => {
  try {
    const res = await api.get('/admin/tags')
    tags.value = res.data.map((t: any) => ({ ...t, _editing: false, _newName: t.name }))
    sortTags()
  } catch (e) { console.error(e) }
}

const addTag = async () => {
  if (!newTagName.value.trim()) { ElMessage.warning('请输入标签名称'); return }
  try {
    await api.post('/admin/tags', { name: newTagName.value.trim() })
    ElMessage.success('标签已创建')
    newTagName.value = ''
    fetchTags()
  } catch { ElMessage.error('创建失败') }
}

const startRename = (tag: any) => { tag._editing = true; tag._newName = tag.name }
const confirmRename = async (tag: any) => {
  if (!tag._newName.trim() || tag._newName.trim() === tag.name) { tag._editing = false; return }
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
.tags-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.tag-card { padding: 20px; display: flex; flex-direction: column; gap: 14px; }
.tag-card-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.tag-name { font-size: 16px; font-weight: 600; color: var(--admin-text, #1a1a2e); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tag-count { flex-shrink: 0; padding: 2px 10px; background: var(--admin-primary-light, #eef1ff); color: var(--admin-primary, #4361ee); border-radius: 10px; font-size: 12px; font-weight: 600; }
.tag-card-actions { display: flex; gap: 4px; }
</style>
