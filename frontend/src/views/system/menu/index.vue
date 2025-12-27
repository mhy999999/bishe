<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button class="filter-item" type="primary" icon="Edit" @click="handleCreateRoot">
        新增根菜单
      </el-button>
      <el-button class="filter-item" icon="Refresh" @click="getList" style="margin-left: 10px;">
        刷新
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row row-key="menuId"
      :tree-props="{ children: 'children' }" style="width: 100%;">
      <el-table-column label="菜单名称" prop="menuName" min-width="160" />
      <el-table-column label="类型" prop="menuType" align="center" width="90" />
      <el-table-column label="路径" prop="path" min-width="150" />
      <el-table-column label="组件" prop="component" min-width="150" />
      <el-table-column label="权限标识" prop="perms" min-width="140" />
      <el-table-column label="排序" prop="orderNum" align="center" width="80" />
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="handleCreateChild(row)">
            新增子菜单
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
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="temp.menuName" />
        </el-form-item>
        <el-form-item label="父级ID" prop="parentId">
          <el-input v-model="temp.parentId" />
        </el-form-item>
        <el-form-item label="类型" prop="menuType">
          <el-select v-model="temp.menuType" style="width: 100%;">
            <el-option label="目录" value="M" />
            <el-option label="菜单" value="C" />
            <el-option label="按钮" value="F" />
          </el-select>
        </el-form-item>
        <el-form-item label="路由路径" prop="path">
          <el-input v-model="temp.path" />
        </el-form-item>
        <el-form-item label="组件路径" prop="component">
          <el-input v-model="temp.component" />
        </el-form-item>
        <el-form-item label="权限标识" prop="perms">
          <el-input v-model="temp.perms" />
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input v-model="temp.orderNum" />
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
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/system'

const list = ref([])
const listLoading = ref(true)

const dialogFormVisible = ref(false)
const dialogStatus = ref('')

const textMap = {
  update: '编辑菜单',
  create: '新增菜单'
}

const temp = reactive({
  menuId: undefined,
  menuName: '',
  parentId: 0,
  orderNum: 0,
  path: '',
  component: '',
  perms: '',
  menuType: 'C'
})

const rules = {
  menuName: [{ required: true, message: '菜单名称必填', trigger: 'blur' }],
  menuType: [{ required: true, message: '类型必填', trigger: 'change' }]
}

const getList = () => {
  listLoading.value = true
  getMenuTree().then(res => {
    list.value = res || []
    listLoading.value = false
  }).catch(() => {
    listLoading.value = false
  })
}

const resetTemp = () => {
  temp.menuId = undefined
  temp.menuName = ''
  temp.parentId = 0
  temp.orderNum = 0
  temp.path = ''
  temp.component = ''
  temp.perms = ''
  temp.menuType = 'C'
}

const handleCreateRoot = () => {
  resetTemp()
  temp.parentId = 0
  dialogStatus.value = 'create'
  dialogFormVisible.value = true
}

const handleCreateChild = (row) => {
  resetTemp()
  temp.parentId = row.menuId
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
  await createMenu({ ...temp })
  dialogFormVisible.value = false
  ElMessage({ message: '创建成功', type: 'success', duration: 2000 })
  getList()
}

const updateData = async () => {
  await updateMenu({ ...temp })
  dialogFormVisible.value = false
  ElMessage({ message: '更新成功', type: 'success', duration: 2000 })
  getList()
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该菜单?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteMenu(row.menuId)
    ElMessage({ type: 'success', message: '删除成功!' })
    getList()
  })
}

onMounted(() => {
  getList()
})
</script>
