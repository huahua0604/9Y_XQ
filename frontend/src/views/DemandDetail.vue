<template>
  <div class="container my-4" v-if="d">
    <!-- 标题与状态 -->
    <div class="d-flex align-items-center justify-content-between mb-3 flex-wrap gap-2">
      <div class="d-flex align-items-center gap-2 flex-wrap">
        <div class="d-flex align-items-center gap-2">
          <template v-if="!titleEditing">
            <h2 class="h5 mb-0">#{{ d.id }} {{ d.title }}</h2>
          </template>
          <template v-else>
            <span class="text-body-secondary small">#{{ d.id }}</span>
            <input
              v-model="newTitle"
              class="form-control form-control-sm"
              style="min-width: 240px;"
              :disabled="savingTitle"
            />
          </template>
          <span class="badge" :class="statusBadgeClass(statusDisplay)">{{ statusLabel(statusDisplay) }}</span>
        </div>
        <div v-if="isAdmin" class="d-flex align-items-center gap-1">
          <button
            v-if="!titleEditing"
            class="btn btn-outline-secondary btn-sm"
            @click="startEditTitle"
          >
            修改标题
          </button>
          <template v-else>
            <button
              class="btn btn-primary btn-sm"
              @click="saveTitle"
              :disabled="savingTitle"
            >
              {{ savingTitle ? '保存中…' : '保存' }}
            </button>
            <button
              class="btn btn-outline-secondary btn-sm"
              @click="cancelEditTitle"
              :disabled="savingTitle"
            >
              取消
            </button>
          </template>
        </div>
      </div>
      <div class="text-body-secondary small" v-if="d.submittedAt || d.createdAt">
        最近更新：{{ fmt(d.submittedAt || d.createdAt) }}
      </div>
    </div>
    <!-- 基本信息 -->
    <div class="card shadow-sm mb-3">
      <div class="card-header fw-semibold">基本信息</div>
      <div class="card-body">
        <div class="row g-3">
          <div class="col-12 col-md-6">
            <div class="text-body-secondary small mb-1">申请类型</div>
            <div><span class="badge text-bg-info">{{ d.category || '未分类' }}</span></div>
          </div>
          <div class="col-12 col-md-6">
            <div class="text-body-secondary small mb-1">影响范围</div>
            <div>{{ d.impactScope || '-' }}</div>
          </div>
          <div class="col-6 col-md-3">
            <div class="text-body-secondary small mb-1">联系电话</div>
            <div>{{ d.contactPhone || '-' }}</div>
          </div>
          <div class="col-6 col-md-3">
            <div class="text-body-secondary small mb-1">地点/科室</div>
            <div>{{ d.location || '-' }}</div>
          </div>
          <div class="col-12 col-md-6">
            <div class="text-body-secondary small mb-1">提交人</div>
            <div>{{ d.createdByName }}({{ d.createdByDept }}/{{ d.createdByEmpId }})</div>
          </div>
          <div class="col-12 col-md-6">
            <div class="text-body-secondary small mb-1">提交时间</div>
            <div>{{ fmt(d.submittedAt || d.createdAt) }}</div>
          </div>
        </div>
      </div>
    </div>
    <!-- 需求详情 -->
    <div class="card shadow-sm mb-3">
      <div class="card-header fw-semibold">需求详情</div>
      <div class="card-body">
        <pre class="mb-0 text-wrap" style="white-space: pre-wrap;">{{ d.description }}</pre>
      </div>
    </div>
    <!-- 附件上传 -->
    <div class="card shadow-sm mb-3">
      <div class="card-header fw-semibold">附件</div>
      <div class="card-body">
        <div class="alert alert-light border py-2 mb-3" role="alert">
          新上传的附件会出现在下方流转历史，<b>（周报或月报注意时间选取，默认为最新一周或最新一月）</b>
        </div>
        <div class="input-group">
          <input type="file" class="form-control" ref="fileInput" multiple @change="pick">
          <button class="btn btn-primary" @click="doUpload" :disabled="!picked || uploading">
            {{ uploading ? '上传中…' : '上传附件' }}
          </button>
        </div>
      </div>
    </div>
    <!--意见与审批-->
    <div class="card shadow-sm mb-3">
      <div class="card-header fw-semibold d-flex align-items-center justify-content-between">
        <span>意见与审批</span>
        <small class="text-body-secondary" v-if="isAdmin || isReviewer">
          当前状态：{{ statusLabel(d.status) }}
        </small>
      </div>
      <div class="card-body">
        <!-- 意见输入 -->
        <div class="mb-2">
          <textarea
            class="form-control"
            rows="4"
            v-model="opinion"
            placeholder="填写意见（周报或月报需要注意时间选取，默认为最新一周或最新一月）"
          ></textarea>
        </div>
        <!-- 审批/评论 + 完成/撤销 操作区 -->
        <div class="d-flex flex-wrap gap-2 align-items-center">
          <!-- 主任审批 -->
          <template v-if="isAdmin">
            <button class="btn btn-success btn-sm"
                    v-if="d.status==='SUBMITTED'"
                    @click="approve('admin')">主任同意</button>
            <button class="btn btn-danger btn-sm"
                    v-if="d.status==='SUBMITTED'"
                    @click="reject('admin')">主任驳回</button>
          </template>
          <!-- 科室审批 -->
          <template v-if="isReviewer">
            <button class="btn btn-success btn-sm"
                    v-if="d.status==='ADMIN_APPROVED'"
                    @click="approve('reviewer')">科室同意</button>
            <button class="btn btn-danger btn-sm"
                    v-if="d.status==='ADMIN_APPROVED'"
                    @click="reject('reviewer')">科室驳回</button>
          </template>
          <!-- 仅发表评论 -->
          <button class="btn btn-outline-secondary btn-sm"
                  v-if="isAdmin || isReviewer"
                  @click="leaveComment">仅发表意见</button>
          <button class="btn btn-primary btn-sm"
                  v-else
                  @click="leaveComment">发布</button>
          <template v-if="isAdmin || isReviewer">
            <button
              class="btn btn-outline-primary btn-sm"
              :disabled="smsSending || !d?.contactPhone"
              @click="toggleSms"
            >
              {{ showSms ? '收起短信' : '短信通知' }}
            </button>
            <small v-if="!d?.contactPhone" class="text-muted">
              联系电话为空
            </small>
          </template>
          <!-- 分隔线 -->
          <div class="vr mx-2 d-none d-sm-block"></div>
          <!-- 临时/其他：整体完成/撤销（管理员或科室可见） -->
          <template v-if="!isPeriodic && (isAdmin || isReviewer)">
            <button class="btn btn-success btn-sm"
                    v-if="canMarkCompleted"
                    @click="doMarkCompleted">标记为已完成</button>
            <button class="btn btn-danger btn-sm"
                    v-if="canUnmarkCompleted"
                    @click="doUnmarkCompleted">撤销已完成</button>
          </template>
        </div>
        <!-- 短信发送区 -->
        <div v-if="showSms && (isAdmin || isReviewer)" class="mt-2">
          <div class="border rounded p-2">
            <div class="small text-muted mb-1">
              发送到：{{ d?.contactPhone }}
            </div>
            <textarea
              class="form-control form-control-sm"
              rows="2"
              v-model="smsText"
              placeholder="短信内容（按模板规则：这里只填变量值，不要带{}）"
            ></textarea>
            <div class="mt-2 d-flex gap-2">
              <button
                class="btn btn-primary btn-sm"
                @click="sendSms"
                :disabled="smsSending || !smsText.trim()"
              >
                {{ smsSending ? '发送中…' : '发送短信' }}
              </button>
              <button
                class="btn btn-outline-secondary btn-sm"
                @click="showSms = false"
                :disabled="smsSending"
              >
                取消
              </button>
            </div>
            <div v-if="smsOk" class="alert alert-success py-1 mt-2 mb-0">
              {{ smsOk }}
            </div>
            <div v-if="smsErr" class="alert alert-danger py-1 mt-2 mb-0">
              {{ smsErr }}
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- 流转历史：分组 + 时间筛选 + 仅附件 + 特定月/周选择 -->
    <div class="card shadow-sm">
      <div class="card-header fw-semibold d-flex align-items-center justify-content-between flex-wrap gap-2">
        <span>流转历史</span>
        <div class="d-flex align-items-center gap-2 flex-wrap">
          <!-- 分组切换 -->
          <div class="btn-group btn-group-sm" role="group" aria-label="分组">
            <button
              v-for="g in groupingOptions"
              :key="g.key"
              type="button"
              class="btn btn-outline-secondary"
              :class="{ active: viewGrouping===g.key }"
              @click="viewGrouping=g.key"
            >
              {{ g.label }}
            </button>
          </div>
          <!-- 仅附件 -->
          <div class="form-check form-check-sm ms-1">
            <input class="form-check-input" type="checkbox" id="onlyAtt" v-model="onlyAttachments">
            <label class="form-check-label small" for="onlyAtt">仅附件</label>
          </div>
          <!-- 包含已删除 -->
          <div class="form-check form-check-sm ms-1">
            <input class="form-check-input" type="checkbox" id="showDeleted" v-model="showDeleted">
            <label class="form-check-label small" for="showDeleted">包含已删除</label>
          </div>
          <!-- 当按周：周报完成控制区 -->
          <template v-if="viewGrouping==='week'">
            <!-- 周次下拉（筛选 + 操作对象） -->
            <div class="dropdown">
              <button class="btn btn-sm btn-outline-primary dropdown-toggle" type="button" data-bs-toggle="dropdown">
                {{ weekDropdownLabel }}
              </button>
              <ul class="dropdown-menu dropdown-menu-end weeks-menu">
                <li>
                  <a class="dropdown-item" href="#" @click.prevent="selectWeek('')">（全部周）</a>
                </li>
                <li><hr class="dropdown-divider" /></li>
                <li v-for="w in availableWeeks" :key="w.key">
                  <a class="dropdown-item" href="#" @click.prevent="selectWeek(w.key)">
                    {{ w.label }}
                  </a>
                </li>
              </ul>
            </div>
            <!-- 仅周报 + 有权限时显示周完成按钮（作用于当前周次选择） -->
            <template v-if="isPeriodic && isWeekType && (isAdmin || isReviewer || myEmpId===d.createdByEmpId)">
              <button
                v-if="selectedWeekKey && !isSelectedWeekDone"
                class="btn btn-success btn-sm"
                @click="doMarkWeekDone"
              >
                确认该周完成
              </button>
              <button
                v-else-if="selectedWeekKey && isSelectedWeekDone"
                class="btn btn-danger btn-sm"
                @click="doUnmarkWeekDone"
              >
                撤销该周完成
              </button>
              <button
                v-else
                class="btn btn-outline-secondary btn-sm"
                disabled
              >
                请先选择周次
              </button>
            </template>
          </template>
          <!-- 当按月：月报完成控制区 -->
          <template v-if="viewGrouping==='month'">
            <!-- 月次下拉 -->
            <div class="dropdown">
              <button class="btn btn-sm btn-outline-primary dropdown-toggle" type="button" data-bs-toggle="dropdown">
                {{ monthDropdownLabel }}
              </button>
              <ul class="dropdown-menu dropdown-menu-end months-menu">
                <li>
                  <a class="dropdown-item" href="#" @click.prevent="selectMonth('')">（全部月份）</a>
                </li>
                <li><hr class="dropdown-divider" /></li>
                <li v-for="m in availableMonths" :key="m.key">
                  <a class="dropdown-item" href="#" @click.prevent="selectMonth(m.key)">
                    {{ m.label }}
                  </a>
                </li>
              </ul>
            </div>
            <!-- 仅月报 + 有权限时显示月完成按钮（作用于当前月次选择） -->
            <template v-if="isPeriodic && isMonthType && (isAdmin || isReviewer || myEmpId===d.createdByEmpId)">
              <button
                v-if="selectedMonthKey && !isSelectedMonthDone"
                class="btn btn-success btn-sm"
                @click="doMarkMonthDone"
              >
                确认该月完成
              </button>
              <button
                v-else-if="selectedMonthKey && isSelectedMonthDone"
                class="btn btn-danger btn-sm"
                @click="doUnmarkMonthDone"
              >
                撤销该月完成
              </button>
              <button
                v-else
                class="btn btn-outline-secondary btn-sm"
                disabled
              >
                请先选择月份
              </button>
            </template>
          </template>
        </div>
      </div>
      <div class="card-body">
        <template v-for="g in groupedHistory" :key="g.key">
          <div v-if="viewGrouping!=='none'" class="small text-body-secondary fw-semibold mb-1 mt-2">
            {{ g.label }} <span class="badge text-bg-light ms-1">{{ g.items.length }}</span>
          </div>
          <ul class="list-group mb-2">
            <li
              v-for="h in g.items"
              :key="h.id ?? (h.occurredAt + '-' + h.action)"
              class="list-group-item d-flex justify-content-between align-items-start"
              :class="{ 'text-decoration-line-through opacity-75': isStruck(h) }"
            >
              <div>
                <div class="d-flex flex-wrap align-items-center gap-2">
                  <span class="fw-semibold">{{ h.actorName }}</span>
                  <span class="text-body-secondary small">({{ h.actorEmpId }})</span>
                  <span class="text-body-secondary small"> @ {{ fmt(h.occurredAt) }}</span>
                  <span v-if="isStruck(h)" class="badge text-bg-secondary ms-2">已删除</span>
                </div>
                <div :class="['mt-1', historyClass(h)]">{{ historyLine(h) }}</div>
              </div>
              <div class="ms-2 d-flex gap-2">
                <template v-if="canPreviewOrDownload(h)">
                  <button class="btn btn-sm btn-outline-primary"  @click="previewHistoryAttachment(h)">预览</button>
                  <button class="btn btn-sm btn-outline-secondary" @click="downloadHistoryAttachment(h)">下载</button>
                </template>
                <button
                  v-if="canDeleteHistoryComment(h) && !isStruck(h)"
                  class="btn btn-sm btn-outline-danger"
                  @click="onDeleteHistoryComment(h)"
                >删除</button>
                <button
                  v-else-if="canDeleteHistoryAttachment(h) && !isStruck(h)"
                  class="btn btn-sm btn-outline-danger"
                  @click="onDeleteHistoryAttachment(h)"
                >删除</button>
              </div>
            </li>
          </ul>
        </template>
        <div v-if="filteredHistory.length === 0" class="text-body-secondary small">没有符合条件的记录。</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import axios from '@/utils/axios'
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import dayjs from 'dayjs'
import isoWeek from 'dayjs/plugin/isoWeek'
dayjs.extend(isoWeek)
import { useRoute } from 'vue-router'
import {
  getDemand, uploadAttachment, adminApprove, adminReject,
  reviewerApprove, reviewerReject, addComment, deleteComment,
  markCompleted, unmarkCompleted,
  markWeekDone, unmarkWeekDone,
  markMonthDone, unmarkMonthDone,
  updateDemandTitle,sendDemandSms
} from '@/api/demand'
import { useAuthStore } from '@/store/auth'
import { fetchFileBlob, deleteFile, getFileMeta } from '@/api/files'

