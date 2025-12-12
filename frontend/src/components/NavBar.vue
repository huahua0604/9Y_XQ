<template>
  <header class="navbar navbar-expand-lg navbar-dark bg-dark shadow-sm sticky-top">
    <div class="container">
      <!-- 品牌 -->
      <router-link to="/" class="navbar-brand fw-semibold">
        上海九院数据提取申请平台
      </router-link>

      <!-- 折叠按钮（用 Vue 控制，不依赖 bootstrap.js） -->
      <button
        class="navbar-toggler"
        type="button"
        aria-controls="mainNav"
        :aria-expanded="navOpen ? 'true' : 'false'"
        aria-label="Toggle navigation"
        @click="navOpen = !navOpen"
      >
        <span class="navbar-toggler-icon"></span>
      </button>

      <!-- 折叠区 -->
      <div
        id="mainNav"
        class="collapse navbar-collapse"
        :class="{ show: navOpen }"
        @click.self="navOpen = false"
      >
        <!-- 左侧菜单 -->
        <ul class="navbar-nav me-auto mb-2 mb-lg-0">
          <li class="nav-item">
            <router-link
              to="/my"
              class="nav-link"
              :class="activeClass('/my')"
              @click="closeNav"
            >我的需求</router-link>
          </li>

          <li class="nav-item" v-if="hasAuthority('USER') || hasAuthority('REVIEWER') || hasAuthority('ADMIN')">
            <router-link
              to="/demands/new"
              class="nav-link"
              :class="activeClass('/demands/new')"
              @click="closeNav"
            >新建需求</router-link>
          </li>

          <li class="nav-item" v-if="hasAuthority('REVIEWER') || hasAuthority('ADMIN')">
            <router-link
              to="/inbox"
              class="nav-link"
              :class="activeClass('/inbox')"
              @click="closeNav"
            >审批收件箱</router-link>
          </li>

          <li class="nav-item" v-if="hasAuthority('ADMIN')">
            <router-link
              to="/admin/users"
              class="nav-link"
              :class="activeClass('/admin/users')"
              @click="closeNav"
            >用户管理</router-link>
          </li>
        </ul>

        <!-- 右侧用户区 -->
        <div class="d-flex align-items-center gap-2 ms-lg-3">
          <!-- 未登录 -->
          <router-link
            v-if="!isAuthed"
            to="/login"
            class="btn btn-outline-light btn-sm"
            @click="closeNav"
          >登录</router-link>

          <!-- 已登录：问候 + 下拉 -->
          <div v-else class="dropdown" :class="{ 'show': userMenuOpen }">
            <button
              class="btn btn-outline-light btn-sm dropdown-toggle"
              type="button"
              id="userMenu"
              :aria-expanded="userMenuOpen ? 'true' : 'false'"
              @click="toggleUserMenu"
            >
              👋 {{ displayName || employeeId }}
            </button>
            <ul class="dropdown-menu dropdown-menu-end" :class="{ show: userMenuOpen }" aria-labelledby="userMenu">
              <!-- 新增：用户中心 -->
              <li>
                <router-link class="dropdown-item" to="/me" @click="closeUserMenu">
                  用户中心
                </router-link>
              </li>
              <li><hr class="dropdown-divider" /></li>
              <li>
                <button class="dropdown-item text-danger" @click="onLogoutAndClose">
                  退出登录
                </button>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const { isAuthed, employeeId, displayName } = storeToRefs(auth)
const hasAuthority = auth.hasAuthority

const navOpen = ref(false)
const userMenuOpen = ref(false)

function activeClass(prefix) {
  return route.path.startsWith(prefix) ? 'active' : ''
}
function closeNav() { navOpen.value = false }
function toggleUserMenu() { userMenuOpen.value = !userMenuOpen.value }
function closeUserMenu() { userMenuOpen.value = false; closeNav() }

function onLogout () {
  if (typeof auth.clear === 'function') auth.clear()
  else if (typeof auth.logout === 'function') auth.logout()
  router.push('/login')
}
function onLogoutAndClose() {
  closeUserMenu()
  onLogout()
}
</script>

<style scoped>
.navbar-brand { letter-spacing: .5px; }
</style>
