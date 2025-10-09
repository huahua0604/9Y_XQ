import { defineStore } from 'pinia'

const STORAGE_KEY = 'auth/v1'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    user: null,                 // { employeeId, name, department, roles: [] }
    exp: 0,                     // JWT 过期时间（unix秒）
    mustChangePassword: false
  }),

  getters: {
    // 路由守卫用：是否已登录（且未过期）
    isAuthed: (s) => !!s.token && (!s.exp || s.exp * 1000 > Date.now()),
    // 路由守卫用：角色数组（直接 auth.roles.includes('ADMIN')）
    roles:   (s) => Array.isArray(s?.user?.roles) ? s.user.roles : [],
    employeeId: (s) => s.user?.employeeId || '',
    displayName: (s) => s.user?.name || ''
  },

  actions: {
    // 刷新/启动时恢复本地会话
    hydrate () {
      try {
        const raw = localStorage.getItem(STORAGE_KEY)
        if (!raw) return
        const data = JSON.parse(raw)
        this.token = data.token || ''
        this.user  = data.user  || null
        this.exp   = data.exp   || 0
        this.mustChangePassword = !!data.mustChangePassword
        // 已过期则清空
        if (this.exp && this.exp * 1000 <= Date.now()) this.clear()
      } catch (e) {
        console.warn('auth hydrate failed', e)
      }
    },

    /**
     * 登录成功后调用
     * @param {string} token JWT
     * @param {object} user  后端返回的用户对象
     * @param {object} opts  { mustChangePassword?: boolean }
     */
    setAuth (token, user, opts = {}) {
      this.token = token || ''
      this.user  = normalizeUser(user)
      const payload = parseJwt(token)
      this.exp   = payload?.exp || 0
      this.mustChangePassword = !!opts.mustChangePassword
      persist(this.$state)
    },

    setUser (user) {
      this.user = normalizeUser(user)
      persist(this.$state)
    },

    setMustChangePassword (flag) {
      this.mustChangePassword = !!flag
      persist(this.$state)
    },

    clear () {
      this.token = ''
      this.user  = null
      this.exp   = 0
      this.mustChangePassword = false
      localStorage.removeItem(STORAGE_KEY)
    },

    hasAuthority (role) {
      return this.roles.includes(role)
    }
  }
})

function persist (state) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({
    token: state.token,
    user: state.user,
    exp: state.exp,
    mustChangePassword: state.mustChangePassword
  }))
}

function normalizeUser (u) {
  if (!u) return null
  const roles = Array.isArray(u.roles)
    ? u.roles.map(r => String(r || '').replace(/^ROLE_/, ''))
    : []
  return {
    employeeId: u.employeeId || u.empId || u.username || '',
    name:       u.name || u.fullName || '',
    department: u.department || u.dept || '',
    roles
  }
}

function parseJwt (token) {
  try {
    const parts = (token || '').split('.')
    if (parts.length !== 3) return null
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(escape(window.atob(base64)))
    return JSON.parse(json)
  } catch {
    return null
  }
}