const route = useRoute()
const id = Number(route.params.id)

const d = ref(null)
const titleEditing = ref(false)
const newTitle = ref('')
const savingTitle = ref(false)
const opinion = ref('')
const picked = ref([])
const thumbs = ref([])
const uploading = ref(false)
const auth = useAuthStore()
const isAdmin = computed(() => auth.user?.roles?.includes('ADMIN'))
const isReviewer = computed(() => auth.user?.roles?.includes('REVIEWER'))
const myEmpId = computed(() => auth.employeeId)
const showDeleted = ref(false)

const isPeriodic  = computed(() => {
  const c = (d.value?.category || '')
  return c.includes('周报') || c.includes('月报')
})
const isWeekType  = computed(() => (d.value?.category || '').includes('周报'))
const isMonthType = computed(() => (d.value?.category || '').includes('月报'))

const groupingOptions = computed(() => {
  if (isWeekType.value) {
    return [
      { key: 'none', label: '不分组' },
      { key: 'week', label: '按周' }
    ]
  }
  if (isMonthType.value) {
    return [
      { key: 'none', label: '不分组' },
      { key: 'month', label: '按月' }
    ]
  }
  return [
    { key: 'none', label: '不分组' },
    { key: 'week', label: '按周' },
    { key: 'month', label: '按月' }
  ]
})
const canMarkCompleted   = computed(() => !isPeriodic.value && d.value?.status === 'REVIEW_APPROVED')
const canUnmarkCompleted = computed(() => !isPeriodic.value && d.value?.status === 'COMPLETED')

