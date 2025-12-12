<template>
  <div class="container my-4">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h2 class="h4 mb-0">审批收件箱</h2>
      <div class="d-flex align-items-center gap-2">
        <input
          v-model.trim="q"
          placeholder="按标题/提交人搜索…"
          class="form-control form-control-sm"
          style="min-width: 120px; flex-shrink: 1"
        />
        <button
          class="btn btn-sm btn-primary"
          style="white-space: nowrap"
          @click="load"
          :disabled="loading"
        >
          <span
            v-if="loading"
            class="spinner-border spinner-border-sm me-1"
            aria-hidden="true"
          ></span>
          刷新
        </button>
      </div>
    </div>

    <div class="card shadow-sm">
      <div class="card-body pb-2">
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-3">
          <ul class="nav nav-pills gap-2 mb-0">
            <li class="nav-item">
              <button
                class="nav-link"
                :class="{ active: tab === 'todo' }"
                @click="switchTab('todo')"
              >
                待我审批
              </button>
            </li>
            <li class="nav-item">
              <button
                class="nav-link"
                :class="{ active: tab === 'all' }"
                @click="switchTab('all')"
              >
                全部
              </button>
            </li>
          </ul>
          <div class="d-flex align-items-center gap-2">
            <label class="form-label mb-0">状态：</label>
            <select
              class="form-select form-select-sm w-auto"
              v-model="statusFilter"
            >
              <option value="">全部</option>
              <option value="processing">流程中</option>
              <option value="rejected">驳回</option>
              <option value="done">已完成</option>
            </select>
          </div>
        </div>
        <div
          v-if="loading"
          class="d-flex align-items-center gap-2 text-muted py-4 justify-content-center"
        >
          <div class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></div>
          <span>加载中…</span>
        </div>
        <!-- 表格 -->
        <div v-else>
          <div v-if="rowsShown.length" class="table-responsive">
            <table class="table table-hover table-striped align-middle mb-0">
              <thead class="table-light">
                <tr>
                  <th style="width:90px">ID</th>
                  <th>标题</th>
                  <th style="width:120px">申请类型</th>
                  <th style="width:140px">状态</th>
                  <th style="width:120px">提交人</th>
                  <th style="width:180px">时间</th>
                  <th style="width:90px">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="r in rowsShown"
                  :key="r.id"
                  class="cursor-pointer"
                  @click="goDetail(r)"
                >
                  <td class="text-body-secondary">#{{ r.id }}</td>
                  <td class="fw-semibold">
                    <div class="text-truncate" style="max-width: 220px">
                      {{ r.title }}
                    </div>
                  </td>
                  <td>
                    <span class="badge" :class="categoryBadgeClass(r.category)">
                      {{ r.category || '未分类' }}
                    </span>
                  </td>
                  <td>
                    <span class="badge" :class="statusBadgeClass(r)">
                      {{ statusLabel(r) }}
                    </span>
                  </td>
                  <td>
                    <div class="d-flex flex-column">
                      <span>{{ r.createdByName }}</span>
                      <small class="text-body-secondary">{{ r.createdByDept }}</small>
                    </div>
                  </td>
                  <td class="text-nowrap">
                    {{ fmt(r.submittedAt || r.createdAt) }}
                  </td>
                  <td class="text-nowrap">
                    <button
                      class="btn btn-sm btn-outline-primary"
                      @click.stop="goDetail(r)"
                    >
                      查看
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <!-- 无数据 -->
          <div
            v-else
            class="alert alert-light border d-flex align-items-center m-0"
            role="alert"
          >
            <i class="bi bi-inbox me-2" aria-hidden="true"></i>
            <div>暂无数据</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import isoWeek from 'dayjs/plugin/isoWeek'
dayjs.extend(isoWeek)

import { useAuthStore } from '@/store/auth'
import { listInbox, getDemand } from '@/api/demand'

const router = useRouter()
const auth = useAuthStore()

const rows = ref([])
const loading = ref(false)
const tab = ref('todo')
const statusFilter = ref('')
const q = ref('')
const derivedStatusMap = ref({})

const roles = computed(() => auth.user?.roles || auth.roles || [])
const isAdmin = computed(() => roles.value.includes('ADMIN'))
const isReviewer = computed(() => roles.value.includes('REVIEWER'))

function fmt(s) {
  return s ? s.replace('T', ' ').slice(0, 19) : ''
}

function goDetail(r) {
  router.push(`/demands/${r.id}`)
}

