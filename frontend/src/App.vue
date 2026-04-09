<template>
  <router-view />
  <!-- Global session expired dialog -->
  <el-dialog
    v-model="sessionExpiredVisible"
    title="登录已过期"
    width="400px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    center
  >
    <div style="text-align: center; padding: 10px 0 20px;">
      <p style="font-size: 15px; color: var(--text-secondary); margin: 0 0 6px;">
        您的登录已过期，请重新登录以继续操作。
      </p>
      <p style="font-size: 13px; color: var(--text-muted); margin: 0;">
        您当前的数据已自动保存，登录后可继续使用。
      </p>
    </div>
    <template #footer>
      <el-button type="primary" @click="goToLogin" size="large" style="width: 100%;">
        去登录
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { setOnSessionExpired } from './api'

const router = useRouter()
const sessionExpiredVisible = ref(false)

const goToLogin = () => {
  sessionExpiredVisible.value = false
  router.push('/login')
}

// Register global session expired handler
setOnSessionExpired(() => {
  sessionExpiredVisible.value = true
})
</script>