const viewGrouping = ref('week')
const timeFilter = ref('all')
const onlyAttachments = ref(false)

watch(groupingOptions, (opts) => {
  const allowed = opts.map(o => o.key)
  if (!allowed.includes(viewGrouping.value)) {
    viewGrouping.value = opts[0]?.key || 'none'
  }
}, { immediate: true })

const selectedMonthKey = ref('')
const selectedWeekKey  = ref('')

const initedByCategory = ref(false)

function initByCategoryOnce () {
  if (initedByCategory.value) return
  const cat = (d.value?.category || '').toString()
  if (cat.includes('周报')) {
    viewGrouping.value = 'week'
    timeFilter.value = 'all'
  } else if (cat.includes('月报')) {
    viewGrouping.value = 'month'
    timeFilter.value = 'all'
  }
  initedByCategory.value = true
}

function pick(e) {
  const files = Array.from(e.target.files || [])
  picked.value = files.length ? files : null
}

function buildAttachmentNotePayload(filename) {
  const name = String(filename || '').trim()

  if (viewGrouping.value === 'week' && selectedWeekKey.value) {
    const key = selectedWeekKey.value
    const label = labelFromPeriodKey(key)
    return JSON.stringify({ key, label, filename: name })
  }

  if (viewGrouping.value === 'month' && selectedMonthKey.value) {
    const key = selectedMonthKey.value
    const label = labelFromPeriodKey(key)
    return JSON.stringify({ key, label, filename: name })
  }
  return name
}

function occurredAtForSelectedKey() {
  if (viewGrouping.value === 'week' && selectedWeekKey.value) {
    const p = parseWeekKey(selectedWeekKey.value)
    if (p) {
      const anchor = dayjs().year(p.year).month(0).date(4)
      return anchor.isoWeek(p.week).startOf('isoWeek').hour(12).minute(0).second(0)
        .format('YYYY-MM-DDTHH:mm:ss')
    }
  }

  if (viewGrouping.value === 'month' && selectedMonthKey.value) {
    const p = parseMonthKey(selectedMonthKey.value)
    if (p) {
      return dayjs().year(p.year).month(p.month - 1).date(15).hour(12).minute(0).second(0)
        .format('YYYY-MM-DDTHH:mm:ss')
    }
  }
  return ''
}

