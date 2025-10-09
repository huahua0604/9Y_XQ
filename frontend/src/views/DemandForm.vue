<template>
  <div class="container-lg py-4">
    <div class="d-flex align-items-center mb-3">
      <h2 class="mb-0">新建需求</h2>
    </div>

    <form @submit.prevent="onSubmit" class="needs-validation" novalidate>
      <div class="card shadow-sm mb-4">
        <div class="card-body">
          <div class="row g-3">
            <!-- 标题 -->
            <div class="col-12">
              <label class="form-label">标题<span class="text-danger">*</span></label>
              <input
                v-model.trim="f.title"
                required
                maxlength="120"
                class="form-control"
                placeholder="请输入标题（不超过 120 字）"
              />
            </div>

            <!-- 申请类型 -->
            <div class="col-12 col-md-6">
              <label class="form-label">申请类型<span class="text-danger">*</span></label>
              <select v-model="f.category" class="form-select">
                <option>临时</option><option>常态化(如:周报,月报等)</option>
                <option>其他</option>
              </select>
            </div>
            <!-- 期望完成日期 -->
            <!-- <div class="col-12 col-md-6">
              <label class="form-label">期望完成日期</label>
              <input type="date" v-model="f.desiredDueDate" class="form-control" />
            </div> -->

            <!-- 预算预估 -->
            <!-- <div class="col-12 col-md-6">
              <label class="form-label">预算预估(元)</label>
              <div class="input-group">
                <input
                  type="number"
                  step="0.01"
                  v-model.number="f.budgetEstimate"
                  class="form-control"
                  placeholder="例如:10000"
                />
                <span class="input-group-text">CNY</span>
              </div>
            </div> -->

            <!-- 影响范围 -->
            <div class="col-12 col-md-6">
              <label class="form-label">影响范围</label>
              <select v-model="f.impactScope" class="form-select">
                <option>个人</option><option>本科室</option><option>跨科室</option><option>全院</option>
              </select>
            </div>

            <!-- 关联系统/设备 -->
            <!-- <div class="col-12 col-md-6">
              <label class="form-label">关联系统/设备</label>
              <input v-model="f.relatedSystems" class="form-control" placeholder="例如：HIS、PACS、某型号设备…" />
            </div> -->

            <!-- 联系电话 -->
            <div class="col-12 col-md-6">
              <label class="form-label">联系电话</label>
              <input v-model="f.contactPhone" class="form-control" placeholder="手机号或座机" />
            </div>

            <!-- 地点/科室 -->
            <div class="col-12 col-md-6">
              <label class="form-label">地点/科室</label>
              <input v-model="f.location" class="form-control" placeholder="如：门诊一楼、财务科" />
            </div>

            <!-- 需求详情 -->
            <div class="col-12">
              <label class="form-label">需求详情</label>
              <textarea
                v-model="f.description"
                rows="6"
                class="form-control"
                placeholder="详细描述问题、期望、影响、复现步骤等"
              ></textarea>
            </div>
          </div>
        </div>
      </div>

      <!-- 附件 -->
      <div class="card shadow-sm mb-4">
        <div class="card-header bg-body-tertiary">
          <h5 class="mb-0">附件</h5>
        </div>
        <div class="card-body">
          <input type="file" class="form-control" multiple @change="onPickFiles" />
          <div v-if="picked.length" class="form-text mt-2">
            待上传：<strong>{{ picked.length }}</strong> 个文件（将在创建成功后逐个上传）
          </div>
        </div>
      </div>

      <!-- 动作区 -->
      <div class="d-flex justify-content-end gap-2">
        <button type="button" class="btn btn-outline-secondary" @click="saveDraft">
          保存草稿
        </button>
        <button class="btn btn-primary">
          提交审批
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { createDemand, uploadAttachment } from '@/api/demand'
import { useRouter } from 'vue-router'

const router = useRouter()
const f = reactive<any>({
  title:'', category:'',
  desiredDueDate:'', budgetEstimate: null, impactScope:'',
  relatedSystems:'', contactPhone:'', location:'', description:'', submit: true
})
const picked = ref<File[]>([])

function onPickFiles(e: Event){
  const files = (e.target as HTMLInputElement).files
  picked.value = files ? Array.from(files) : []
}
async function saveDraft(){
  f.submit = false
  await create()
}
async function onSubmit(){
  f.submit = true
  await create()
}
async function create(){
  const res = await createDemand(f)
  if (picked.value.length){
    for (const file of picked.value){
      await uploadAttachment(res.id, file)
    }
  }
  router.push(`/demands/${res.id}`)
}
</script>

<style scoped>
</style>
