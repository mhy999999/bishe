<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button class="filter-item" type="primary" icon="Edit" @click="handleCreateRoot">
        新增根部门
      </el-button>
      <el-button class="filter-item" icon="Refresh" @click="getList" style="margin-left: 10px;">
        刷新
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row row-key="deptId"
      :tree-props="{ children: 'children' }" style="width: 100%;">
      <el-table-column label="部门名称" prop="deptName" min-width="160" />
      <el-table-column label="负责人" prop="leader" align="center" width="120" />
      <el-table-column label="手机" prop="phone" align="center" width="140" />
      <el-table-column label="排序" prop="orderNum" align="center" width="80" />
      <el-table-column label="状态" align="center" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' || row.status === 0 ? 'success' : 'danger'">
            {{ row.status === '0' || row.status === 0 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="handleCreateChild(row)">
            新增子部门
          </el-button>
          <el-button size="small" @click="handleUpdate(row)">
            编辑
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="textMap[dialogStatus]" v-model="dialogFormVisible" width="560px">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="90px"
        style="width: 460px; margin-left: 40px;">
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="temp.deptName" />
        </el-form-item>
        <el-form-item label="父级ID" prop="parentId">
          <el-input v-model="temp.parentId" />
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input v-model="temp.orderNum" />
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="temp.leader" />
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="temp.phone" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="temp.status" style="width: 100%;">
            <el-option label="正常" value="0" />
            <el-option label="禁用" value="1" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeptTree, createDept, updateDept, deleteDept } from '@/api/system'

const list = ref([])
const listLoading = ref(true)

const dialogFormVisible = ref(false)
const dialogStatus = ref('')

const textMap = {
  update: '编辑部门',
  create: '新增部门'
}

const temp = reactive({
  deptId: undefined,
  parentId: 0,
  deptName: '',
  orderNum: 0,
  leader: '',
  phone: '',
  status: '0'
})

const rules = {
  deptName: [{ required: true, message: '部门名称必填', trigger: 'blur' }]
}

const getList = () => {
  listLoading.value = true
  getDeptTree().then(res => {
    list.value = res || []
    listLoading.value = false
  }).catch(() => {
    listLoading.value = false
  })
}

const resetTemp = () => {
  temp.deptId = undefined
  temp.parentId = 0
  temp.deptName = ''
  temp.orderNum = 0
  temp.leader = ''
  temp.phone = ''
  temp.status = '0'
}

const handleCreateRoot = () => {
  resetTemp()
  temp.parentId = 0
  dialogStatus.value = 'create'
  dialogFormVisible.value = true
}

const handleCreateChild = (row) => {
  resetTemp()
  temp.parentId = row.deptId
  dialogStatus.value = 'create'
  dialogFormVisible.value = true
}

const handleUpdate = (row) => {
  resetTemp()
  Object.assign(temp, row)
  if (temp.status === 0) temp.status = '0'
  if (temp.status === 1) temp.status = '1'
  dialogStatus.value = 'update'
  dialogFormVisible.value = true
}

const createData = async () => {
  await createDept({ ...temp })
  dialogFormVisible.value = false
  ElMessage({ message: '创建成功', type: 'success', duration: 2000 })
  getList()
}

const updateData = async () => {
  await updateDept({ ...temp })
  dialogFormVisible.value = false
  ElMessage({ message: '更新成功', type: 'success', duration: 2000 })
  getList()
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该部门?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteDept(row.deptId)
    ElMessage({ type: 'success', message: '删除成功!' })
    getList()
  })
}

onMounted(() => {
  getList()
})
</script>
