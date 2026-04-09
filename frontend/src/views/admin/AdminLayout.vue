<template>
  <div class="admin-layout">
    <!-- Mobile overlay -->
    <div
      v-if="isMobile && showSidebar"
      class="sidebar-overlay"
      @click="showSidebar = false"
    ></div>
    <!-- Sidebar -->
    <aside class="admin-sidebar" :class="{ 'is-visible': showSidebar }">
      <div class="sidebar-header">
        <router-link to="/" class="back-link" @click="onNavClick">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回前台</span>
        </router-link>
        <h2 class="sidebar-title">后台管理</h2>
      </div>
      <nav class="sidebar-nav">
        <router-link to="/admin" exact-active-class="active" class="nav-item" @click="onNavClick">
          <el-icon><DataBoard /></el-icon><span>仪表盘</span>
        </router-link>
        <router-link to="/admin/users" active-class="active" class="nav-item" @click="onNavClick">
          <el-icon><User /></el-icon><span>账号管理</span>
        </router-link>
        <router-link to="/admin/documents" active-class="active" class="nav-item" @click="onNavClick">
          <el-icon><Document /></el-icon><span>文档管理</span>
        </router-link>
        <router-link to="/admin/tags" active-class="active" class="nav-item" @click="onNavClick">
          <el-icon><PriceTag /></el-icon><span>标签管理</span>
        </router-link>
        <router-link to="/admin/stats" active-class="active" class="nav-item" @click="onNavClick">
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
      <button v-if="isMobile" class="hamburger-btn" @click="showSidebar = !showSidebar">
        <el-icon :size="22"><Menu /></el-icon>
      </button>
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ArrowLeft, DataBoard, User, Document, PriceTag, TrendCharts, Menu } from '@element-plus/icons-vue'

const MOBILE_BREAKPOINT = 768

const isMobile = ref(false)
const showSidebar = ref(true)

const checkMobile = () => {
  const wasMobile = isMobile.value
  isMobile.value = window.innerWidth < MOBILE_BREAKPOINT
  if (isMobile.value && !wasMobile) {
    showSidebar.value = false
  } else if (!isMobile.value && wasMobile) {
    showSidebar.value = true
  }
}

const onNavClick = () => {
  if (isMobile.value) {
    showSidebar.value = false
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkMobile)
})

const username = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user_info') || '{}').username || 'Admin'
  } catch { return 'Admin' }
})
</script>

<style scoped>
:root {
  --z-overlay: 99;
}

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

/* Mobile overlay */
.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: var(--z-overlay);
}

/* Hamburger button */
.hamburger-btn {
  display: none;
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: #1e293b;
  color: #e2e8f0;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}
.hamburger-btn:hover {
  background: #334155;
}

/* Mobile responsive */
@media (max-width: 767px) {
  .admin-sidebar {
    transform: translateX(-100%);
    transition: transform 0.3s ease;
    z-index: calc(var(--z-overlay) + 1);
  }
  .admin-sidebar.is-visible {
    transform: translateX(0);
  }
  .admin-main {
    margin-left: 0;
    padding: 20px 16px;
    position: relative;
  }
  .hamburger-btn {
    display: flex;
  }
}
</style>
