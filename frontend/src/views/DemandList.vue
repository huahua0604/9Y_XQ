<template>
  <div class="container my-4">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h2 class="h4 mb-0">需求列表</h2>
      <router-link class="btn btn-primary" to="/demands/new">
        <span class="me-1">+</span> 新建需求
      </router-link>
    </div>
    <div class="card shadow-sm">
      <div class="card-body pb-2">
        <ul class="nav nav-pills gap-2 mb-3">
          <li class="nav-item">
            <button
              class="nav-link"
              :class="{ active: tab==='mine' }"
              @click="switchTab('mine')"
            >
              我的
            </button>
          </li>
          <!-- <li class="nav-item" v-if="isStaff">
            <button
              class="nav-link"
              :class="{ active: tab==='inbox' }"
              @click="switchTab('inbox')"
            >
              待我/全量
            </button>
          </li> -->
        </ul>
        <div v-if="loading" class="d-flex align-items-center gap-2 text-muted py-4">
          <div class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></div>
          <span>加载中…</span>
        </div>
        <div v-else>
          <div v-if="rows.length" class="table-responsive">
            <table class="table table-hover table-striped align-middle">
              <thead class="table-light">
              <tr>
                <th style="width:90px">ID</th>
                <th>标题</th>
                <th style="width:120px">申请类型</th>
                <th style="width:120px">状态</th>
                <th style="width:120px">提交人</th>
                <th style="width:180px">提交时间</th>
              </tr>
              </thead>
              <tbody>
              <tr
                v-for="r in rows"
                :key="r.id"
                class="cursor-pointer"
                @click="$router.push(`/demands/${r.id}`)"
              >
                <td class="text-body-secondary">#{{ r.id }}</td>
                <td class="fw-semibold">
                  <div class="text-truncate" style="max-width: 520px">{{ r.title }}</div>
                </td>
                <td>
                  <span class="badge" :class="categoryBadgeClass(r.category)">
                    {{ r.category || '未分类' }}
                  </span>
                </td>
                <td>
                  <span class="badge" :class="statusBadgeClass(r.status)">
                    {{ statusLabel(r.status) }}
                  </span>
                </td>
                <td>
                  <div class="d-flex flex-column">
                    <span>{{ r.createdByName }}</span>
                    <small class="text-body-secondary">{{ r.createdByDept }}</small>
                  </div>
                </td>
                <td class="text-nowrap">{{ fmt(r.submittedAt || r.createdAt) }}</td>
              </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="alert alert-light border d-flex align-items-center" role="alert">
            <i class="bi bi-inbox me-2" aria-hidden="true"></i>
            <div>暂无数据</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { listMine, listInbox } from '@/api/demand'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const isStaff = computed(() =>
  auth.user.roles?.includes('ADMIN') || auth.user.roles?.includes('REVIEWER')
)

type Tab = 'mine' | 'inbox'
const tab = ref<Tab>('mine')
const rows = ref<any[]>([])
const loading = ref(false)

function fmt(s?: string) {
  return s ? s.replace('T',' ').slice(0,19) : ''
}

function statusBadgeClass(status?: string) {
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

function categoryBadgeClass(category?: string) {
  const c = (category || '').trim()
  if (['临时'].includes(c)) return 'text-bg-primary'
  if (['常态化(如:周报,月报等)'].includes(c)) return 'text-bg-info'
  if (['其他'].includes(c)) return 'text-bg-warning'
  return 'text-bg-secondary'
}

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

async function load() {
  loading.value = true
  try {
    rows.value = tab.value === 'mine' ? await listMine() : await listInbox()
  } finally {
    loading.value = false
  }
}

function switchTab(t: Tab) {
  if (tab.value !== t) {
    tab.value = t
    load()
  }
}

onMounted(load)
</script>

<style scoped>
.cursor-pointer { cursor: pointer; }

@media (max-width: 576px) {
  h2.h4 { font-size: 1.1rem; }
}
</style>
