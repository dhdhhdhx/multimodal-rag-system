<template>
  <div class="admin-layout">
    <div v-if="isMobile && showSidebar" class="sidebar-overlay" @click="showSidebar = false"></div>

    <aside class="admin-sidebar" :class="{ collapsed, 'is-visible': showSidebar }">
      <div class="sidebar-header">
        <div class="sidebar-brand">
          <div class="brand-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>
          </div>
          <transition name="fade"><span v-if="!collapsed" class="brand-text">RAG Admin</span></transition>
        </div>
        <button v-if="!isMobile" class="collapse-btn" :class="{ collapsed }" @click="collapsed = !collapsed" :aria-label="collapsed ? 'Expand' : 'Collapse'">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
      </div>

      <nav class="sidebar-nav">
        <router-link v-for="item in navItems" :key="item.path" :to="item.path" exact-active-class="active" active-class="active" class="nav-item" @click="onNavClick">
          <span class="nav-icon" v-html="item.icon"></span>
          <transition name="fade"><span v-if="!collapsed" class="nav-label">{{ item.label }}</span></transition>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <router-link to="/" class="back-link" @click="onNavClick">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
          <transition name="fade"><span v-if="!collapsed">返回前台</span></transition>
        </router-link>
      </div>
    </aside>

    <div class="admin-main-area" :class="{ 'sidebar-collapsed': collapsed && !isMobile }">
      <header class="admin-topbar">
        <div class="topbar-left">
          <button v-if="isMobile" class="hamburger-btn" @click="showSidebar = !showSidebar" aria-label="Toggle menu">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
          </button>
          <nav class="breadcrumb" aria-label="Breadcrumb">
            <router-link to="/admin" class="breadcrumb-item">Admin</router-link>
            <span class="breadcrumb-sep">/</span>
            <span class="breadcrumb-current">{{ currentPageTitle }}</span>
          </nav>
        </div>
        <div class="topbar-right">
          <div class="user-menu">
            <div class="user-avatar">{{ username.charAt(0).toUpperCase() }}</div>
            <span class="user-name">{{ username }}</span>
          </div>
          <button class="logout-btn" @click="handleLogout" aria-label="Logout">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          </button>
        </div>
      </header>
      <main class="admin-content"><router-view /></main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearTokens } from '../../api'

const route = useRoute()
const router = useRouter()
const MOBILE_BREAKPOINT = 768
const isMobile = ref(false)
const showSidebar = ref(true)
const collapsed = ref(false)

