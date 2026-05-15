<template>
  <div class="app-layout">
    <header class="top-nav">
      <div class="nav-inner">
        <router-link to="/" class="nav-brand">
          <span class="brand-icon">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2a6 6 0 0 1 6 6c0 1.5-.5 2.9-1.4 4l1.9 6H5.5l1.9-6A5.97 5.97 0 0 1 6 8a6 6 0 0 1 6-6z"/>
              <path d="M9 20h6"/>
              <path d="M10 22h4"/>
              <circle cx="9" cy="8" r="0.8" fill="currentColor" stroke="none"/>
              <circle cx="15" cy="8" r="0.8" fill="currentColor" stroke="none"/>
              <circle cx="12" cy="5.5" r="0.8" fill="currentColor" stroke="none"/>
              <line x1="9.8" y1="8" x2="11.2" y2="5.5"/>
              <line x1="14.2" y1="8" x2="12.8" y2="5.5"/>
              <line x1="9.8" y1="8" x2="14.2" y2="8"/>
            </svg>
          </span>
          <span class="brand-text">多模态RAG知识库</span>
        </router-link>
        <nav class="nav-links">
          <router-link to="/" exact-active-class="active">首页</router-link>
          <router-link to="/topics" active-class="active">话题</router-link>
          <router-link to="/tags" active-class="active">标签</router-link>
          <router-link to="/search" active-class="active">搜索</router-link>
          <router-link to="/ai" class="ai-link" active-class="active">AI 问答</router-link>
        </nav>
        <div class="nav-right">
          <template v-if="isAuthenticated">
            <router-link to="/manage" class="manage-link" active-class="active">管理</router-link>
            <el-dropdown trigger="click" @command="handleCommand">
              <div class="user-avatar">
                <span class="avatar-circle">{{ username.charAt(0).toUpperCase() }}</span>
                <span class="avatar-name">{{ username }}</span>
                <span class="role-badge" :class="{ admin: isAdmin }">{{ roleLabel }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="isAdmin" command="admin">系统管理</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <router-link v-else to="/login" class="login-link">登录</router-link>
        </div>
        <button class="hamburger-btn" @click="showMobileMenu = !showMobileMenu" aria-label="打开菜单">
          <el-icon :size="22"><MenuIcon /></el-icon>
        </button>
      </div>
    </header>
    <!-- Mobile Menu Overlay + Slide Panel -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showMobileMenu" class="mobile-overlay" @click="closeMobileMenu"></div>
      </Transition>
      <Transition name="slide">
        <div v-if="showMobileMenu" class="mobile-panel">
          <div class="mobile-panel-header">
            <span class="brand-text">多模态RAG知识库</span>
            <button class="mobile-close-btn" @click="closeMobileMenu" aria-label="关闭菜单">
              <el-icon :size="20"><MenuIcon /></el-icon>
            </button>
          </div>
          <nav class="mobile-nav-links">
            <router-link to="/" exact-active-class="active" @click="closeMobileMenu">首页</router-link>
            <router-link to="/topics" active-class="active" @click="closeMobileMenu">话题</router-link>
            <router-link to="/tags" active-class="active" @click="closeMobileMenu">标签</router-link>
            <router-link to="/search" active-class="active" @click="closeMobileMenu">搜索</router-link>
            <router-link to="/ai" class="ai-link" active-class="active" @click="closeMobileMenu">AI 问答</router-link>
            <router-link v-if="isAuthenticated" to="/manage" class="manage-link" active-class="active" @click="closeMobileMenu">管理</router-link>
          </nav>
          <div class="mobile-panel-footer">
            <template v-if="isAuthenticated">
              <div class="mobile-user-info">
                <span class="avatar-circle">{{ username.charAt(0).toUpperCase() }}</span>
                <span class="avatar-name">{{ username }}</span>
                <span class="role-badge" :class="{ admin: isAdmin }">{{ roleLabel }}</span>
              </div>
              <button v-if="isAdmin" class="mobile-action-btn" @click="handleCommand('admin')">系统管理</button>
              <button class="mobile-action-btn mobile-action-btn--logout" @click="handleCommand('logout')">退出登录</button>
            </template>
            <router-link v-else to="/login" class="login-link" @click="closeMobileMenu">登录</router-link>
          </div>
        </div>
      </Transition>
    </Teleport>
    <main class="page-container">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Menu as MenuIcon } from '@element-plus/icons-vue'
import { clearTokens, getAccessToken } from '../api'
import { getRoleLabel, isAdmin as checkIsAdmin } from '../utils/auth'

const showMobileMenu = ref(false)

const router = useRouter()

const isAuthenticated = computed(() => !!getAccessToken())

