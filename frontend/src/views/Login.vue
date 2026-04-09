<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import api, { setTokens } from '../api'
import { useRouter } from 'vue-router'
import { User, Lock, Compass } from '@element-plus/icons-vue'

const router = useRouter()
const loginForm = ref({ username: '', password: '' })
const loading = ref(false)

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const response = await api.post('/auth/login', loginForm.value)
    const { token, refreshToken, expiresIn, user } = response.data
    setTokens(token, refreshToken, expiresIn || 3600)
    localStorage.setItem('user_info', JSON.stringify(user))
    ElMessage.success(`欢迎回来，${user.fullName || user.username}！`)
    router.push('/')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

const goToRegister = () => router.push('/register')
</script>

<template>
  <div class="login-page">
    <div class="login-left">
      <div class="left-content">
        <div class="logo-icon">
          <el-icon :size="32"><Compass /></el-icon>
        </div>
        <h1 class="left-title">多模态RAG知识库</h1>
        <p class="left-desc">智能知识管理与多模态检索增强生成平台</p>
        <div class="feature-list">
          <div class="feature-item"><div class="feature-dot"></div><span>支持文本、图像、音频、视频多模态检索</span></div>
          <div class="feature-item"><div class="feature-dot"></div><span>AI 驱动的智能问答与知识发现</span></div>
          <div class="feature-item"><div class="feature-dot"></div><span>安全可靠的个人知识库管理</span></div>
        </div>
      </div>
    </div>
    <div class="login-right">
      <div class="login-card">
        <h2 class="card-title">欢迎回来</h2>
        <p class="card-subtitle">登录您的账号以继续</p>
        <el-form :model="loginForm" @submit.prevent="handleLogin" class="login-form">
          <div class="form-field">
            <label for="login-username">用户名</label>
            <el-input id="login-username" v-model="loginForm.username" placeholder="请输入用户名" :prefix-icon="User" size="large" />
          </div>
          <div class="form-field">
            <label for="login-password">密码</label>
            <el-input id="login-password" v-model="loginForm.password" type="password" placeholder="请输入密码"
              :prefix-icon="Lock" size="large" show-password @keyup.enter="handleLogin" />
          </div>
          <el-button type="primary" :loading="loading" @click="handleLogin" size="large" class="login-btn">登 录</el-button>
          <div class="register-link">还没有账号？<router-link to="/register">立即注册</router-link></div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page { min-height: 100vh; display: flex; }
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #064e3b 0%, #065f46 50%, #10b981 100%);
  display: flex; align-items: center; justify-content: center;
  padding: 60px; position: relative; overflow: hidden;
}
.login-left::before {
  content: ''; position: absolute; top: -50%; left: -50%; width: 200%; height: 200%;
  background: radial-gradient(circle, rgba(16,185,129,0.15) 0%, transparent 60%);
  animation: float 15s ease-in-out infinite;
}
@keyframes float { 0%,100%{transform:translate(0,0)} 50%{transform:translate(30px,-30px)} }
.left-content { position: relative; z-index: 1; max-width: 420px; }
.logo-icon {
  width: 56px; height: 56px; border-radius: 16px;
  background: rgba(255,255,255,0.15); backdrop-filter: blur(10px);
  display: flex; align-items: center; justify-content: center; color: white; margin-bottom: 24px;
}
.left-title { font-size: 36px; font-weight: 800; color: white; margin: 0 0 12px; }
.left-desc { font-size: 16px; color: rgba(255,255,255,0.7); margin: 0 0 40px; line-height: 1.6; }
.feature-list { display: flex; flex-direction: column; gap: 16px; }
.feature-item { display: flex; align-items: center; gap: 12px; color: rgba(255,255,255,0.85); font-size: 14px; }
.feature-dot { width: 8px; height: 8px; border-radius: 50%; background: #6ee7b7; flex-shrink: 0; }
.login-right {
  width: 480px; display: flex; align-items: center; justify-content: center;
  padding: 60px; background: #fff;
}
.login-card { width: 100%; max-width: 360px; }
.card-title { font-size: 28px; font-weight: 700; color: #1e293b; margin: 0 0 8px; }
.card-subtitle { font-size: 14px; color: #64748b; margin: 0 0 32px; }
.login-form { display: flex; flex-direction: column; gap: 20px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-field label { font-size: 13px; font-weight: 500; color: #64748b; }
.login-btn {
  width: 100%; height: 44px; font-size: 15px; font-weight: 600; border-radius: 10px; margin-top: 4px;
  background: linear-gradient(135deg, #10b981, #059669) !important; border: none !important;
}
.register-link { text-align: center; font-size: 13px; color: #64748b; }
.register-link a { color: #10b981; cursor: pointer; text-decoration: none; font-weight: 500; }
.register-link a:hover { text-decoration: underline; }
.register-link a:visited { color: #10b981; }
@media (max-width: 900px) { .login-left { display: none; } .login-right { width: 100%; } }
</style>
