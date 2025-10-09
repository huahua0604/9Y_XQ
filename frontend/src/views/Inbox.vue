<template>
  <div class="container">
    <div class="hdr">
      <h2>审批收件箱</h2>
      <div class="actions">
        <input v-model.trim="q" placeholder="按标题/提交人搜索…" />
        <button @click="load" :disabled="loading">刷新</button>
      </div>
    </div>

    <div class="tabs">
      <button :class="{on: tab==='todo'}" @click="tab='todo'">待我审批</button>
      <button :class="{on: tab==='all'}"  @click="tab='all'">全部</button>

      <div class="d-flex align-items-center gap-2">
        <label class="form-label mb-0">状态：</label>
        <select class="form-select form-select-sm w-auto" v-model="statusFilter">
          <option value="">全部</option>
          <option value="SUBMITTED">待主任审批</option>
          <option value="ADMIN_APPROVED">待科室审批</option>
          <option value="ADMIN_REJECTED">主任驳回</option>
          <option value="REVIEW_APPROVED">科室通过</option>
          <option value="REVIEW_REJECTED">科室驳回</option>
          <option value="COMPLETED">已完成</option>
        </select>
      </div>
    </div>

    <table class="tbl">
      <thead>
        <tr>
          <th style="width:80px;">ID</th>
          <th>标题</th>
          <th style="width:110px;">申请类型</th>
          <th style="width:140px;">状态</th>
          <th>提交人</th>
          <th style="width:170px;">时间</th>
          <th style="width:90px;">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in rowsShown" :key="r.id">
          <td>{{ r.id }}</td>
          <td class="title" @click="goDetail(r)">{{ r.title }}</td>
          <td>{{ r.category }}</td>
                <td>
                  <span class="badge" :class="statusBadgeClass(r.status)">
                    {{ statusLabel(r.status) }}
                  </span>
                </td>
          <td>{{ r.createdByName }}（{{ r.createdByDept }}）</td>
          <td>{{ fmt(r.submittedAt || r.createdAt) }}</td>
          <td><button class="small" @click="goDetail(r)">查看</button></td>
        </tr>
        <tr v-if="!loading && rowsShown.length===0">
          <td colspan="8" class="empty">暂无数据</td>
        </tr>
        <tr v-if="loading">
          <td colspan="8" class="empty">加载中…</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { listInbox } from '@/api/demand'

const router = useRouter()
const auth = useAuthStore()

const rows = ref([])
const loading = ref(false)
const tab = ref('todo')
const statusFilter = ref('')
const q = ref('')

const isAdmin = computed(() => (auth.roles || []).includes('ADMIN'))
const isReviewer = computed(() => (auth.roles || []).includes('REVIEWER'))

function isTodo(r){
  const todoForAdmin = isAdmin.value && r.status === 'SUBMITTED'
  const todoForReviewer = isReviewer.value && r.status === 'ADMIN_APPROVED'
  return todoForAdmin || todoForReviewer
}

const rowsShown = computed(() => {
  let list = [...rows.value]
  if (tab.value === 'todo') list = list.filter(isTodo)
  if (statusFilter.value) list = list.filter(r => r.status === statusFilter.value)
  if (q.value) {
    const k = q.value.toLowerCase()
    list = list.filter(r =>
      (r.title || '').toLowerCase().includes(k) ||
      (r.createdByName || '').toLowerCase().includes(k)
    )
  }
  return list
})

function fmt(s){ return s ? String(s).replace('T',' ').slice(0,19) : '' }
function goDetail(r){ router.push(`/demands/${r.id}`) }

function statusLabel(status) {
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'SUBMITTED': return '待主任审批'
    case 'ADMIN_APPROVED': return '主任同意'
    case 'ADMIN_REJECTED': return '主任驳回'
    case 'REVIEW_APPROVED': return '科室同意'
    case 'REVIEW_REJECTED': return '科室驳回'
    case 'COMPLETED': return '已完成'
    default: return status || '未知'
  }
}

function statusBadgeClass(status) {
  switch ((status || '').toUpperCase()) {
    case 'DRAFT': return 'text-bg-secondary'
    case 'SUBMITTED': return 'text-bg-info'
    case 'IN_REVIEW': return 'text-bg-primary'
    case 'RETURNED': return 'text-bg-warning'
    case 'REJECTED': return 'text-bg-danger'
    case 'APPROVED':
    case 'DONE': return 'text-bg-success'
    default: return 'text-bg-secondary'
  }
}

async function load(){
  loading.value = true
  try{
    rows.value = await listInbox()
  }finally{
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.container{max-width:1100px;margin:24px auto;padding:12px}
.hdr{display:flex;justify-content:space-between;align-items:center;margin-bottom:8px}
.actions{display:flex;gap:8px;align-items:center}
.actions input{padding:6px 10px;border:1px solid #dbe3ef;border-radius:8px;min-width:240px}
.actions button{padding:6px 12px;border:1px solid #cbd5e1;border-radius:8px;background:#fafafa}
.tabs{display:flex;gap:8px;align-items:center;margin:10px 0 12px}
.tabs button{padding:6px 10px;border:1px solid #cbd5e1;background:#f8fafc;border-radius:8px}
.tabs .on{background:#1e80ff;color:#fff;border-color:#1e80ff}
.seg{margin-left:8px;display:flex;gap:6px;align-items:center}
.tbl{width:100%;border-collapse:collapse}
.tbl th,.tbl td{border-bottom:1px solid #eef2f7;padding:10px 8px;font-size:14px}
.title{cursor:pointer;color:#1e80ff}
.title:hover{text-decoration:underline}
.empty{text-align:center;color:#94a3b8}
.small{padding:4px 8px;border:1px solid #cbd5e1;border-radius:8px;background:#fff}
.pill{padding:2px 8px;border-radius:999px;border:1px solid #e2e8f0;font-size:12px}
.pill.HIGH{border-color:#f59e0b;color:#b45309}
.pill.CRITICAL{border-color:#ef4444;color:#b91c1c}
</style>
