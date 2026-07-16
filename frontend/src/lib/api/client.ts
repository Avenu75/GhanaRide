import axios, { AxiosError, AxiosRequestConfig } from 'axios'
import Cookies from 'js-cookie'

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8088/api'

export const apiClient = axios.create({
  baseURL: API_URL,
  timeout: 15000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
})

// Request interceptor – attach JWT
apiClient.interceptors.request.use((config) => {
  const token = typeof window !== 'undefined' ? localStorage.getItem('ghanaride_token') : null
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor – handle 401 and refresh
apiClient.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean }
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const refreshToken = Cookies.get('ghanaride_refresh') || localStorage.getItem('ghanaride_refresh')
      if (refreshToken) {
        try {
          const resp = await axios.post(`${API_URL}/auth/refresh`, { refreshToken })
          const { token, refreshToken: newRefresh } = resp.data.data || resp.data
          localStorage.setItem('ghanaride_token', token)
          Cookies.set('ghanaride_refresh', newRefresh, { expires: 7, secure: true, sameSite: 'Lax' })
          if (originalRequest.headers) {
            // @ts-ignore
            originalRequest.headers.Authorization = `Bearer ${token}`
          }
          return apiClient(originalRequest)
        } catch (e) {
          // refresh failed – logout
          localStorage.removeItem('ghanaride_token')
          localStorage.removeItem('ghanaride_user')
          Cookies.remove('ghanaride_refresh')
          if (typeof window !== 'undefined') {
            window.location.href = '/login?expired=1'
          }
        }
      }
    }
    return Promise.reject(error)
  }
)

export type ApiError = {
  message: string
  status: number
  errors?: Record<string, string>
}

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as any
    return data?.message || data?.error || error.message || 'Something went wrong'
  }
  if (error instanceof Error) return error.message
  return 'An unexpected error occurred'
}

// Typed wrapper
export async function apiGet<T>(url: string, params?: any): Promise<T> {
  const res = await apiClient.get(url, { params })
  return res.data.data ?? res.data
}

export async function apiPost<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  const res = await apiClient.post(url, data, config)
  return res.data.data ?? res.data
}

export async function apiPut<T>(url: string, data?: any): Promise<T> {
  const res = await apiClient.put(url, data)
  return res.data.data ?? res.data
}

export async function apiDelete<T>(url: string): Promise<T> {
  const res = await apiClient.delete(url)
  return res.data.data ?? res.data
}

export async function apiUpload<T>(url: string, formData: FormData): Promise<T> {
  const res = await apiClient.post(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data.data ?? res.data
}
