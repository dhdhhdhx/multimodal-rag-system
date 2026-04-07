<template>
  <div class="app-layout">
    <header class="top-nav">
      <div class="nav-inner">
        <router-link to="/" class="nav-brand">
          <span class="brand-icon">🧠</span>
          <span class="brand-text">多模态RAG知识库</span>
        </router-link>
        <nav class="nav-links">
          <router-link to="/" exact-active-class="active">首页</router-link>
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
      </div>
    </header>
    <main class="page-container">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const isAuthenticated = computed(() => !!localStorage.getItem('jwt_token'))

const username = computed(() => {
  try {
    const info = JSON.parse(localStorage.getItem('user_info') || '{}')
    return info.username || ''
  } catch { return '' }
})

const isAdmin = computed(() => {
  try {
    const info = JSON.parse(localStorage.getItem('user_info') || '{}')
    return info.roles?.includes('ADMIN') || false
  } catch { return false }
})

const handleCommand = (cmd: string) => {
  if (cmd === 'logout') {
    localStorage.removeItem('jwt_token')
    localStorage.removeItem('user_info')
    router.push('/login')
  } else if (cmd === 'admin') {
    router.push('/admin')
  }
}
</script>

<style scoped>
.app-layout { min-height: 100vh; }

.top-nav {
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
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
.brand-icon { font-size: 24px; }
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
</style>
