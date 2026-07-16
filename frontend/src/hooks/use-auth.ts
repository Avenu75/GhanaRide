
'use client'
import { useAuthStore } from '@/store/auth.store'
export function useAuth() { const store = useAuthStore(); return { user: store.user, isAuthenticated: store.isAuthenticated, hasRole: (roles:string[]) => store.user ? roles.includes(store.user.role) : false } }
