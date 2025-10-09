<template>
  <div class="container my-5">
    <div class="card shadow-sm mx-auto" style="max-width: 420px;">
      <div class="card-body">
        <h1 class="h5 mb-3">登录</h1>

        <!-- 接口错误提示 -->
        <div v-if="errorMsg" class="alert alert-danger py-2" role="alert">
          {{ errorMsg }}
        </div>

        <form @submit.prevent="onLogin" novalidate>
          <!-- 工号 -->
          <div class="mb-3">
            <label class="form-label">工号</label>
            <input
              class="form-control"
              v-model.trim="employeeId"
              required
              autofocus
              placeholder="请输入工号"
            />
          </div>

          <!-- 密码（显示/隐藏） -->
          <div class="mb-3">
            <label class="form-label">密码</label>
            <div class="input-group">
              <input
                :type="showPwd ? 'text' : 'password'"
                class="form-control"
                v-model="password"
                required
                placeholder="请输入密码"
              />
              <button
                type="button"
                class="btn btn-outline-secondary"
                @click="showPwd = !showPwd"
              >
                {{ showPwd ? '隐藏' : '显示' }}
              </button>
            </div>
          </div>

          <!-- 操作区 -->
          <button
            type="submit"
            class="btn btn-primary w-100"
            :disabled="loading || !employeeId || !password"
          >
            <span
              v-if="loading"
              class="spinner-border spinner-border-sm me-2"
              role="status"
              aria-hidden="true"
            ></span>
            登录
          </button>

          <!-- 轻提示
          <div class="form-text mt-2">
            首次登录成功后，系统将引导你修改密码。
          </div> -->
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const employeeId = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')
const showPwd = ref(false)

async function onLogin () {
  errorMsg.value = ''
  loading.value = true
  try {
    const { data } = await http.post('/api/auth/login', {
      employeeId: employeeId.value,
      password: password.value
    })

    const apiUser = data.user || {
      employeeId: data.employeeId ?? employeeId.value,
      name: data.name ?? '',
      department: data.department ?? '',
      roles: Array.isArray(data.roles) ? data.roles
            : (Array.isArray(data.authorities) ? data.authorities : [])
    }

    auth.setAuth(data.token, apiUser, { mustChangePassword: !!data.mustChangePassword })

    if (auth.mustChangePassword) {
      router.replace('/change-password')
    } else {
      const redirect = route.query.redirect ? String(route.query.redirect) : '/my'
      router.replace(redirect)
    }
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || e?.message || '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 可选：让卡片标题更紧凑些 */
.h5 { letter-spacing: .2px; }
</style>
