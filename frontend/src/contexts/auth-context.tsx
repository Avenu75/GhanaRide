'use client'
import { createContext, useContext, useEffect, useState } from 'react'
import { useAuthStore } from '@/store/auth.store'
import { apiClient } from '@/lib/api/client'
import { User } from '@/types'

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  hasRole: (roles: string[]) => boolean
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const { user, isAuthenticated, setAuth, logout: storeLogout, setUser } = useAuthStore()
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const init = async () => {
      const token = localStorage.getItem('ghanaride_token')
      if (token && !user) {
        try {
          const res = await apiClient.get('/auth/me')
          const u = res.data.data || res.data
          setUser(u)
        } catch {
          storeLogout()
        }
      }
      setIsLoading(false)
    }
    init()
  }, [])

  const login = async (email: string, password: string) => {
    const res = await apiClient.post('/auth/login', { email, password })
    const data = res.data.data || res.data
    setAuth(data.user, data.token, data.refreshToken)
  }

  const logout = () => {
    storeLogout()
    window.location.href = '/login'
  }

  const hasRole = (roles: string[]) => {
    if (!user) return false
    return roles.includes(user.role)
  }

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, isLoading, login, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
