<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.roleName" placeholder="角色名称" style="width: 200px;" class="filter-item"
        @keyup.enter="handleFilter" />
      <el-button class="filter-item" type="primary" icon="Search" @click="handleFilter">
        搜索
      </el-button>
      <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="Edit" @click="handleCreate">
        新增角色
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="角色ID" prop="roleId" align="center" width="100" />
      <el-table-column label="角色名称" prop="roleName" align="center" />
      <el-table-column label="角色标识" prop="roleKey" align="center" />
      <el-table-column label="状态" align="center" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">
            {{ row.status === 0 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" align="center" width="180" />
      <el-table-column label="操作" align="center" width="320" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleUpdate(row)">
            编辑
          </el-button>
          <el-button type="warning" size="small" @click="handleAssignMenu(row)">
            分配菜单
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
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="temp.roleName" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="temp.roleKey" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="temp.status" style="width: 100%;">
            <el-option label="正常" :value="0" />
            <el-option label="禁用" :value="1" />
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

    <el-dialog title="分配菜单" v-model="menuDialogVisible" width="520px">
      <el-tree ref="menuTreeRef" :data="menuTreeData" node-key="menuId" show-checkbox default-expand-all
        :props="{ children: 'children', label: 'menuName' }" />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="menuDialogVisible = false">
            取消
          </el-button>
          <el-button type="primary" @click="saveRoleMenus">
            确认
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoleList, createRole, updateRole, deleteRole, getMenuTree, getRoleMenuIds, updateRoleMenuIds } from '@/api/system'

const list = ref([])
const total = ref(0)
const listLoading = ref(true)
const dialogFormVisible = ref(false)
const dialogStatus = ref('')

const menuDialogVisible = ref(false)
const menuTreeData = ref([])
const menuTreeRef = ref()
const currentRoleId = ref()

const textMap = {
  update: '编辑角色',
  create: '新增角色'
}

const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  roleName: undefined
})

const temp = reactive({
  roleId: undefined,
  roleName: '',
  roleKey: '',
  status: 0
})

const rules = {
  roleName: [{ required: true, message: '角色名称必填', trigger: 'blur' }],
  roleKey: [{ required: true, message: '角色标识必填', trigger: 'blur' }]
}

const getList = () => {
  listLoading.value = true
  getRoleList(listQuery).then(pageData => {
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
  temp.roleId = undefined
  temp.roleName = ''
  temp.roleKey = ''
  temp.status = 0
}

const handleCreate = () => {
  resetTemp()
  dialogStatus.value = 'create'
  dialogFormVisible.value = true
}

const handleUpdate = (row) => {
  resetTemp()
  Object.assign(temp, row)
  dialogStatus.value = 'update'
  dialogFormVisible.value = true
}

const createData = async () => {
  await createRole({ ...temp })
  dialogFormVisible.value = false
  ElMessage({ message: '创建成功', type: 'success', duration: 2000 })
  getList()
}

const updateData = async () => {
  await updateRole({ ...temp })
  dialogFormVisible.value = false
  ElMessage({ message: '更新成功', type: 'success', duration: 2000 })
  getList()
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该角色?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteRole(row.roleId)
    ElMessage({ type: 'success', message: '删除成功!' })
    getList()
  })
}

const ensureMenuTreeLoaded = async () => {
  if (menuTreeData.value.length > 0) return
  const tree = await getMenuTree()
  menuTreeData.value = tree || []
}

const handleAssignMenu = async (row) => {
  currentRoleId.value = row.roleId
  await ensureMenuTreeLoaded()
  const ids = await getRoleMenuIds(row.roleId)
  menuDialogVisible.value = true
  await nextTick()
  if (menuTreeRef.value) {
    menuTreeRef.value.setCheckedKeys(ids || [])
  }
}

const saveRoleMenus = async () => {
  if (!currentRoleId.value) return
  const checked = menuTreeRef.value ? menuTreeRef.value.getCheckedKeys(false) : []
  const half = menuTreeRef.value ? menuTreeRef.value.getHalfCheckedKeys() : []
  const menuIds = Array.from(new Set([...(checked || []), ...(half || [])]))
  await updateRoleMenuIds(currentRoleId.value, menuIds)
  menuDialogVisible.value = false
  ElMessage({ message: '保存成功', type: 'success', duration: 2000 })
}

onMounted(() => {
  getList()
})
</script>
