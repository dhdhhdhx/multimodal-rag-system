<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useRouter } from 'vue-router'
import { User, Lock, Message, Compass, Postcard } from '@element-plus/icons-vue'

const router = useRouter()

const registerForm = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  fullName: ''
})

const loading = ref(false)

const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.email || !registerForm.value.password) {
    ElMessage.warning('请填写所有必填字段')
    return
  }
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  if (registerForm.value.password.length < 6) {
    ElMessage.warning('密码长度至少6位')
    return
  }
  loading.value = true
  try {
    const { confirmPassword, ...data } = registerForm.value
    const response = await api.post('/auth/register', data)
    localStorage.setItem('jwt_token', response.data.token)
    localStorage.setItem('user_info', JSON.stringify(response.data.user))
    ElMessage.success('注册成功！欢迎加入')
    router.push('/')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '注册失败，用户名或邮箱可能已存在')
  } finally {
    loading.value = false
  }
}

const goToLogin = () => { router.push('/login') }
</script>

<template>
  <div class="register-page">
    <div class="register-left">
      <div class="left-content">
        <div class="logo-icon">
          <el-icon :size="32"><Compass /></el-icon>
        </div>
        <h1 class="left-title">加入我们</h1>
        <p class="left-desc">创建账号，开启智能知识管理之旅</p>
        <div class="feature-list">
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>上传多种格式文件构建个人知识库</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>AI 智能问答，快速获取精准答案</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>探索发现社区共享的优质知识</span>
          </div>
        </div>
      </div>
    </div>
    <div class="register-right">
      <div class="register-card">
        <h2 class="card-title">创建账号</h2>
        <p class="card-subtitle">填写以下信息完成注册</p>
        <el-form :model="registerForm" @submit.prevent="handleRegister" class="register-form">
          <div class="form-field">
            <label class="field-label">用户名 <span class="req">*</span></label>
            <el-input v-model="registerForm.username" placeholder="请输入用户名" :prefix-icon="User" size="large" />
          </div>
          <div class="form-field">
            <label class="field-label">邮箱 <span class="req">*</span></label>
            <el-input v-model="registerForm.email" type="email" placeholder="请输入邮箱" :prefix-icon="Message" size="large" />
          </div>
          <div class="form-field">
            <label class="field-label">姓名</label>
            <el-input v-model="registerForm.fullName" placeholder="请输入姓名（可选）" :prefix-icon="Postcard" size="large" />
          </div>
          <div class="form-row">
            <div class="form-field">
              <label class="field-label">密码 <span class="req">*</span></label>
              <el-input v-model="registerForm.password" type="password" placeholder="至少6位" :prefix-icon="Lock" size="large" show-password />
            </div>
            <div class="form-field">
              <label class="field-label">确认密码 <span class="req">*</span></label>
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入" :prefix-icon="Lock" size="large" show-password @keyup.enter="handleRegister" />
            </div>
          </div>
          <el-button type="primary" :loading="loading" @click="handleRegister" size="large" class="register-btn">注 册</el-button>
          <div class="login-link">已有账号？<a @click="goToLogin">立即登录</a></div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-page { min-height: 100vh; display: flex; }
.register-left {
  flex: 1;
  background: linear-gradient(135deg, #064e3b 0%, #065f46 50%, #10b981 100%);
  display: flex; align-items: center; justify-content: center;
  padding: 60px; position: relative; overflow: hidden;
}
.register-left::before {
  content: ''; position: absolute; top: -50%; right: -50%;
  width: 200%; height: 200%;
  background: radial-gradient(circle, rgba(16,185,129,0.15) 0%, transparent 60%);
  animation: float 15s ease-in-out infinite;
}
@keyframes float { 0%,100%{transform:translate(0,0)} 50%{transform:translate(-30px,30px)} }
.left-content { position: relative; z-index: 1; max-width: 420px; }
.logo-icon {
  width: 56px; height: 56px; border-radius: 16px;
  background: rgba(255,255,255,0.15); backdrop-filter: blur(10px);
  display: flex; align-items: center; justify-content: center;
  color: white; margin-bottom: 24px;
}
.left-title { font-size: 36px; font-weight: 800; color: white; margin: 0 0 12px 0; }
.left-desc { font-size: 16px; color: rgba(255,255,255,0.7); margin: 0 0 40px 0; line-height: 1.6; }
.feature-list { display: flex; flex-direction: column; gap: 16px; }
.feature-item { display: flex; align-items: center; gap: 12px; color: rgba(255,255,255,0.85); font-size: 14px; }
.feature-dot { width: 8px; height: 8px; border-radius: 50%; background: #6ee7b7; flex-shrink: 0; }
.register-right {
  width: 520px; display: flex; align-items: center; justify-content: center;
  padding: 60px; background: #fff;
}
.register-card { width: 100%; max-width: 420px; }
.card-title { font-size: 28px; font-weight: 700; color: #1e293b; margin: 0 0 8px 0; }
.card-subtitle { font-size: 14px; color: #64748b; margin: 0 0 28px 0; }
.register-form { display: flex; flex-direction: column; gap: 16px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-row { display: flex; gap: 12px; }
.form-row .form-field { flex: 1; }
.field-label { font-size: 13px; font-weight: 500; color: #64748b; }
.req { color: #f87171; }
.register-btn {
  width: 100%; height: 44px; font-size: 15px; font-weight: 600;
  border-radius: 10px; margin-top: 4px;
  background: linear-gradient(135deg, #10b981, #059669) !important; border: none !important;
}
.login-link { text-align: center; font-size: 13px; color: #64748b; }
.login-link a { color: #10b981; cursor: pointer; text-decoration: none; font-weight: 500; }
.login-link a:hover { text-decoration: underline; }
@media (max-width: 900px) { .register-left { display: none; } .register-right { width: 100%; } }
</style>
