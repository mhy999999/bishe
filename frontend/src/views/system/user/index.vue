<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.username" placeholder="用户名" style="width: 200px;" class="filter-item"
        @keyup.enter="handleFilter" />
      <el-button class="filter-item" type="primary" icon="Search" @click="handleFilter">
        搜索
      </el-button>
      <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="Edit" @click="handleCreate">
        新增用户
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="用户ID" prop="userId" align="center" width="100" />
      <el-table-column label="用户名" prop="username" align="center" />
      <el-table-column label="昵称" prop="nickname" align="center" />
      <el-table-column label="部门" prop="deptName" align="center" width="140">
        <template #default="{ row }">
          <span>{{ row.deptName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="角色" align="center" min-width="160">
        <template #default="{ row }">
          <span v-if="!row.roleNames || row.roleNames.length === 0">-</span>
          <el-tag v-else v-for="name in row.roleNames" :key="name" size="small" style="margin-right: 6px;">
            {{ name }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="邮箱" prop="email" align="center" />
      <el-table-column label="手机" prop="phone" align="center" />
      <el-table-column label="状态" align="center" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">
            {{ row.status === 0 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" align="center" width="180" />
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleUpdate(row)">
            编辑
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog :title="textMap[dialogStatus]" v-model="dialogFormVisible" width="520px">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="90px"
        style="width: 420px; margin-left: 40px;">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="temp.username" :disabled="dialogStatus === 'update'" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="temp.password" type="password" show-password placeholder="编辑时留空不修改" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="temp.nickname" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="temp.email" />
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="temp.phone" />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-tree-select v-model="temp.deptId" :data="deptTreeData" filterable default-expand-all check-strictly
            style="width: 100%;" :props="{ children: 'children', label: 'deptName', value: 'deptId' }"
            placeholder="请选择部门" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="temp.status" style="width: 100%;">
            <el-option label="正常" :value="0" />
            <el-option label="禁用" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="roleIds" multiple filterable style="width: 100%;" placeholder="选择角色">
            <el-option v-for="role in roleOptions" :key="role.roleId" :label="role.roleName" :value="role.roleId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">
            取消
          </el-button>
          <el-button type="primary" @click="dialogStatus === 'create' ? createData() : updateData()">
            确认
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, createUser, updateUser, deleteUser, getRoleAll, getUserRoleIds, updateUserRoleIds, getDeptTree } from '@/api/system'

const list = ref([])
const total = ref(0)
const listLoading = ref(true)
const dialogFormVisible = ref(false)
const dialogStatus = ref('')
const roleOptions = ref([])
const roleIds = ref([])
const deptTreeData = ref([])

const textMap = {
  update: '编辑用户',
  create: '新增用户'
}

const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  username: undefined
})

const temp = reactive({
  userId: undefined,
  deptId: undefined,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  status: 0
})

const rules = {
  username: [{ required: true, message: '用户名必填', trigger: 'blur' }],
  password: [
    {
      validator: (rule, value, callback) => {
        if (dialogStatus.value === 'create' && !value) {
          callback(new Error('密码必填'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const fetchRoleOptions = async () => {
  if (roleOptions.value.length > 0) return
  const roles = await getRoleAll()
  roleOptions.value = roles || []
}

const fetchDeptTree = async () => {
  if (deptTreeData.value.length > 0) return
  const depts = await getDeptTree()
  deptTreeData.value = depts || []
}

const getList = () => {
  listLoading.value = true
  getUserList(listQuery).then(pageData => {
    if (pageData && pageData.records) {
      list.value = pageData.records
      total.value = pageData.total
    } else {
      list.value = []
      total.value = 0
    }
    listLoading.value = false
  }).catch(() => {
    listLoading.value = false
  })
}

const handleFilter = () => {
  listQuery.pageNum = 1
  getList()
}

const resetTemp = () => {
  temp.userId = undefined
  temp.deptId = undefined
  temp.username = ''
  temp.password = ''
  temp.nickname = ''
  temp.email = ''
  temp.phone = ''
  temp.status = 0
  roleIds.value = []
}

const handleCreate = async () => {
  resetTemp()
  await fetchRoleOptions()
  await fetchDeptTree()
  dialogStatus.value = 'create'
  dialogFormVisible.value = true
}

const handleUpdate = async (row) => {
  resetTemp()
  await fetchRoleOptions()
  await fetchDeptTree()
  Object.assign(temp, row)
  temp.password = ''
  const ids = await getUserRoleIds(row.userId)
  roleIds.value = ids || []
  dialogStatus.value = 'update'
  dialogFormVisible.value = true
}

const createData = async () => {
  const created = await createUser({ ...temp })
  if (created && created.userId) {
    await updateUserRoleIds(created.userId, roleIds.value || [])
  }
  dialogFormVisible.value = false
  ElMessage({ message: '创建成功', type: 'success', duration: 2000 })
  getList()
}

const updateData = async () => {
  const payload = { ...temp }
  if (!payload.password) {
    delete payload.password
  }
  await updateUser(payload)
  await updateUserRoleIds(temp.userId, roleIds.value || [])
  dialogFormVisible.value = false
  ElMessage({ message: '更新成功', type: 'success', duration: 2000 })
  getList()
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该用户?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteUser(row.userId)
    ElMessage({ type: 'success', message: '删除成功!' })
    getList()
  })
}

onMounted(() => {
  getList()
})
</script>
