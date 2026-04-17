import { getAccessToken, clearTokens } from '../api'

export interface User {
    id: number
    username: string
    email: string
    fullName: string
    isActive: boolean
    roles: string[]
}

export interface Permission {
    id: number
    name: string
    resource: string
    action: string
}

export const getCurrentUser = (): User | null => {
    const userInfo = localStorage.getItem('user_info')
    if (!userInfo) return null
    try {
        return JSON.parse(userInfo)
    } catch {
        return null
    }
}

export const getToken = (): string | null => {
    return getAccessToken()
}

export const hasRole = (roleName: string): boolean => {
    const user = getCurrentUser()
    if (!user || !user.roles) return false

    return user.roles.some((role: any) => {
        if (typeof role === 'string') return role === roleName
        return role.name === roleName
    })
}

export const isAdmin = (): boolean => {
    return hasRole('ADMIN')
}

export const isPremium = (): boolean => {
    return hasRole('PREMIUM')
}

export const getPrimaryRole = (): string => {
    const user = getCurrentUser()
    if (!user || !user.roles || user.roles.length === 0) return 'USER'
    const firstRole = user.roles[0] as any
    return typeof firstRole === 'string' ? firstRole : (firstRole.name || 'USER')
}

export const getRoleLabel = (): string => {
    const role = getPrimaryRole()
    if (role === 'ADMIN') return '管理员'
    if (role === 'PREMIUM') return '高级会员'
    return '普通会员'
}

export const hasPermission = (_permissionName: string): boolean => {
    const user = getCurrentUser()
    if (!user || !user.roles) return false
    if (user.roles.includes('ADMIN')) return true
    return false
}

export const logout = () => {
    clearTokens()
}
