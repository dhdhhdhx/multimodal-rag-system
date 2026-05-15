import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

const api = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json'
    }
})

// ======================== Token Storage Helpers ========================

const TOKEN_KEY = 'jwt_token'
const REFRESH_TOKEN_KEY = 'jwt_refresh_token'
const TOKEN_EXPIRY_KEY = 'jwt_token_expiry'

/** How many seconds before actual expiry to trigger proactive refresh */
const REFRESH_BUFFER_SECONDS = 60

export function getAccessToken(): string | null {
    return localStorage.getItem(TOKEN_KEY)
}

export function getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setTokens(accessToken: string, refreshToken: string, expiresInSec: number) {
    localStorage.setItem(TOKEN_KEY, accessToken)
    if (refreshToken) {
        localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
    }
    // Store absolute expiry timestamp for proactive refresh
    const expiryMs = Date.now() + (expiresInSec * 1000)
    localStorage.setItem(TOKEN_EXPIRY_KEY, String(expiryMs))
}

export function clearTokens() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(TOKEN_EXPIRY_KEY)
    localStorage.removeItem('user_info')
}

/** Check if the access token is (almost) expired and needs refresh */
export function isTokenExpiringSoon(): boolean {
    const expiry = localStorage.getItem(TOKEN_EXPIRY_KEY)
    if (!expiry) return false
    const remaining = Number(expiry) - Date.now()
    return remaining <= REFRESH_BUFFER_SECONDS * 1000
}

// ======================== Refresh Token Logic ========================

/** Track in-flight refresh to avoid concurrent refresh requests */
let refreshPromise: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
    const refreshToken = getRefreshToken()
    if (!refreshToken) return null

    try {
        const res = await axios.post('/api/auth/refresh', { refreshToken })
        const data = res.data
        if (data.token) {
            setTokens(
                data.token,
                data.refreshToken || refreshToken, // reuse old refresh token if new one not provided
                data.expiresIn || 3600
            )
            return data.token
        }
    } catch {
        // Refresh failed — token is beyond recovery
    }
    return null
}

/** Get or create a pending refresh promise (deduplication) */
function getOrRefreshToken(): Promise<string | null> {
    if (!refreshPromise) {
        refreshPromise = refreshAccessToken().finally(() => {
            refreshPromise = null
        })
    }
    return refreshPromise
}

// ======================== Session Expired Handler ========================

type SessionExpiredCallback = () => void
let onSessionExpired: SessionExpiredCallback | null = null

/**
 * Register a callback for when the session truly expires
 * (refresh token also fails). The app should use this to show
 * a user-friendly dialog instead of a hard redirect.
 */
export function setOnSessionExpired(callback: SessionExpiredCallback) {
    onSessionExpired = callback
}

function handleSessionExpired() {
    clearTokens()
    if (onSessionExpired) {
        onSessionExpired()
    } else {
        // Fallback: hard redirect
        window.location.href = '/login'
    }
}

// ======================== Request Interceptor ========================

api.interceptors.request.use(
    async (config: InternalAxiosRequestConfig) => {
        // Skip auth header for refresh endpoint itself
        if (config.url === '/auth/refresh') {
            return config
        }

        let token = getAccessToken()

        // Proactive refresh: if token is about to expire, refresh before sending
        if (token && isTokenExpiringSoon()) {
            const newToken = await getOrRefreshToken()
            if (newToken) {
                token = newToken
            }
        }

        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }

        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// ======================== Response Interceptor ========================

api.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

        // Only attempt refresh on 401 responses
        if (error.response?.status !== 401) {
            return Promise.reject(error)
        }

        // Don't retry refresh endpoint itself
        if (originalRequest.url === '/auth/refresh') {
            handleSessionExpired()
            return Promise.reject(error)
        }

        // Don't retry if already retried
        if (originalRequest._retry) {
            handleSessionExpired()
            return Promise.reject(error)
        }

        originalRequest._retry = true

        // Attempt to refresh the token
        const newToken = await refreshAccessToken()
        if (newToken) {
            // Retry the original request with the new token
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            return api(originalRequest)
        }

        // Refresh failed — session is truly expired
        handleSessionExpired()
        return Promise.reject(error)
    }
)

// ======================== Topic API Helpers ========================

export const topicApi = {
  /** 获取话题详情 */
  getDetail: (id: number) => api.get(`/topics/${id}`),

  /** 获取话题下的所有文档 */
  getDocuments: (id: number) => api.get(`/topics/${id}/documents`),

  /** 订阅话题 */
  subscribe: (id: number) => api.post(`/topics/${id}/subscribe`),

  /** 取消订阅话题 */
  unsubscribe: (id: number) => api.delete(`/topics/${id}/subscribe`),

  /** 获取订阅状态 */
  getSubscriptionStatus: (id: number) => api.get(`/topics/${id}/subscription-status`),

  /** 分页获取话题文档 */
  getDocumentsPaged: (id: number, params: { keyword?: string; page?: number; size?: number }) =>
    api.get(`/topics/${id}/documents/paged`, { params }),

  /** 获取推荐话题 */
  getRecommended: (limit = 6) => api.get('/topics/recommended', { params: { limit } }),

  /** 获取热门话题 */
  getHot: (limit = 6) => api.get('/topics/hot', { params: { limit } }),

  /** 分页获取公开话题 */
  getPublicPaged: (params: { keyword?: string; page?: number; size?: number }) =>
    api.get('/topics/public/paged', { params }),

  /** 获取我的订阅列表 */
  getMySubscriptions: () => api.get('/topics/subscribed'),
}

// ======================== Export ========================

export default api