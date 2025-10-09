import axios from 'axios'
import { useAuthStore } from '../store/auth'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:6060',
  timeout: 15000
})

http.interceptors.request.use(cfg => {
  try {
    const auth = useAuthStore()
    if (auth?.token) cfg.headers.Authorization = `Bearer ${auth.token}`
  } catch (_) {
  }
  return cfg
})

http.interceptors.response.use(
  r => r,
  async e => {
    const res = e?.response
    const cfg = e?.config || {}
    const method = (cfg.method || 'GET').toUpperCase()
    const url = cfg.url || ''
    const status = res?.status || 0

    let data = res?.data
    if (data instanceof Blob) {
      try { data = JSON.parse(await data.text()) } catch {}
    }

    const msg = (data && (data.message || data.error || data.msg)) || e.message || '请求失败'
    const errId = data?.errorId ? `（错误号: ${data.errorId}）` : ''

    console.error(`[API ${method} ${url}] ${status}`, data || e)

    if (status === 401) {
      try {
        const auth = useAuthStore()
        if (typeof auth.clear === 'function') auth.clear()
        else if (typeof auth.logout === 'function') auth.logout()
      } catch {}
      const back = encodeURIComponent(location.pathname + location.search)
      location.assign(`/login?redirect=${back}`)
    } else if (status === 403) {
      alert(`无权限访问：${method} ${url}。${errId}`)
    } else if (status >= 500) {
      alert(`服务器内部错误。${errId}`)
    } else {
      alert(`${msg}${errId}`)
    }

    return Promise.reject(e)
  }
)

function safeReadToken(){
  try { const { useAuthStore } = require('../store/auth'); const a = useAuthStore(); if (a?.token) return a.token } catch {}
  try { const raw = localStorage.getItem('auth/v1'); if (raw) return JSON.parse(raw).token || '' } catch {}
  return ''
}
http.interceptors.request.use(cfg => {
  const t = safeReadToken()
  if (t) cfg.headers.Authorization = `Bearer ${t}`
  return cfg
})

export default http