const username = computed(() => {
  try {
    const info = JSON.parse(localStorage.getItem('user_info') || '{}')
    return info.username || ''
  } catch { return '' }
})

const isAdmin = computed(() => checkIsAdmin())

const roleLabel = computed(() => getRoleLabel())

const handleCommand = (cmd: string) => {
  if (cmd === 'logout') {
    clearTokens()
    router.push('/login')
  } else if (cmd === 'admin') {
    router.push('/admin')
  }
  showMobileMenu.value = false
}

const closeMobileMenu = () => {
  showMobileMenu.value = false
}
</script>

<style scoped>
.app-layout { min-height: 100vh; }

.top-nav {
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
}
.nav-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.nav-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: var(--text-primary);
  font-weight: 700;
  font-size: 16px;
}
.brand-icon {
  font-size: 24px;
  display: flex;
  align-items: center;
  color: var(--accent);
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nav-links a {
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  padding: 6px 16px;
  border-radius: 6px;
  transition: all 0.2s;
}
.nav-links a:hover { color: var(--text-primary); background: var(--bg-secondary); }
.nav-links a.active { color: var(--accent); font-weight: 600; }
.ai-link {
  border: 1.5px solid var(--accent) !important;
  border-radius: 20px !important;
  color: var(--accent) !important;
  padding: 4px 16px !important;
}
.ai-link.active {
  background: var(--accent) !important;
  color: #fff !important;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.manage-link {
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.2s;
}
.manage-link:hover { color: var(--text-primary); }
.manage-link.active { color: var(--accent); font-weight: 600; }
.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.avatar-circle {
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
.avatar-name { font-size: 14px; color: var(--text-primary); font-weight: 500; }
.role-badge {
  font-size: 11px;
  line-height: 1;
  padding: 5px 8px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.14);
  color: #059669;
  border: 1px solid rgba(16, 185, 129, 0.24);
}
.role-badge.admin {
  background: rgba(245, 158, 11, 0.14);
  color: #d97706;
  border-color: rgba(245, 158, 11, 0.24);
}
.login-link {
  text-decoration: none;
  color: var(--accent);
  font-size: 14px;
  font-weight: 600;
  padding: 6px 16px;
  border: 1.5px solid var(--accent);
  border-radius: 20px;
  transition: all 0.2s;
}
.login-link:hover { background: var(--accent); color: #fff; }

.page-container {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 40px 24px;
}

/* Hamburger button */
.hamburger-btn {
  display: none;
  align-items: center;
  justify-content: center;
  background: none;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 6px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
}
.hamburger-btn:hover {
  color: var(--text-primary);
  background: var(--bg-secondary);
}

/* Mobile overlay */
.mobile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: var(--z-overlay);
}

/* Mobile slide panel */
.mobile-panel {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 280px;
  max-width: 80vw;
  background: var(--bg-primary);
  z-index: calc(var(--z-overlay) + 1);
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.15);
}
.mobile-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}
.mobile-close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 6px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
}
.mobile-close-btn:hover {
  color: var(--text-primary);
  background: var(--bg-secondary);
}

/* Mobile nav links */
.mobile-nav-links {
  display: flex;
  flex-direction: column;
  padding: 12px 0;
  flex: 1;
}
.mobile-nav-links a {
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 500;
  padding: 12px 20px;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}
.mobile-nav-links a:hover {
  color: var(--text-primary);
  background: var(--bg-secondary);
}
.mobile-nav-links a.active {
  color: var(--accent);
  font-weight: 600;
  border-left-color: var(--accent);
  background: var(--bg-secondary);
}
.mobile-nav-links .ai-link {
  border: none !important;
  border-radius: 0 !important;
  border-left: 3px solid transparent !important;
  padding: 12px 20px !important;
}
.mobile-nav-links .ai-link.active {
  border-left-color: var(--accent) !important;
}
.mobile-nav-links .manage-link {
  border-left: 3px solid transparent;
}

/* Mobile panel footer */
.mobile-panel-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.mobile-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.mobile-action-btn {
  display: block;
  width: 100%;
  padding: 10px 16px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  text-align: center;
  transition: all 0.2s;
}
.mobile-action-btn:hover {
  background: var(--bg-tertiary, var(--bg-secondary));
}
.mobile-action-btn--logout {
  color: #ef4444;
  border-color: #fecaca;
}
.mobile-action-btn--logout:hover {
  background: #fef2f2;
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}
.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}

/* Responsive */
@media (max-width: 767px) {
  .nav-links,
  .nav-right {
    display: none;
  }
  .hamburger-btn {
    display: flex;
  }
  .mobile-panel .login-link {
    display: block;
    text-align: center;
    padding: 10px 16px;
    border-radius: 8px;
  }
}
</style>
