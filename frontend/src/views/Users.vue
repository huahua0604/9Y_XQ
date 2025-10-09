<template>
  <div class="container my-4" style="max-width:1100px;">
    <h2 class="h4 mb-3">用户管理</h2>

    <!-- ========== 新增用户 ========== -->
    <div class="card shadow-sm mb-3">
      <div class="card-header fw-semibold">新增用户</div>
      <div class="card-body">
        <div class="row g-3">
          <div class="col-12 col-md-4">
            <label class="form-label">工号</label>
            <input class="form-control" v-model.trim="form.employeeId" placeholder="如：100001" />
          </div>
          <div class="col-12 col-md-4">
            <label class="form-label">姓名</label>
            <input class="form-control" v-model.trim="form.name" placeholder="张三" />
          </div>
          <div class="col-12 col-md-4">
            <label class="form-label">科室</label>
            <input class="form-control" v-model.trim="form.department" placeholder="信息科" />
          </div>
          <div class="col-12 col-md-4">
            <label class="form-label">密码 <span class="text-body-secondary">(可不填，默认两遍工号)</span></label>
            <input class="form-control" v-model.trim="form.password" type="password" placeholder="8-64位" />
          </div>

          <div class="col-12 col-md-4">
            <label class="form-label d-block">角色</label>
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" value="USER" v-model="form.roles" id="role-user">
              <label class="form-check-label" for="role-user">用户</label>
            </div>
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" value="REVIEWER" v-model="form.roles" id="role-reviewer">
              <label class="form-check-label" for="role-reviewer">审核员(信息科成员)</label>
            </div>
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" value="ADMIN" v-model="form.roles" id="role-admin">
              <label class="form-check-label" for="role-admin">管理员(主任)</label>
            </div>
          </div>

          <div class="col-12 col-md-4">
            <label class="form-label">首登需改密</label>
            <select class="form-select" v-model="form.mustChangePassword">
              <option :value="true">是</option>
              <option :value="false">否</option>
            </select>
          </div>
        </div>

        <div class="d-flex justify-content-end gap-2 mt-3">
          <button class="btn btn-primary" @click="onCreate">创建用户</button>
        </div>

        <p v-if="tempPwd" class="text-success small mt-2">
          已创建，临时密码：<b>{{ tempPwd }}</b>
        </p>
      </div>
    </div>

    <!-- ========== 全部用户 ========== -->
    <div class="card shadow-sm">
      <div class="card-header fw-semibold">全部用户</div>
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-hover table-striped align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th style="width:70px;">ID</th>
                <th style="width:140px;">工号</th>
                <th style="width:140px;">姓名</th>
                <th style="width:160px;">科室</th>
                <th>角色</th>
                <th style="width:120px;">首登改密</th>
                <th style="width:220px;">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in users" :key="u.id">
                <td class="text-body-secondary">#{{ u.id }}</td>
                <td>{{ u.employeeId }}</td>
                <td class="fw-semibold">{{ u.name }}</td>
                <td>{{ u.department }}</td>
                <td>
                  <span
                    v-for="r in u.roles"
                    :key="r"
                    class="badge me-1"
                    :class="roleBadgeClass(r)"
                  >{{ roleLabel(r) }}</span>
                </td>
                <td>
                  <span class="badge" :class="u.mustChangePassword ? 'text-bg-warning' : 'text-bg-secondary'">
                    {{ u.mustChangePassword ? '是' : '否' }}
                  </span>
                </td>
                <td>
                  <div class="btn-group btn-group-sm" role="group">
                    <button class="btn btn-outline-primary" @click="onReset(u)">重置密码</button>
                    <button class="btn btn-outline-warning" @click="openEdit(u)">修改</button>
                    <button class="btn btn-outline-danger" @click="onDelete(u)">删除</button>
                  </div>
                </td>
              </tr>
              <tr v-if="!users.length">
                <td colspan="7" class="text-center text-body-secondary py-4">暂无用户</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- ========== 编辑弹窗（Bootstrap 样式，Vue 控制） ========== -->
    <div v-if="editVisible">
      <!-- backdrop -->
      <div class="modal-backdrop fade show"></div>

      <div
        class="modal fade show"
        tabindex="-1"
        style="display:block;"
        role="dialog"
        aria-modal="true"
        @click.self="closeEdit"
      >
        <div class="modal-dialog modal-lg modal-dialog-centered">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">修改用户信息</h5>
              <button type="button" class="btn-close" aria-label="Close" @click="closeEdit"></button>
            </div>

            <div class="modal-body">
              <div class="row g-3">
                <div class="col-12 col-md-4">
                  <label class="form-label">工号</label>
                  <input class="form-control" v-model.trim="editForm.employeeId" />
                </div>
                <div class="col-12 col-md-4">
                  <label class="form-label">姓名</label>
                  <input class="form-control" v-model.trim="editForm.name" />
                </div>
                <div class="col-12 col-md-4">
                  <label class="form-label">科室</label>
                  <input class="form-control" v-model.trim="editForm.department" />
                </div>
                <div class="col-12">
                  <label class="form-label d-block">角色</label>
                  <div class="form-check form-check-inline">
                    <input class="form-check-input" type="checkbox" value="USER" v-model="editForm.roles" id="edit-role-user">
                    <label class="form-check-label" for="edit-role-user">用户</label>
                  </div>
                  <div class="form-check form-check-inline">
                    <input class="form-check-input" type="checkbox" value="REVIEWER" v-model="editForm.roles" id="edit-role-reviewer">
                    <label class="form-check-label" for="edit-role-reviewer">审核员(信息科成员)</label>
                  </div>
                  <div class="form-check form-check-inline">
                    <input class="form-check-input" type="checkbox" value="ADMIN" v-model="editForm.roles" id="edit-role-admin">
                    <label class="form-check-label" for="edit-role-admin">管理员(主任)</label>
                  </div>
                </div>
                <div class="col-12 col-md-4">
                  <label class="form-label">首登需改密</label>
                  <select class="form-select" v-model="editForm.mustChangePassword">
                    <option :value="true">是</option>
                    <option :value="false">否</option>
                  </select>
                </div>
              </div>
            </div>

            <div class="modal-footer">
              <button class="btn btn-outline-secondary" @click="closeEdit">取消</button>
              <button class="btn btn-primary" @click="onEditSave">保存</button>
            </div>
          </div>
        </div>
      </div>
    </div> <!-- /modal -->
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  adminCreateUser,
  adminListUsers,
  adminResetPassword,
  adminUpdateUser,
  adminDeleteUser
} from '@/api/admin'

