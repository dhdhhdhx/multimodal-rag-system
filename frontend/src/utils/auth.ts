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
    return localStorage.getItem('jwt_token')
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

export const hasPermission = (_permissionName: string): boolean => {
    // Note: With string roles, we check for basic admin or specific roles 
    // that imply permissions. For granular permissions, we'd need them in the User JSON.
    const user = getCurrentUser()
    if (!user || !user.roles) return false
    // Temporary fallback: if admin, they have all permissions
    if (user.roles.includes('ADMIN')) return true
    return false
}

export const logout = () => {
    localStorage.removeItem('jwt_token')
    localStorage.removeItem('user_info')
}
