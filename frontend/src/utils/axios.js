import axios from 'axios'
import { useAuthStore } from '../store/auth'

function resolveBaseURL() {
  const raw = (import.meta.env.VITE_API_BASE_URL || '').trim()
  if (!raw) return ''
  return raw.replace(/\/+$/, '')
}

const baseURL = resolveBaseURL()

console.log('[Axios] baseURL =', baseURL)

const http = axios.create({
  baseURL,
  timeout: 15000,
})

http.interceptors.request.use(cfg => {
  try {
    const auth = useAuthStore()
    const token =
      auth?.token ??
      (JSON.parse(localStorage.getItem('auth/v1') || '{}').token)
    if (token) cfg.headers.Authorization = `Bearer ${token}`
  } catch (err) {
    console.error('[Axios Request Error]', err)
  }
  return cfg
})

http.interceptors.response.use(
  r => r,
  async err => {
    const res = err?.response
    const status = res?.status || 0

    if (status === 401) {
      try {
        const auth = useAuthStore()
        if (typeof auth?.clear === 'function') auth.clear()
        else if (typeof auth?.logout === 'function') auth.logout()
      } catch {}
      const redirect = encodeURIComponent(location.pathname + location.search)
      if (!location.pathname.startsWith('/login')) {
        location.assign(`/login?redirect=${redirect}`)
      }
      return
    }

    console.error('[Axios Error]', status, res?.data || err)
    return Promise.reject(err)
  }
)

export default http