<template>
  <div class="container my-4" v-if="loaded">
    <h3 class="mb-3">用户中心</h3>
    <!-- 基础信息 -->
    <div class="card shadow-sm mb-4">
      <div class="card-header fw-semibold">基础信息</div>
      <div class="card-body">
        <div class="row g-3">
          <div class="col-md-4">
            <label class="form-label">工号</label>
            <input v-model.trim="form.employeeId" class="form-control" maxlength="32" />
          </div>
          <div class="col-md-4">
            <label class="form-label">姓名</label>
            <input v-model.trim="form.name" class="form-control" maxlength="50" />
          </div>
          <div class="col-md-4">
            <label class="form-label">科室</label>
            <input v-model.trim="form.department" class="form-control" maxlength="50" />
          </div>
        </div>
        <div class="mt-3">
          <button class="btn btn-primary" :disabled="savingInfo" @click="saveInfo">
            {{ savingInfo ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
    <!-- 修改密码 -->
    <div id="password" class="card shadow-sm">
      <div class="card-header fw-semibold">修改密码</div>
      <div class="card-body">
        <div class="row g-3">
          <div class="col-md-4">
            <label class="form-label">原密码</label>
            <div class="input-group">
              <input
                :type="showOld ? 'text' : 'password'"
                v-model="pwd.oldPassword"
                class="form-control"
                autocomplete="current-password"
              />
              <button
                class="btn btn-outline-secondary"
                type="button"
                @click="showOld = !showOld"
                :title="showOld ? '隐藏' : '显示'"
                :aria-label="showOld ? '隐藏密码' : '显示密码'"
              >
                {{ showOld ? '隐藏' : '显示' }}
              </button>
            </div>
          </div>
          <div class="col-md-4">
            <label class="form-label">新密码（8-64位）</label>
            <div class="input-group">
              <input
                :type="showNew ? 'text' : 'password'"
                v-model="pwd.newPassword"
                class="form-control"
                autocomplete="new-password"
              />
              <button
                class="btn btn-outline-secondary"
                type="button"
                @click="showNew = !showNew"
                :title="showNew ? '隐藏' : '显示'"
                :aria-label="showNew ? '隐藏密码' : '显示密码'"
              >
                {{ showNew ? '隐藏' : '显示' }}
              </button>
            </div>
          </div>
        </div>
        <div class="mt-3">
          <button class="btn btn-primary" :disabled="savingPwd" @click="savePwd">
            {{ savingPwd ? '提交中...' : '修改密码' }}
          </button>
        </div>
      </div>
    </div>
  </div>
  <div class="container my-5" v-else>
    正在加载...
  </div>
</template>

<script setup>
const showOld = ref(false)
const showNew = ref(false)

import { ref, onMounted } from 'vue'
import axios from '@/utils/axios'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()

const loaded = ref(false)
const form = ref({ employeeId: '', name: '', department: '' })
const savingInfo = ref(false)

const pwd = ref({ oldPassword: '', newPassword: '' })
const savingPwd = ref(false)

async function fetchMe() {
  const { data } = await axios.get('/api/me')
  form.value = {
    employeeId: data.employeeId || '',
    name: data.name || '',
    department: data.department || ''
  }
  loaded.value = true
}
onMounted(fetchMe)

function forceReLogin(msg = '信息已更新，请重新登录') {
  auth.logout()
  alert(msg)
  window.location.replace('/login') 
}

// 保存基础信息
async function saveInfo() {
  if (!form.value) return
  try {
    savingInfo.value = true
    await axios.put('/api/me', {
      employeeId: form.value.employeeId?.trim(),
      name: form.value.name?.trim(),
      department: form.value.department?.trim(),
    })
    forceReLogin('信息已更新，请重新登录后生效')
  } catch (e) {
    alert(e?.response?.data?.message || '保存失败')
  } finally {
    savingInfo.value = false
  }
}

// 修改密码
async function savePwd() {
  if (!pwd.value.oldPassword || !pwd.value.newPassword) {
    alert('请填写完整')
    return
  }
  try {
    savingPwd.value = true
    await axios.put('/api/me/password', {
      oldPassword: pwd.value.oldPassword,
      newPassword: pwd.value.newPassword
    })
    forceReLogin('密码已修改，请重新登录')
  } catch (e) {
    alert(e?.response?.data?.message || '修改失败')
  } finally {
    savingPwd.value = false
  }
}
</script>