import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const routes = [
  { path: '/', redirect: '/my' },

  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/change-password', name: 'changePwd', component: () => import('../views/ChangePassword.vue') },
  { path: '/me', name: 'me', component: () => import('../views/UserCenter.vue') },
  { path: '/admin/users', name: 'admin-users', component: () => import('../views/Users.vue'), meta: { roles: ['ADMIN'] } },
  { path: '/my', name: 'my', component: () => import('../views/DemandList.vue') },
  { path: '/demands', name: 'demands', component: () => import('../views/DemandList.vue') },
  { path: '/demands/new', name: 'demand-new', component: () => import('../views/DemandForm.vue') },
  { path: '/demands/:id', name: 'demand-detail', component: () => import('../views/DemandDetail.vue') },
  { path: '/inbox', name: 'inbox', component: () => import('../views/Inbox.vue'), meta: { roles: ['ADMIN', 'REVIEWER'] } }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!auth.isAuthed && typeof auth.hydrate === 'function') {
    try { auth.hydrate() } catch (_) {}
  }

  if (to.meta.public) {
    if (to.name === 'login' && auth.isAuthed) {
      const back = (to.query?.redirect && String(to.query.redirect)) || '/my'
      return { path: back }
    }
    return true
  }

  if (!auth.isAuthed) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (auth.mustChangePassword && to.name !== 'changePwd') {
    return { name: 'changePwd' }
  }

  if (to.meta.roles) {
    const ok = to.meta.roles.some(r => (auth.roles || []).includes(r))
    if (!ok) return { path: '/403' }
  }

  return true
})

export default router