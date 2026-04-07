<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-header">
        <router-link to="/" class="back-link">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回前台</span>
        </router-link>
        <h2 class="sidebar-title">后台管理</h2>
      </div>
      <nav class="sidebar-nav">
        <router-link to="/admin" exact-active-class="active" class="nav-item">
          <el-icon><DataBoard /></el-icon><span>仪表盘</span>
        </router-link>
        <router-link to="/admin/users" active-class="active" class="nav-item">
          <el-icon><User /></el-icon><span>账号管理</span>
        </router-link>
        <router-link to="/admin/documents" active-class="active" class="nav-item">
          <el-icon><Document /></el-icon><span>文档管理</span>
        </router-link>
        <router-link to="/admin/tags" active-class="active" class="nav-item">
          <el-icon><PriceTag /></el-icon><span>标签管理</span>
        </router-link>
        <router-link to="/admin/stats" active-class="active" class="nav-item">
          <el-icon><TrendCharts /></el-icon><span>数据统计</span>
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <div class="admin-info">
          <span class="admin-avatar">{{ username.charAt(0).toUpperCase() }}</span>
          <span class="admin-name">{{ username }}</span>
        </div>
      </div>
    </aside>
    <main class="admin-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowLeft, DataBoard, User, Document, PriceTag, TrendCharts } from '@element-plus/icons-vue'

const username = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user_info') || '{}').username || 'Admin'
  } catch { return 'Admin' }
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #f0f2f5;
}
.admin-sidebar {
  width: 220px;
  background: #1e293b;
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
}
.sidebar-header {
  padding: 20px 16px 12px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
.back-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #94a3b8;
  text-decoration: none;
  font-size: 12px;
  margin-bottom: 12px;
  transition: color 0.2s;
}
.back-link:hover { color: #fff; }
.sidebar-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}
.sidebar-nav {
  flex: 1;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-radius: 8px;
  color: #94a3b8;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
}
.nav-item:hover { background: rgba(255,255,255,0.06); color: #e2e8f0; }
.nav-item.active {
  background: #10b981;
  color: #fff;
  font-weight: 600;
}
.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255,255,255,0.08);
}
.admin-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.admin-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981, #3b82f6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}
.admin-name { font-size: 13px; color: #e2e8f0; }
.admin-main {
  flex: 1;
  margin-left: 220px;
  padding: 28px 32px;
}
</style>