async function doUpload() {
  if (!picked.value || picked.value.length === 0) return
  uploading.value = true
  try {
    for (const f of picked.value) {
      const noteToSend = buildAttachmentNotePayload(f?.name)
      const occurredAt = occurredAtForSelectedKey() // 不想传就删掉这一行以及下面 occurredAt
      await uploadAttachment(id, f, { note: noteToSend, occurredAt })
    }
    await load()
  } catch (e) {
    console.error('附件上传失败', e)
    alert(e?.response?.data?.message || e?.message || '附件上传失败')
  } finally {
    picked.value = null
    uploading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

const fileInput = ref(null)

function fmt(s){
  if (!s) return ''
  return String(s).replace('T',' ').slice(0,19)
}

function syncTitleInput() {
  newTitle.value = d.value?.title || ''
}

function startEditTitle() {
  syncTitleInput()
  titleEditing.value = true
}

function cancelEditTitle() {
  syncTitleInput()
  titleEditing.value = false
}

async function saveTitle() {
  const title = newTitle.value.trim()
  if (!title) {
    alert('标题不能为空')
    return
  }
  savingTitle.value = true
  try {
    d.value = await updateDemandTitle(id, title)
    titleEditing.value = false
  } catch (e) {
    alert(e?.response?.data?.message || '修改标题失败')
  } finally {
    savingTitle.value = false
  }
}

function statusBadgeClass(status){
  switch ((status || '').toUpperCase()) {
    case 'DRAFT': return 'text-bg-secondary'
    case 'SUBMITTED': return 'text-bg-info'
    case 'ADMIN_APPROVED':
    case 'IN_REVIEW': return 'text-bg-primary'
    case 'RETURNED': return 'text-bg-warning'
    case 'REJECTED': return 'text-bg-danger'
    case 'APPROVED':

    case 'DONE': return 'text-bg-success'
    case 'COMPLETED': return 'text-bg-success'
    case 'WEEK_DONE':
    case 'MONTH_DONE': return 'text-bg-success'
    case 'WEEK_NOT_DONE':
    case 'MONTH_NOT_DONE': return 'text-bg-secondary'

    default: return 'text-bg-secondary'
  }
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

    case 'WEEK_DONE': return '上周已完成'
    case 'WEEK_NOT_DONE': return '上周未完成'
    case 'MONTH_DONE': return '上月已完成'
    case 'MONTH_NOT_DONE': return '上月未完成'

    default: return status || '未知'
  }
}

async function approve(who){
  if (!opinion.value.trim()) { alert('请填写审批意见'); return }
  if (who === 'admin') await adminApprove(id, opinion.value.trim())
  else await reviewerApprove(id, opinion.value.trim())
  opinion.value = ''
  await load()
}

async function reject(who){
  if (!opinion.value.trim()) { alert('请填写驳回理由'); return }
  if (who === 'admin') await adminReject(id, opinion.value.trim())
  else await reviewerReject(id, opinion.value.trim())
  opinion.value = ''
  await load()
}
function buildNotePayload(raw) {
  const text = raw.trim()
  if (!text) return ''
  if (isPeriodic.value) {
    if (viewGrouping.value === 'week' && selectedWeekKey.value) {
      const key = selectedWeekKey.value
      const label = labelFromPeriodKey(key)
      return JSON.stringify({ key, label, text })
    }
    if (viewGrouping.value === 'month' && selectedMonthKey.value) {
      const key = selectedMonthKey.value
      const label = labelFromPeriodKey(key)
      return JSON.stringify({ key, label, text })
    }
  }
  return text
}

async function leaveComment(){
  const raw = opinion.value.trim()
  if (!raw) return
  const noteToSend = buildNotePayload(raw)
  await addComment(id, noteToSend)
  opinion.value = ''
  await load()
}

async function doMarkCompleted () {
  await markCompleted(id, opinion.value.trim() || '')
  opinion.value = ''
  await load()
}

async function doUnmarkCompleted () {
  await unmarkCompleted(id, opinion.value.trim() || '')
  opinion.value = ''
  await load()
}

function lastWeekKeyAndLabel () {
  const base = dayjs().subtract(1, 'week')
  const key = `${base.isoWeekYear()}-W${String(base.isoWeek()).padStart(2,'0')}`
  const start = base.startOf('isoWeek')
  const end = base.endOf('isoWeek')
  const label = `${base.isoWeekYear()}年第${base.isoWeek()}周（${start.format('MM/DD')}~${end.format('MM/DD')}）`
  return { key, label }
}

function currentWeekKeyAndLabel () {
  if (selectedWeekKey.value) {
    const w = availableWeeks.value.find(x => x.key === selectedWeekKey.value)
    return { key: selectedWeekKey.value, label: w?.label || selectedWeekKey.value }
  }
  return lastWeekKeyAndLabel()
}

function currentMonthKeyAndLabel () {
  if (selectedMonthKey.value) {
    const m = availableMonths.value.find(x => x.key === selectedMonthKey.value)
    return { key: selectedMonthKey.value, label: m?.label || selectedMonthKey.value }
  }
  const t = dayjs().subtract(1,'month')
  const key = `${t.year()}-${String(t.month()+1).padStart(2,'0')}`
  const label = `${t.year()}年${String(t.month()+1).padStart(2,'0')}月`
  return { key, label }
}

async function doMarkWeekDone () {
  const { key, label } = currentWeekKeyAndLabel()
  await markWeekDone(id, key, label)
  await load()
}
async function doUnmarkWeekDone () {
  const { key, label } = currentWeekKeyAndLabel()
  await unmarkWeekDone(id, key, label)
  await load()
}
async function doMarkMonthDone () {
  const { key, label } = currentMonthKeyAndLabel()
  await markMonthDone(id, key, label)
  await load()
}
async function doUnmarkMonthDone () {
  const { key, label } = currentMonthKeyAndLabel()
  await unmarkMonthDone(id, key, label)
  await load()
}

async function rebuildThumbs() {
  for (const t of thumbs.value) {
    try { URL.revokeObjectURL(t.src) } catch {}
  }
  thumbs.value = []
  if (!d.value?.attachments?.length) return

  for (const a of d.value.attachments) {
    if (deletedAttachmentIds.value.has(Number(a.id))) continue
    if ((a.contentType || '').startsWith('image/')) {
      try {
        const res = await fetchFileBlob(a.id, 'inline')
        const ct = res.headers?.['content-type'] || a.contentType
        const blob = new Blob([res.data], { type: ct })
        const url = URL.createObjectURL(blob)
        thumbs.value.push({
          id: a.id,
          name: a.originalFilename,
          src: url
        })
      } catch (e) {
        console.warn('跳过无效图片附件', a.id, e?.response?.status)
        continue
      }
    }
  }
}

async function load(){
  d.value = await getDemand(id)
  syncTitleInput()
  await rebuildThumbs()
}

const deletedSet = computed(() => {
  const s = new Set()
  for (const h of (d.value?.history || [])) {
    if (h.action === 'COMMENT_DELETED' && h.commentId != null) {
      s.add(`C:${h.commentId}`)
    }
    if (h.action === 'ATTACHMENT_DELETED' && h.attachmentId != null) {
      s.add(`A:${h.attachmentId}`)
    }
  }
  return s
})

function isStruck(h){
  if (h.action === 'COMMENT_ADDED' && h.commentId != null) {
    return deletedSet.value.has(`C:${h.commentId}`)
  }
  if (h.action === 'ATTACHMENT_ADDED' && h.attachmentId != null) {
    return deletedSet.value.has(`A:${h.attachmentId}`)
  }
  return false
}

function canDeleteHistoryComment(h){
  return h?.action === 'COMMENT_ADDED'
      && h?.commentId != null
      && h?.actorEmpId === myEmpId.value;
}

function canDeleteHistoryAttachment(h){
  return h?.action === 'ATTACHMENT_ADDED'
      && h?.attachmentId != null
      && h?.actorEmpId === myEmpId.value;
}

async function onDeleteHistoryAttachment(h) {
  if (!confirm('确定删除该附件？')) return
  try {
    await deleteFile(h.attachmentId)
    try { deletedAttachmentIds.value.add(Number(h.attachmentId)) } catch {}
    await load()
  } catch (e) {
    alert(e?.response?.data?.message || '删除失败')
  }
}

const shownHistory = computed(() => {
  const all = d.value?.history || []
  return all.filter(h => {
    if (h.action === 'COMMENT_DELETED' || h.action === 'ATTACHMENT_DELETED') {
      return false
    }
    if (!showDeleted.value && isStruck(h)) {
      return false
    }
    return true
  })
})

async function onDeleteHistoryComment(h){
  if (!confirm('确定删除这条评论？')) return
  try{
    await deleteComment(h.commentId)
    await load()
  }catch(e){
    alert(e?.response?.data?.message || '删除失败')
  }
}

const deletedAttachmentIds = computed(() => {
  const ids = new Set()
  const list = d.value?.history || []
  for (const h of list) {
    if (h.action === 'ATTACHMENT_DELETED' && h.attachmentId != null) {
      ids.add(Number(h.attachmentId))
    }
  }
  return ids
})

function canPreviewOrDownload(h){
  return (
    h?.action === 'ATTACHMENT_ADDED' &&
    h?.attachmentId != null &&
    !deletedAttachmentIds.value.has(Number(h.attachmentId))
  )
}

const fileMetaCache = {}
async function ensureMeta(id){
  if (!fileMetaCache[id]) {
    try { fileMetaCache[id] = await getFileMeta(id) } catch { fileMetaCache[id] = {} }
  }
  return fileMetaCache[id] || {}
}

async function previewHistoryAttachment(h){
  const id = h.attachmentId
  const win = window.open('', '_blank')
  try{
    const meta = await ensureMeta(id)
    const res = await fetchFileBlob(id, 'inline')
    const ct = res.headers?.['content-type'] || meta.contentType || 'application/octet-stream'
    const blob = new Blob([res.data], { type: ct })
    const url = URL.createObjectURL(blob)
    if (win) win.location.href = url
    else window.open(url, '_blank')
    setTimeout(() => URL.revokeObjectURL(url), 60 * 1000)
  }catch(e){
    if (win) win.close()
    alert(e?.response?.data?.message || '预览失败或附件已被删除')
  }
}

async function downloadHistoryAttachment(h){
  const id = h.attachmentId
  try{
    const meta = await ensureMeta(id)
    const res  = await fetchFileBlob(id, 'download')
    const blob = new Blob([res.data], { type: res.headers?.['content-type'] || 'application/octet-stream' })
    const url  = URL.createObjectURL(blob)
    const a    = document.createElement('a')
    a.href = url
    a.download = meta.name || `file-${id}`
    document.body.appendChild(a)
    a.click()
    a.remove()
    setTimeout(() => URL.revokeObjectURL(url), 10 * 1000)
  }catch(e){
    alert(e?.response?.data?.message || '下载失败或附件已被删除')
  }
}

function weekKey(d) {
  const t = dayjs(d)
  return { y: t.isoWeekYear(), w: t.isoWeek() }
}
function monthKey(d) {
  const t = dayjs(d)
  return { y: t.year(), m: t.month() + 1 }
}

const availableMonths = computed(() => {
  const map = new Map()
  for (const h of shownHistory.value) {
    if (onlyAttachments.value && h.action !== 'ATTACHMENT_ADDED') continue
    const info = historyPeriodInfo(h)
    if (!info || !parseMonthKey(info.key)) continue
    if (!map.has(info.key)) {
      map.set(info.key, { key: info.key, label: info.label, sort: info.sort })
    }
  }
  return [...map.values()].sort((a, b) => b.sort - a.sort)
})

const availableWeeks = computed(() => {
  const map = new Map()
  for (const h of shownHistory.value) {
    if (onlyAttachments.value && h.action !== 'ATTACHMENT_ADDED') continue
    const info = historyPeriodInfo(h)
    if (!info || !parseWeekKey(info.key)) continue
    if (!map.has(info.key)) {
      map.set(info.key, { key: info.key, label: info.label, sort: info.sort })
    }
  }
  return [...map.values()].sort((a, b) => b.sort - a.sort)
})

const monthDropdownLabel = computed(() => selectedMonthKey.value ? `月份：${availableMonths.value.find(m => m.key === selectedMonthKey.value)?.label || selectedMonthKey.value}` : '选择月份')
const weekDropdownLabel  = computed(() => selectedWeekKey.value  ? `周次：${availableWeeks.value.find(w => w.key === selectedWeekKey.value)?.label || selectedWeekKey.value}`   : '选择周')

function selectMonth(key) {
  selectedMonthKey.value = key
  if (key) { timeFilter.value = 'all' }
  selectedWeekKey.value = ''
}
function selectWeek(key) {
  selectedWeekKey.value = key
  if (key) { timeFilter.value = 'all' }
  selectedMonthKey.value = ''
}

function pickNearestWeekKey () {
  const weeks = availableWeeks.value
  if (!weeks.length) return ''

  const { key: targetKey } = lastWeekKeyAndLabel()
  const target = parseWeekKey(targetKey)
  if (!target) return weeks[0].key

  const targetSort = target.sort
  let best = weeks[0]
  let bestDiff = Math.abs((weeks[0].sort ?? 0) - targetSort)

  for (const w of weeks) {
    const diff = Math.abs((w.sort ?? 0) - targetSort)
    if (diff < bestDiff) {
      best = w
      bestDiff = diff
    }
  }
  return best.key
}

function pickNearestMonthKey () {
  const months = availableMonths.value
  if (!months.length) return ''

  const base = dayjs().subtract(1, 'month')
  const targetKey = `${base.year()}-${String(base.month() + 1).padStart(2, '0')}`
  const target = parseMonthKey(targetKey)
  if (!target) return months[0].key

  const targetSort = target.sort
  let best = months[0]
  let bestDiff = Math.abs((months[0].sort ?? 0) - targetSort)

  for (const m of months) {
    const diff = Math.abs((m.sort ?? 0) - targetSort)
    if (diff < bestDiff) {
      best = m
      bestDiff = diff
    }
  }
  return best.key
}

watch(viewGrouping, (v) => {
  if (v === 'none') {
    selectedMonthKey.value = ''
    selectedWeekKey.value  = ''
    return
  }
  if (v === 'week') {
    selectedMonthKey.value = ''
    if (!selectedWeekKey.value) {
      const k = pickNearestWeekKey()
      if (k) selectedWeekKey.value = k
    }
  }
  if (v === 'month') {
    selectedWeekKey.value  = ''
    if (!selectedMonthKey.value) {
      const k = pickNearestMonthKey()
      if (k) selectedMonthKey.value = k
    }
  }
})

const filteredHistory = computed(() => {
  const all = shownHistory.value
  const now = dayjs()
  const { y: thisY, w: thisW } = weekKey(now)
  const { y: lastY, w: lastW } = weekKey(now.subtract(1, 'week'))

  return all.filter(h => {
    if (onlyAttachments.value && h.action !== 'ATTACHMENT_ADDED') return false

    const info = historyPeriodInfo(h)

    if (viewGrouping.value === 'week' && selectedWeekKey.value) {
      if (!info || info.key !== selectedWeekKey.value) return false
    }
    if (viewGrouping.value === 'month' && selectedMonthKey.value) {
      if (!info || info.key !== selectedMonthKey.value) return false
    }
    const t = dayjs(h.occurredAt)
    if (!t.isValid()) return false

    switch (timeFilter.value) {
      case 'thisWeek': {
        const { y, w } = weekKey(t); return y === thisY && w === thisW
      }
      case 'lastWeek': {
        const { y, w } = weekKey(t); return y === lastY && w === lastW
      }
      case 'thisMonth':
        return t.year() === now.year() && t.month() === now.month()
      case 'lastMonth': {
        const lm = now.subtract(1, 'month')
        return t.year() === lm.year() && t.month() === lm.month()
      }
      default:
        return true
    }
  })
})

const groupedHistory = computed(() => {
  const list = filteredHistory.value

  if (viewGrouping.value === 'week') {
    const map = new Map()
    for (const h of list) {
      const info = historyPeriodInfo(h)
      if (!info) continue
      if (!map.has(info.key)) map.set(info.key, { key: info.key, label: info.label, sort: info.sort, items: [] })
      map.get(info.key).items.push(h)
    }
    const groups = [...map.values()].sort((a,b) => b.sort - a.sort)
    groups.forEach(g => g.items.sort((a,b) => dayjs(b.occurredAt).valueOf() - dayjs(a.occurredAt).valueOf()))
    return groups
  }

  if (viewGrouping.value === 'month') {
    const map = new Map()
    for (const h of list) {
      const info = historyPeriodInfo(h)
      if (!info) continue
      if (!map.has(info.key)) map.set(info.key, { ...info, items: [] })
      map.get(info.key).items.push(h)
    }
    const groups = [...map.values()].sort((a,b) => b.sort - a.sort)
    groups.forEach(g => g.items.sort((a,b) => dayjs(b.occurredAt).valueOf() - dayjs(a.occurredAt).valueOf()))
    return groups
  }

  const sorted = list.slice().sort((a,b) => dayjs(b.occurredAt).valueOf() - dayjs(a.occurredAt).valueOf())
  return [{ key: 'all', label: '全部', items: sorted }]
})

const PERSIST_KEY = 'historyViewPrefs/v2'
onMounted(async() => {
  try {
    const saved = JSON.parse(localStorage.getItem(PERSIST_KEY) || '{}')
    if (saved.viewGrouping) viewGrouping.value = saved.viewGrouping
    if (saved.timeFilter) timeFilter.value = saved.timeFilter
    if (typeof saved.onlyAttachments === 'boolean') onlyAttachments.value = saved.onlyAttachments
    if (typeof saved.selectedMonthKey === 'string') selectedMonthKey.value = saved.selectedMonthKey
    if (typeof saved.selectedWeekKey  === 'string') selectedWeekKey.value  = saved.selectedWeekKey
    if (typeof saved.showDeleted === 'boolean') showDeleted.value = saved.showDeleted
  } catch {}
  await load()
  await nextTick()
  initByCategoryOnce()
})

watch([viewGrouping, timeFilter, onlyAttachments, selectedMonthKey, selectedWeekKey, showDeleted],
  ([vg, tf, oa, mk, wk, sd]) => {
    localStorage.setItem(
      PERSIST_KEY,
      JSON.stringify({ viewGrouping: vg, timeFilter: tf, onlyAttachments: oa, selectedMonthKey: mk, selectedWeekKey: wk, showDeleted: sd})
    )
  }
)

watch(availableWeeks, (list) => {
  if (viewGrouping.value !== 'week') return
  if (!selectedWeekKey.value && list.length) {
    const k = pickNearestWeekKey()
    if (k) selectedWeekKey.value = k
    return
  }
  if (selectedWeekKey.value) {
    const ok = list.some(w => w.key === selectedWeekKey.value)
    if (!ok) selectedWeekKey.value = ''
  }
})

watch(availableMonths, (list) => {
  if (viewGrouping.value !== 'month') return

  if (!selectedMonthKey.value && list.length) {
    const k = pickNearestMonthKey()
    if (k) selectedMonthKey.value = k
    return
  }

  if (selectedMonthKey.value) {
    const ok = list.some(m => m.key === selectedMonthKey.value)
    if (!ok) selectedMonthKey.value = ''
  }
})

watch(timeFilter, (tf) => {
  if (tf !== 'all') {
    selectedMonthKey.value = ''
    selectedWeekKey.value  = ''
  }
})

function parseObjNote(note) {
  if (!note || typeof note !== 'string') return null
  try {
    const o = JSON.parse(note)
    return o && typeof o === 'object' ? o : null
  } catch { return null }
}

function labelFromNote(note) {
  const obj = parseObjNote(note)
  if (obj?.label) return String(obj.label)
  const m = /"label"\s*:\s*"([^"]+)"/.exec(note || '')
  if (m) return m[1]
  return (note || '').trim()
}

