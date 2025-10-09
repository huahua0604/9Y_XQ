<template>
  <div class="container my-4" v-if="d">
    <!-- 标题与状态 -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="d-flex align-items-center gap-2">
        <h2 class="h5 mb-0">#{{ d.id }} {{ d.title }}</h2>
        <span class="badge" :class="statusBadgeClass(d.status)">{{ statusLabel(d.status) }}</span>
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

    <!-- 附件 -->
    <div class="card shadow-sm mb-3">
      <div class="card-header fw-semibold">附件</div>
      <div class="card-body">
        <div class="alert alert-light border py-2 mb-3" role="alert">
          新上传的附件会出现在下方 <b>流转历史</b> 中；如需删除，请在流转历史中点击该条记录的“删除”。（仅允许删除本人上传的附件）
        </div>
        <div class="input-group">
          <input type="file" class="form-control" ref="fileInput" multiple @change="pick">
          <button class="btn btn-primary" @click="doUpload" :disabled="!picked || uploading">{{ uploading ? '上传中…' : '上传附件' }}</button>
        </div>
      </div>
    </div>

    <!-- 意见与审批 -->
    <div class="card shadow-sm mb-3">
      <div class="card-header fw-semibold">意见</div>
      <div class="card-body">
        <div class="mb-2">
          <textarea
            class="form-control"
            rows="4"
            v-model="opinion"
            placeholder="填写意见..."
          ></textarea>
        </div>

        <div class="d-flex flex-wrap gap-2">
          <!-- 审批按钮：主任 -->
          <template v-if="isAdmin">
            <button
              class="btn btn-success"
              v-if="d.status==='SUBMITTED'"
              @click="approve('admin')"
            >主任同意</button>
            <button
              class="btn btn-danger"
              v-if="d.status==='SUBMITTED'"
              @click="reject('admin')"
            >主任驳回</button>
          </template>

          <!-- 审批按钮：科室负责人 -->
          <template v-if="isReviewer">
            <button
              class="btn btn-success"
              v-if="d.status==='ADMIN_APPROVED'"
              @click="approve('reviewer')"
            >科室同意</button>
            <button
              class="btn btn-danger"
              v-if="d.status==='ADMIN_APPROVED'"
              @click="reject('reviewer')"
            >科室驳回</button>
          </template>

          <button
            class="btn btn-outline-secondary"
            v-if="isAdmin || isReviewer"
            @click="leaveComment"
          >仅发表意见</button>
          <button
            class="btn btn-primary"
            v-else
            @click="leaveComment"
          >发布</button>
        </div>
      </div>
    </div>

    <!-- 评论列表（新增：支持删除自己评论） -->
    <!-- <div class="card shadow-sm mb-3">
      <div class="card-header fw-semibold d-flex align-items-center justify-content-between">
        <span>评论</span>
        <span class="badge text-bg-light">{{ d.comments?.length || 0 }}</span>
      </div>
      <div class="card-body p-0">
        <ul class="list-group list-group-flush">
          <li v-for="c in d.comments" :key="c.id" class="list-group-item">
            <div class="d-flex justify-content-between align-items-start">
              <div>
                <div class="mb-1">{{ c.content }}</div>
                <small class="text-body-secondary">
                  {{ c.authorName }}（{{ c.authorRole }}）
                  · {{ fmt(c.createdAt) }}
                  · 工号 {{ c.authorEmpId }}
                </small>
              </div>
              <button
                v-if="canDeleteComment(c)"
                class="btn btn-sm btn-outline-danger"
                @click="onDeleteComment(c)"
              >删除</button>
            </div>
          </li>
          <li v-if="!d.comments?.length" class="list-group-item text-body-secondary">暂无评论</li>
        </ul>
      </div>
    </div> -->

    <!-- 流转历史 -->
    <div class="card shadow-sm">
      <div class="card-header fw-semibold d-flex align-items-center justify-content-between">
        <span>流转历史</span>
        <span class="badge text-bg-light">{{ d.history?.length || 0 }}</span>
      </div>
      <div class="card-body">
        <ul class="list-group">
          <li
            v-for="h in shownHistory"
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
              <div v-if="h.note" class="mt-1">{{ h.note }}</div>
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
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  getDemand, uploadAttachment, adminApprove, adminReject,
  reviewerApprove, reviewerReject, addComment, deleteComment
} from '@/api/demand'
import { fetchFileBlob, deleteFile } from '@/api/files'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const id = Number(route.params.id)

const d = ref(null)
const opinion = ref('')
const picked = ref([])
const thumbs = ref([])
const uploading = ref(false)
const auth = useAuthStore()
const isAdmin = computed(() => auth.user?.roles?.includes('ADMIN'))
const isReviewer = computed(() => auth.user?.roles?.includes('REVIEWER'))
const myEmpId = computed(() => auth.employeeId)

function pick(e) {
  // 允许多选，转换为数组
  const files = Array.from(e.target.files || [])
  picked.value = files.length ? files : null
}

async function doUpload() {
  if (!picked.value || picked.value.length === 0) return
  uploading.value = true
  try {
    // 逐个上传
    for (const f of picked.value) {
      await uploadAttachment(id, f)
    }
    window.location.reload()
  } finally {
    picked.value = null
    uploading.value = false
    if (fileInput.value) {
      fileInput.value.value = '' // 清空文件框
    }
  }
}


const fileInput = ref(null)

function fmt(s){
  if (!s) return ''
  return String(s).replace('T',' ').slice(0,19)
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
async function leaveComment(){
  if (!opinion.value.trim()) return
  await addComment(id, opinion.value.trim())
  opinion.value = ''
  await load()
}

async function rebuildThumbs(){
  for (const t of thumbs.value) {
    try { URL.revokeObjectURL(t.src) } catch {}
  }
  thumbs.value = []
  if (!d.value?.attachments?.length) return
  for (const a of d.value.attachments){
    if ((a.contentType || '').startsWith('image/')){
      const res = await fetchFileBlob(a.id, 'inline')
      const ct = res.headers?.['content-type'] || a.contentType
      const blob = new Blob([res.data], { type: ct })
      const url = URL.createObjectURL(blob)
      thumbs.value.push({ id: a.id, name: a.originalFilename, src: url })
    }
  }
}

async function load(){
  d.value = await getDemand(id)
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

async function onDeleteHistoryAttachment(h){
  if (!confirm('确定删除该附件？')) return
  try{
    await deleteFile(h.attachmentId)
    await load()
  }catch(e){
    alert(e?.response?.data?.message || '删除失败')
  }
}

const shownHistory = computed(() => {
  const all = d.value?.history || []
  return all.filter(h => h.action !== 'COMMENT_DELETED' && h.action !== 'ATTACHMENT_DELETED')
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

function canPreviewOrDownload(h){
  return h?.action === 'ATTACHMENT_ADDED' && h?.attachmentId != null && !isStruck(h)
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

onMounted(load)
</script>

<style scoped>
.text-wrap { word-wrap: break-word; }
</style>