const navItems = [
  { path: '/admin', label: '仪表盘', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>' },
  { path: '/admin/users', label: '账号管理', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>' },
  { path: '/admin/documents', label: '文档管理', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>' },
  { path: '/admin/tags', label: '标签管理', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>' },
  { path: '/admin/topics', label: '话题管理', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>' },
  { path: '/admin/stats', label: '数据统计', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>' }
]

const pageTitles: Record<string, string> = {
  '/admin': '仪表盘', '/admin/users': '账号管理',
  '/admin/documents': '文档管理', '/admin/tags': '标签管理', '/admin/stats': '数据统计'
}
const currentPageTitle = computed(() => pageTitles[route.path] || '仪表盘')
const username = computed(() => {
  try { return JSON.parse(localStorage.getItem('user_info') || '{}').username || 'Admin' }
  catch { return 'Admin' }
})

const checkMobile = () => {
  const wasMobile = isMobile.value
  isMobile.value = window.innerWidth < MOBILE_BREAKPOINT
  if (isMobile.value && !wasMobile) showSidebar.value = false
  else if (!isMobile.value && wasMobile) showSidebar.value = true
}
const onNavClick = () => { if (isMobile.value) showSidebar.value = false }
const handleLogout = () => { clearTokens(); router.push('/login') }

onMounted(() => { checkMobile(); window.addEventListener('resize', checkMobile) })
onBeforeUnmount(() => { window.removeEventListener('resize', checkMobile) })
</script>

<style scoped>
.admin-layout { display: flex; min-height: 100vh; background: var(--admin-bg, #f5f7fa); }
.admin-sidebar { width: var(--admin-sidebar-width, 240px); background: #1a1a2e; color: #c8cad8; display: flex; flex-direction: column; position: fixed; top: 0; left: 0; bottom: 0; z-index: 100; transition: width 0.25s ease; overflow: hidden; }
.admin-sidebar.collapsed { width: var(--admin-sidebar-collapsed-width, 64px); }
.sidebar-header { padding: 20px 16px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid rgba(255,255,255,0.06); min-height: 68px; }
.sidebar-brand { display: flex; align-items: center; gap: 10px; overflow: hidden; white-space: nowrap; }
.brand-icon { width: 32px; height: 32px; border-radius: var(--admin-radius-sm, 8px); background: var(--admin-primary, #4361ee); color: #fff; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.brand-text { font-size: 16px; font-weight: 700; color: #fff; letter-spacing: -0.3px; }
.collapse-btn { background: none; border: none; color: #64748b; cursor: pointer; padding: 6px; border-radius: 6px; display: flex; align-items: center; justify-content: center; transition: color 0.2s, background 0.2s; flex-shrink: 0; }
.collapse-btn:hover { color: #fff; background: rgba(255,255,255,0.06); }
.collapse-btn.collapsed svg { transform: rotate(180deg); }
.sidebar-nav { flex: 1; padding: 12px 8px; display: flex; flex-direction: column; gap: 2px; overflow-x: hidden; }
.nav-item { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: var(--admin-radius-sm, 8px); color: #8b8fa3; text-decoration: none; font-size: 14px; font-weight: 500; transition: all 0.2s; overflow: hidden; white-space: nowrap; cursor: pointer; }
.nav-item:hover { background: rgba(255,255,255,0.04); color: #d4d6e0; }
.nav-item.active { background: var(--admin-primary, #4361ee); color: #fff; font-weight: 600; }
.nav-icon { display: flex; align-items: center; justify-content: center; width: 20px; height: 20px; flex-shrink: 0; }
.nav-icon :deep(svg) { width: 20px; height: 20px; }
.sidebar-footer { padding: 12px 8px; border-top: 1px solid rgba(255,255,255,0.06); }
.back-link { display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-radius: var(--admin-radius-sm, 8px); color: #64748b; text-decoration: none; font-size: 13px; transition: color 0.2s, background 0.2s; cursor: pointer; overflow: hidden; white-space: nowrap; }
.back-link:hover { color: #c8cad8; background: rgba(255,255,255,0.04); }
.admin-main-area { flex: 1; margin-left: var(--admin-sidebar-width, 240px); min-height: 100vh; display: flex; flex-direction: column; transition: margin-left 0.25s ease; }
.admin-main-area.sidebar-collapsed { margin-left: var(--admin-sidebar-collapsed-width, 64px); }
.admin-topbar { height: var(--admin-topbar-height, 60px); background: #fff; border-bottom: 1px solid var(--admin-border, #e8ecf1); display: flex; align-items: center; justify-content: space-between; padding: 0 24px; position: sticky; top: 0; z-index: 50; }
.topbar-left { display: flex; align-items: center; gap: 16px; }
.topbar-right { display: flex; align-items: center; gap: 16px; }
.breadcrumb { display: flex; align-items: center; gap: 6px; font-size: 14px; }
.breadcrumb-item { color: var(--admin-text-muted, #94a3b8); text-decoration: none; transition: color 0.2s; }
.breadcrumb-item:hover { color: var(--admin-primary, #4361ee); }
.breadcrumb-sep { color: #d4d6e0; }
.breadcrumb-current { color: var(--admin-text, #1a1a2e); font-weight: 600; }
.hamburger-btn { display: none; background: none; border: none; color: var(--admin-text, #1a1a2e); cursor: pointer; padding: 6px; border-radius: 6px; align-items: center; justify-content: center; transition: background 0.2s; }
.hamburger-btn:hover { background: #f5f7fa; }
.user-menu { display: flex; align-items: center; gap: 10px; cursor: default; }
.user-avatar { width: 34px; height: 34px; border-radius: 50%; background: linear-gradient(135deg, var(--admin-primary, #4361ee), #7c3aed); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700; }
.user-name { font-size: 14px; font-weight: 500; color: var(--admin-text, #1a1a2e); }
.logout-btn { background: none; border: 1px solid var(--admin-border, #e8ecf1); color: var(--admin-text-muted, #94a3b8); cursor: pointer; padding: 6px; border-radius: var(--admin-radius-sm, 8px); display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.logout-btn:hover { color: var(--admin-danger, #ef4444); border-color: #fecaca; background: var(--admin-danger-light, #fef2f2); }
.admin-content { flex: 1; padding: 24px 28px; }
.sidebar-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 99; backdrop-filter: blur(2px); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
@media (max-width: 767px) {
  .admin-sidebar { transform: translateX(-100%); transition: transform 0.3s ease; z-index: 101; width: var(--admin-sidebar-width, 240px) !important; }
  .admin-sidebar.is-visible { transform: translateX(0); }
  .admin-main-area { margin-left: 0 !important; }
  .admin-content { padding: 16px; }
  .admin-topbar { padding: 0 16px; }
  .hamburger-btn { display: flex; }
  .user-name { display: none; }
}
</style>