function normalizeAction(rawAction) {
  const A = String(rawAction || '').toUpperCase()
  const isUndo = /(UNMARK|UNDO|UNSET|CANCEL|REVOKE|ROLLBACK)/.test(A) || /^UN/.test(A)
  const isWeek  = /WEEK/.test(A)  || /W(\d{1,2})/.test(A)
  const isMonth = /MONTH/.test(A) || /-?\d{4}-\d{1,2}/.test(A)
  const isOverall = !isWeek && !isMonth && /(COMPLETE|COMPLETED|DONE|FINISH|FINISHED)/.test(A)
  return { raw: A, isUndo, isWeek, isMonth, isOverall }
}

const POSITIVE_KEYS = [
  'MARK_COMPLETED', 'MARK_DONE_WEEK', 'MARK_DONE_MONTH',
  'WEEK_DONE_MARKED', 'MONTH_DONE_MARKED',
  'CONFIRM', 'COMPLETE'
]
const NEGATIVE_KEYS = [
  'UNMARK_COMPLETED', 'UNMARK_DONE_WEEK', 'UNMARK_DONE_MONTH',
  'WEEK_DONE_UNMARKED', 'MONTH_DONE_UNMARKED',
  'UNDO'
]

function historyClass(h) {
  const act = normalizeAction(h?.action, h?.note)
  const isCompleteLike = act.isOverall || act.isWeek || act.isMonth

  if (isCompleteLike && act.isUndo) {
    return 'text-danger fw-semibold'
  }
  if (isCompleteLike && !act.isUndo) {
    return 'text-success fw-semibold'
  }
  return ''
}

