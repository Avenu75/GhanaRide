import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { User } from '@/types'

interface AuthState {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  setAuth: (user: User, token: string, refreshToken?: string) => void
  setUser: (user: User) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      setAuth: (user, token, refreshToken) => {
        if (typeof window !== 'undefined') {
          localStorage.setItem('ghanaride_token', token)
          localStorage.setItem('ghanaride_user', JSON.stringify(user))
          if (refreshToken) {
            localStorage.setItem('ghanaride_refresh', refreshToken)
          }
        }
        set({ user, token, isAuthenticated: true })
      },
      setUser: (user) => {
        if (typeof window !== 'undefined') {
          localStorage.setItem('ghanaride_user', JSON.stringify(user))
        }
        set({ user })
      },
      logout: () => {
        if (typeof window !== 'undefined') {
          localStorage.removeItem('ghanaride_token')
          localStorage.removeItem('ghanaride_user')
          localStorage.removeItem('ghanaride_refresh')
          document.cookie = 'ghanaride_refresh=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;'
        }
        set({ user: null, token: null, isAuthenticated: false })
      },
    }),
    {
      name: 'ghanaride-auth-storage',
      partialize: (state) => ({ user: state.user, token: state.token, isAuthenticated: state.isAuthenticated }),
    }
  )
)