function categoryBadgeClass(category) {
  const c = String(category || '').trim()
  if (c === '临时') return 'text-bg-primary'
  if (c === '周报') return 'text-bg-info'
  if (c === '月报') return 'text-bg-warning'
  if (c === '其他') return 'text-bg-secondary'
  return 'text-bg-secondary'
}

function isChronicCategory(category) {
  const c = String(category || '')
  return c.includes('周报') || c.includes('月报')
}
function isWeekCategory(category) {
  return String(category || '').includes('周报')
}
function isMonthCategory(category) {
  return String(category || '').includes('月报')
}

function extractKeyFromNote(note) {
  const s = String(note ?? '').trim()
  if (!s) return null

  if (s.startsWith('{')) {
    try {
      const o = JSON.parse(s)
      if (o && typeof o.key === 'string' && o.key) return o.key
    } catch {}
  }

  const mWeek = s.match(/\b(\d{4})-W(\d{1,2})\b/)
  if (mWeek) return `${mWeek[1]}-W${String(mWeek[2]).padStart(2, '0')}`

  const mMonth = s.match(/\b(\d{4})-(\d{1,2})\b/)
  if (mMonth) return `${mMonth[1]}-${String(mMonth[2]).padStart(2, '0')}`

  return null
}

function normalizeAction(rawAction) {
  const A = String(rawAction || '').toUpperCase()
  const isUndo  =
    /(UNMARK|UNDO|UNSET|CANCEL|REVOKE|ROLLBACK)/.test(A) || /^UN/.test(A)
  const isWeek  = /WEEK/.test(A) || /W(\d{1,2})\b/.test(A)
  const isMonth = /MONTH/.test(A) || /\b\d{4}-\d{1,2}\b/.test(A)
  return { raw: A, isUndo, isWeek, isMonth }
}

function parseWeekKey(key) {
  const m = /^(\d{4})-W(\d{1,2})$/.exec(String(key || ''))
  if (!m) return null
  const year = Number(m[1])
  const week = Number(m[2])
  return { year, week, sort: year * 100 + week }
}
function parseMonthKey(key) {
  const m = /^(\d{4})-(\d{1,2})$/.exec(String(key || ''))
  if (!m) return null
  const year = Number(m[1])
  const month = Number(m[2])
  return { year, month, sort: year * 100 + month }
}

function latestWeekDoneFromMap(map, now) {
  if (!map || map.size === 0) return false
  const target = now.subtract(1, 'week')
  const key = `${target.isoWeekYear()}-W${String(target.isoWeek()).padStart(2,'0')}`
  return !!map.get(key)
}

function latestMonthDoneFromMap(map, now) {
  if (!map || map.size === 0) return false
  const target = now.subtract(1, 'month')
  const key = `${target.year()}-${String(target.month() + 1).padStart(2,'0')}`
  return !!map.get(key)
}


function rowDisplayStatus(r) {
  const derived = derivedStatusMap.value[r.id]
  if (derived) return derived
  return String((r && r.status) || '')
}

function statusLabel(r) {
  const s = String(rowDisplayStatus(r))

  switch (s) {
    case 'WEEK_DONE':       return '上周已完成'
    case 'WEEK_NOT_DONE':   return '上周未完成'
    case 'MONTH_DONE':      return '上月已完成'
    case 'MONTH_NOT_DONE':  return '上月未完成'
    case 'COMPLETED':       return '已完成'
    case 'DRAFT':           return '草稿'
    case 'SUBMITTED':       return '待主任审批'
    case 'ADMIN_APPROVED':  return '主任同意'
    case 'ADMIN_REJECTED':  return '主任驳回'
    case 'REVIEW_APPROVED': return '科室同意'
    case 'REVIEW_REJECTED': return '科室驳回'
    case 'RETURNED':        return '退回修改'
    case 'REJECTED':        return '已拒绝'
    default:                return s || '未知'
  }
}

function statusBadgeClass(r) {
  const s = String(rowDisplayStatus(r)).toUpperCase()

  switch (s) {
    case 'DRAFT':            return 'text-bg-secondary'
    case 'SUBMITTED':        return 'text-bg-info'
    case 'ADMIN_APPROVED':
    case 'IN_REVIEW':        return 'text-bg-primary'
    case 'RETURNED':         return 'text-bg-warning'
    case 'ADMIN_REJECTED':
    case 'REVIEW_REJECTED':
    case 'REJECTED':         return 'text-bg-danger'
    case 'COMPLETED':
    case 'WEEK_DONE':
    case 'MONTH_DONE':       return 'text-bg-success'
    case 'WEEK_NOT_DONE':
    case 'MONTH_NOT_DONE':   return 'text-bg-secondary'
    default:                 return 'text-bg-secondary'
  }
}