function attNameById(id) {
  const a = (d.value?.attachments || []).find(x => String(x.id) === String(id))
  return a?.originalFilename || `附件 #${id}`
}

function historyLine(h) {
  const act  = normalizeAction(h?.action)
  const note = (h?.note || '').trim()
  const label = labelFromNote(note) || h?.key || ''
  if (act.isWeek) {
    return act.isUndo
      ? `撤销${label ? `${label}` : ''}已完成`
      : `确认${label ? `${label}` : ''}已完成`
  }
  if (act.isMonth) {
    return act.isUndo
      ? `撤销${label ? `${label}` : ''}已完成`
      : `确认${label ? `${label}` : ''}已完成`
  }
  if (act.isOverall) {
    return act.isUndo
      ? `撤销完成${label ? `：${label}` : ''}`
      : `确认完成${label ? `：${label}` : ''}`
  }

  switch (act.raw) {
    case 'CREATE_DRAFT':         return '保存草稿'
    case 'SUBMIT':               return '用户提交'
    case 'ADMIN_APPROVE':        return note ? `主任同意：${note}`   : '主任同意'
    case 'ADMIN_REJECT':         return note ? `主任驳回：${note}`   : '主任驳回'
    case 'REVIEW_APPROVE':       return note ? `科室同意：${note}`   : '科室同意'
    case 'REVIEW_REJECT':        return note ? `科室驳回：${note}`   : '科室驳回'
    case 'COMMENT_ADDED': {
      const obj = parseObjNote(note)
      if (obj && obj.text) return obj.text
      return note ? `${note}` : '发表意见'
    }
    case 'ATTACHMENT_ADDED': {
      const obj = parseObjNote(note)
      const name =
        obj?.filename ||(h?.attachmentId ? attNameById(h.attachmentId) : '') ||'附件'
      const lbl = obj?.label ? `（${obj.label}）` : ''
      return `上传附件：${name}`
    }
  }

  if (/(APPROVE)/.test(act.raw) && /(ADMIN|DIRECTOR)/.test(act.raw)) {
    return note ? `主任同意：${note}` : '主任同意'
  }
  if (/(REJECT)/.test(act.raw) && /(ADMIN|DIRECTOR)/.test(act.raw)) {
    return note ? `主任驳回：${note}` : '主任驳回'
  }
  if (/(APPROVE|PASS)/.test(act.raw) && /(REVIEW|DEPT|SECTION)/.test(act.raw)) {
    return note ? `科室同意：${note}` : '科室同意'
  }
  if (/(REJECT)/.test(act.raw) && /(REVIEW|DEPT|SECTION)/.test(act.raw)) {
    return note ? `科室驳回：${note}` : '科室驳回'
  }
  const safe = label || note
  return safe || '（系统记录）'
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
  const mWeek  = s.match(/\b(\d{4})-W(\d{1,2})\b/)
  if (mWeek)  return `${mWeek[1]}-W${String(mWeek[2]).padStart(2,'0')}`
  const mMonth = s.match(/\b(\d{4})-(\d{1,2})\b/)
  if (mMonth) return `${mMonth[1]}-${String(mMonth[2]).padStart(2,'0')}`
  return null
}

