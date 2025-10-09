<template>
  <div class="container my-5">
    <div class="card shadow-sm mx-auto" style="max-width: 520px;">
      <div class="card-body">
        <h2 class="h5 mb-3">首次登录需修改密码</h2>

        <!-- 错误提示 -->
        <div v-if="errorMsg" class="alert alert-danger py-2" role="alert">
          {{ errorMsg }}
        </div>

        <form @submit.prevent="onSubmit" class="needs-validation" novalidate>
          <!-- 原密码 -->
          <div class="mb-3">
            <label class="form-label">原密码</label>
            <div class="input-group">
              <input
                :type="showOld ? 'text' : 'password'"
                class="form-control"
                v-model.trim="oldPassword"
                required
                @blur="touched.old = true"
                :class="{'is-invalid': touched.old && !oldPassword}"
              />
              <button type="button" class="btn btn-outline-secondary" @click="showOld = !showOld">
                {{ showOld ? '隐藏' : '显示' }}
              </button>
              <div class="invalid-feedback">请输入原密码。</div>
            </div>
          </div>

          <!-- 新密码 -->
          <div class="mb-2">
            <label class="form-label">新密码</label>
            <div class="input-group">
              <input
                :type="showNew ? 'text' : 'password'"
                class="form-control"
                placeholder="密码长度需大于8位"
                v-model.trim="newPassword"
                minlength="8"
                required
                @blur="touched.new = true"
                :class="{'is-invalid': touched.new && !validLength}"
              />
              <button type="button" class="btn btn-outline-secondary" @click="showNew = !showNew">
                {{ showNew ? '隐藏' : '显示' }}
              </button>
              <div class="invalid-feedback">至少 8 位新密码。</div>
            </div>
            <div class="form-text">建议包含大小写字母与数字，避免与旧密码相似。</div>
          </div>

          <!-- 强度/长度提示（可选） -->
          <div class="mb-3">
            <div class="progress" style="height: 6px;">
              <div
                class="progress-bar"
                role="progressbar"
                :style="{ width: strength + '%' }"
                :aria-valuenow="strength"
                aria-valuemin="0"
                aria-valuemax="100"
              />
            </div>
            <small class="text-body-secondary">密码强度：{{ strengthLabel }}</small>
          </div>

          <button
            class="btn btn-primary w-100"
            type="submit"
            :disabled="loading || !formValid"
          >
            <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
            保存
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import http from '../api/http'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()

const oldPassword = ref('')
const newPassword = ref('')
const loading = ref(false)
const errorMsg = ref('')

const showOld = ref(false)
const showNew = ref(false)
const touched = ref({ old: false, new: false })

const validLength = computed(() => (newPassword.value || '').length >= 8)
const formValid = computed(() => !!oldPassword.value && validLength.value)

// 简单强度评估：长度 + 是否包含数字/大小写/特殊符号
const strength = computed(() => {
  const s = newPassword.value || ''
  let score = 0
  if (s.length >= 8) score += 30
  if (/[0-9]/.test(s)) score += 20
  if (/[a-z]/.test(s) && /[A-Z]/.test(s)) score += 30
  if (/[^A-Za-z0-9]/.test(s)) score += 20
  return Math.min(score, 100)
})
const strengthLabel = computed(() => {
  if (!newPassword.value) return '无'
  if (strength.value < 40) return '弱'
  if (strength.value < 70) return '中'
  return '强'
})

async function onSubmit(){
  errorMsg.value = ''
  loading.value = true
  try{
    // 1) 调后端修改密码接口（按你后端的字段名即可）
    await http.post('/api/auth/change-password', {
      oldPassword: oldPassword.value,
      newPassword: newPassword.value
    })

    // 2) 修改成功：清空本地会话，强制回登录页
    alert('密码已修改，请使用新密码重新登录')
    auth.clear() // 清空 token / user / exp / mustChangePassword
    router.replace({ name: 'login', query: { from: 'pwd' } })
  }catch(e){
    errorMsg.value = e?.response?.data?.message || '修改失败，请稍后重试'
  }finally{
    loading.value = false
  }
}
</script>

<style scoped>
/* Progress bar 过渡顺滑一点 */
.progress-bar {
  transition: width .25s ease;
}
</style>
