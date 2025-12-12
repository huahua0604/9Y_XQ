import { defineStore } from 'pinia'

const STORAGE_KEY = 'auth/v1'
const LOGOUT_BROADCAST_KEY = 'auth/logout-broadcast'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    user: null,
    exp: 0,                 
    mustChangePassword: false,
    _boundStorageListener: false,
  }),

  getters: {
    isAuthed: (s) => !!s.token && (!s.exp || s.exp * 1000 > Date.now()),
    roles:   (s) => Array.isArray(s?.user?.roles) ? s.user.roles : [],
    employeeId: (s) => s.user?.employeeId || '',
    displayName: (s) => s.user?.name || ''
  },

  actions: {
    hydrate () {
      try {
        const raw = localStorage.getItem(STORAGE_KEY)
        if (raw) {
          const data = JSON.parse(raw)
          this.token = data.token || ''
          this.user  = data.user  || null
          this.exp   = data.exp   || 0
          this.mustChangePassword = !!data.mustChangePassword
        }
      } catch (e) {
        console.warn('auth hydrate failed', e)
      }
      if (this.exp && this.exp * 1000 <= Date.now()) this.clear()
      this._bindCrossTabLogout()
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
      this.mustChangePassword = !!(opts?.mustChangePassword)
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
      try { localStorage.removeItem(STORAGE_KEY) } catch {}
    },

    logout () {
      this.clear()
      try {
        localStorage.setItem(LOGOUT_BROADCAST_KEY, String(Date.now()))
        localStorage.removeItem(LOGOUT_BROADCAST_KEY)
      } catch {}
    },

    hasAuthority (role) {
      return this.roles.includes(role)
    },

    /** 内部：跨标签页同步登出 */
    _bindCrossTabLogout () {
      if (this._boundStorageListener) return
      if (typeof window === 'undefined') return
      const handler = (e) => {
        if (e.key === LOGOUT_BROADCAST_KEY) {
          this.clear()
        }
      }
      window.addEventListener('storage', handler)
      this._boundStorageListener = true
    },
  }
})

function persist (state) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      token: state.token,
      user: state.user,
      exp: state.exp,
      mustChangePassword: state.mustChangePassword
    }))
  } catch {}
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
    roles,
    phone: u.phone || u.mobile || '',
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