function labelFromPeriodKey(key) {
  const pWeek = parseWeekKey(key)
  if (pWeek) {
    const anchor = dayjs().year(pWeek.year).month(0).date(4)
    const start  = anchor.isoWeek(pWeek.week).startOf('isoWeek')
    const end = start.endOf('isoWeek')
    return `${pWeek.year}年第${pWeek.week}周（${start.format('MM/DD')}~${end.format('MM/DD')}）`
  }
  const pMonth = parseMonthKey(key)
  if (pMonth) {
    return `${pMonth.year}年${String(pMonth.month).padStart(2,'0')}月`
  }
  return key
}

function historyPeriodInfo(h) {
  const act = normalizeAction(h?.action)
  const rawKey =
    extractKeyFromNote(h?.note) ||
    (typeof h?.key === 'string' ? h.key.trim() : '')
  if (rawKey) {
    const wk = parseWeekKey(rawKey)
    if (wk) {
      return {
        key: rawKey,
        label: labelFromPeriodKey(rawKey),
        sort: wk.sort,
      }
    }
    const mk = parseMonthKey(rawKey)
    if (mk) {
      return {
        key: rawKey,
        label: labelFromPeriodKey(rawKey),
        sort: mk.sort,
      }
    }
  }
  if (act.isWeek || act.isMonth) {
    return null
  }
  const t = dayjs(h?.occurredAt)
  if (!t.isValid()) return null

  if (viewGrouping.value === 'week') {
    const y = t.isoWeekYear()
    const w = t.isoWeek()
    const key = `${y}-W${String(w).padStart(2, '0')}`
    const wk = parseWeekKey(key)
    if (!wk) return null
    return {
      key,
      label: labelFromPeriodKey(key),
      sort: wk.sort,
    }
  }

  if (viewGrouping.value === 'month') {
    const y = t.year()
    const m = t.month() + 1
    const key = `${y}-${String(m).padStart(2, '0')}`
    const mk = parseMonthKey(key)
    if (!mk) return null
    return {
      key,
      label: labelFromPeriodKey(key),
      sort: mk.sort,
    }
  }
  return null
}