const form = ref({
  employeeId: '',
  name: '',
  department: '',
  password: '',
  roles: ['USER'],
  mustChangePassword: true
})

const users = ref([])
const tempPwd = ref('')

// ======= 编辑弹窗状态 =======
const editVisible = ref(false)
const editingId = ref(null)
const editForm = ref({
  employeeId: '',
  name: '',
  department: '',
  roles: [],
  mustChangePassword: true
})

async function load () {
  users.value = await adminListUsers()
}

function roleLabel (role) {
  switch (role) {
    case 'ADMIN': return '管理员(主任)'
    case 'REVIEWER': return '审核员(信息科成员)'
    case 'USER': return '普通用户'
    default: return role
  }
}
function roleBadgeClass (role) {
  switch (role) {
    case 'ADMIN': return 'text-bg-danger'
    case 'REVIEWER': return 'text-bg-info'
    case 'USER': return 'text-bg-secondary'
    default: return 'text-bg-light'
  }
}

async function onCreate () {
  if (!form.value.employeeId?.trim() || !form.value.name?.trim()) {
    return alert('请填写工号和姓名')
  }
  if (form.value.password && (form.value.password.length < 8 || form.value.password.length > 64)) {
    return alert('密码长度需在 8-64 位之间')
  }
  const payload = {
    employeeId: form.value.employeeId.trim(),
    name: form.value.name.trim(),
    department: form.value.department?.trim() || undefined,
    roles: (form.value.roles && form.value.roles.length) ? form.value.roles : undefined,
    mustChangePassword: form.value.mustChangePassword,
    ...(form.value.password ? { password: form.value.password } : {})
  }
  try {
    const res = await adminCreateUser(payload)
    const tmp = res?.tempPassword
    tempPwd.value = tmp || ''
    alert(tmp ? `创建成功，临时密码：${tmp}` : '创建成功')
    form.value = { employeeId:'', name:'', department:'', password:'', roles:['USER'], mustChangePassword:true }
    await load()
  } catch (e) {
    const errors = e?.response?.data?.errors
    if (errors && typeof errors === 'object') {
      alert('参数校验失败：\n' + Object.entries(errors).map(([k,v])=>`${k}: ${v}`).join('\n'))
    } else {
      alert(e?.response?.data?.message || '创建失败')
    }
  }
}

async function onReset (u) {
  if (!confirm(`确定把 ${u.name}（${u.employeeId}）重置为默认密码？`)) return
  try {
    await adminResetPassword(u.id)
    alert(`已重置，临时密码：${u.employeeId}${u.employeeId}`)
  } catch (e) {
    alert(e?.response?.data?.message || '重置失败')
  }
}

function openEdit (u) {
  editingId.value = u.id
  editForm.value = {
    employeeId: u.employeeId,
    name: u.name,
    department: u.department,
    roles: Array.isArray(u.roles) ? [...u.roles] : [],
    mustChangePassword: !!u.mustChangePassword
  }
  editVisible.value = true
}
function closeEdit () {
  editVisible.value = false
  editingId.value = null
}
async function onEditSave () {
  const id = editingId.value
  if (!id) return
  const payload = {
    employeeId: editForm.value.employeeId?.trim(),
    name: editForm.value.name?.trim(),
    department: editForm.value.department?.trim() || '',
    roles: editForm.value.roles,
    mustChangePassword: !!editForm.value.mustChangePassword
  }
  if (!payload.employeeId || !payload.name) {
    return alert('工号与姓名不能为空')
  }
  try {
    await adminUpdateUser(id, payload)
    alert('已保存')
    closeEdit()
    await load()
  } catch (e) {
    alert(e?.response?.data?.message || '保存失败')
  }
}
async function onDelete (u) {
  if (!confirm(`确定删除用户：${u.name}（${u.employeeId}）？`)) return
  try {
    await adminDeleteUser(u.id)
    await load()
  } catch (e) {
    alert(e?.response?.data?.message || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
/* 仅保留少量微调；其余交给 Bootstrap */
.modal-backdrop { z-index: 1050; }
.modal { z-index: 1055; }
</style>