function statusGroup(r) {
  const s = String(rowDisplayStatus(r)).toUpperCase()
  if (['COMPLETED', 'WEEK_DONE', 'MONTH_DONE'].includes(s)) {
    return 'done'
  }
  if (['ADMIN_REJECTED', 'REVIEW_REJECTED', 'REJECTED'].includes(s)) {
    return 'rejected'
  }
  if ([
    'SUBMITTED',
    'ADMIN_APPROVED',
    'IN_REVIEW',
    'RETURNED',
    'WEEK_NOT_DONE',
    'MONTH_NOT_DONE',
    'DRAFT'
  ].includes(s)) {
    return 'processing'
  }
  return 'processing'
}

function isTodo(r) {
  const st = String(r?.status || '')
  const todoForAdmin    = isAdmin.value && st === 'SUBMITTED'
  const todoForReviewer = isReviewer.value && st === 'ADMIN_APPROVED'
  return todoForAdmin || todoForReviewer
}

async function hydrateDerivedStatus() {
  const targets = rows.value.filter(r => isChronicCategory(r.category))
  const result = {}

  if (!targets.length) {
    derivedStatusMap.value = result
    return
  }

  const now = dayjs()

  for (const r of targets) {
    try {
      const detail = await getDemand(r.id)
      const cat    = String(detail?.category || r.category || '')
      const server = String(detail?.status   || r.status   || '')
      const isWeek = isWeekCategory(cat)
      const isMonth = isMonthCategory(cat)

      if (!isWeek && !isMonth) {
        result[r.id] = server
        continue
      }
      const preApproval = [
        'DRAFT',
        'SUBMITTED',
        'ADMIN_APPROVED',
        'ADMIN_REJECTED',
        'REVIEW_REJECTED',
        'RETURNED'
      ]
      if (preApproval.includes(server)) {
        result[r.id] = server
        continue
      }

      const hist = Array.isArray(detail?.history) ? detail.history : []
      const sorted = hist.slice().sort((a, b) => {
        const ta = dayjs(a?.occurredAt).valueOf() || 0
        const tb = dayjs(b?.occurredAt).valueOf() || 0
        return ta - tb
      })

      const weekState = new Map()
      const monthState = new Map()

      for (const h of sorted) {
        const act = normalizeAction(h?.action)
        if (!act.isWeek && !act.isMonth) continue

        const key =
          extractKeyFromNote(h?.note) ||
          (typeof h?.key === 'string' ? h.key : null)
        if (!key) continue

        if (act.isWeek)  weekState.set(key,  !act.isUndo)
        if (act.isMonth) monthState.set(key, !act.isUndo)
      }

      if (isWeek) {
        const done = latestWeekDoneFromMap(weekState, now)
        result[r.id] = done ? 'WEEK_DONE' : 'WEEK_NOT_DONE'
      } else if (isMonth) {
        const done = latestMonthDoneFromMap(monthState, now)
        result[r.id] = done ? 'MONTH_DONE' : 'MONTH_NOT_DONE'
      } else {
        result[r.id] = server
      }
    } catch (e) {
      result[r.id] = String((r && r.status) || '')
    }
  }

  derivedStatusMap.value = result
}

async function load() {
  loading.value = true
  try {
    rows.value = await listInbox()
    await hydrateDerivedStatus()
  } finally {
    loading.value = false
  }
}

function switchTab(t) {
  if (tab.value !== t) {
    tab.value = t
  }
}

const rowsShown = computed(() => {
  let list = rows.value.slice()

  if (tab.value === 'todo') {
    list = list.filter(isTodo)
  }

  if (statusFilter.value) {
    list = list.filter(r => statusGroup(r) === statusFilter.value)
  }

  if (q.value) {
    const k = q.value.toLowerCase()
    list = list.filter(r =>
      String(r.title || '').toLowerCase().includes(k) ||
      String(r.createdByName || '').toLowerCase().includes(k)
    )
  }

  return list
})

onMounted(load)
</script>

<style scoped>
.cursor-pointer { cursor: pointer; }

@media (max-width: 576px) {
  h2.h4 { font-size: 1.1rem; }
}
</style>