const partialState = computed(() => {
  const hist = Array.isArray(d.value?.history) ? d.value.history : []
  const sorted = [...hist].sort((a, b) => {
    const ta = dayjs(a?.occurredAt).valueOf() || 0
    const tb = dayjs(b?.occurredAt).valueOf() || 0
    return ta - tb
  })
  const weekState  = new Map()
  const monthState = new Map()
  for (const h of sorted) {
    const act = normalizeAction(h?.action)
    if (!act.isWeek && !act.isMonth) continue
    const key = extractKeyFromNote(h?.note) || (typeof h?.key === 'string' ? h.key : null)
    if (!key) continue
    if (act.isWeek)  weekState.set(key,  !act.isUndo)
    if (act.isMonth) monthState.set(key, !act.isUndo)
  }
  return { weekState, monthState }
})

function parseWeekKey(key) {
  const m = /^(\d{4})-W(\d{1,2})$/.exec(key || '')
  if (!m) return null
  const year = Number(m[1])
  const week = Number(m[2])
  return { year, week, sort: year * 100 + week }
}

function parseMonthKey(key) {
  const m = /^(\d{4})-(\d{1,2})$/.exec(key || '')
  if (!m) return null
  const year = Number(m[1])
  const month = Number(m[2])
  return { year, month, sort: year * 100 + month }
}

const latestWeekDone = computed(() => {
  const map = partialState.value.weekState
  if (!map || map.size === 0) return false
  const { key } = lastWeekKeyAndLabel()
  return !!map.get(key)
})

const latestMonthDone = computed(() => {
  const map = partialState.value.monthState
  if (!map || map.size === 0) return false
  const base = dayjs().subtract(1, 'month')
  const key = `${base.year()}-${String(base.month() + 1).padStart(2, '0')}`
  return !!map.get(key)
})

const statusDisplay = computed(() => {
  const server = String(d.value?.status || '')
  if (!isPeriodic.value) return server
  if ([
    'DRAFT',
    'SUBMITTED',
    'ADMIN_APPROVED',
    'ADMIN_REJECTED',
    'REVIEW_REJECTED',
    'RETURNED'
  ].includes(server)) {
    return server
  }
  if (isWeekType.value) {
    return latestWeekDone.value ? 'WEEK_DONE' : 'WEEK_NOT_DONE'
  }
  if (isMonthType.value) {
    return latestMonthDone.value ? 'MONTH_DONE' : 'MONTH_NOT_DONE'
  }
  return server
})

const isSelectedWeekDone = computed(() => {
  const key = currentWeekKeyAndLabel().key
  return !!partialState.value.weekState.get(key)
})

const isSelectedMonthDone = computed(() => {
  const key = currentMonthKeyAndLabel().key
  return !!partialState.value.monthState.get(key)
})
//发信息
const showSms = ref(false)
const smsText = ref('')
const smsTouched = ref(false)
const smsSending = ref(false)
const smsOk = ref('')
const smsErr = ref('')

const canSendSms = computed(() => {
  const p = d.value?.contactPhone
  return !!p && p.trim().length > 0
})

const defaultSmsText = computed(() => {
  const title = d.value?.title || ''
  const st = statusLabel(statusDisplay.value)
  return `您的需求:<<${title}>> 状态已更新，当前状态：${st}`
})

function openSmsPanel() {
  showSms.value = true
  smsOk.value = ''
  smsErr.value = ''
  smsTouched.value = false
  smsText.value = defaultSmsText.value
}

function closeSmsPanel() {
  showSms.value = false
  smsOk.value = ''
  smsErr.value = ''
}

function toggleSms() {
  if (!showSms.value) openSmsPanel()
  else closeSmsPanel()
}

watch([() => statusDisplay.value, () => d.value?.title], () => {
  if (showSms.value && !smsTouched.value) {
    smsText.value = defaultSmsText.value
  }
})

async function sendSms() {
  smsOk.value = ''
  smsErr.value = ''

  if (!canSendSms.value) {
    smsErr.value = '联系电话为空，无法发送短信'
    return
  }
  const content = (smsText.value || '').trim()
  if (!content) {
    smsErr.value = '短信内容不能为空'
    return
  }

  try {
    smsSending.value = true
    await axios.post(`/api/demands/${d.value.id}/sms/notify`, { content })

    smsOk.value = '短信已提交发送'
    closeSmsPanel()
  } catch (e) {
    smsErr.value = e?.response?.data?.message || e?.message || '短信发送失败'
  } finally {
    smsSending.value = false
  }
}

watch(() => d.value?.id, () => {
  closeSmsPanel()
  smsText.value = ''
  smsTouched.value = false
  smsSending.value = false
})


</script>

<style scoped>
.text-wrap { word-wrap: break-word; }
.small.text-body-secondary.fw-semibold { padding-left: .25rem; }
.months-menu, .weeks-menu {
  max-height: 320px;
  overflow: auto;
}
</style>
