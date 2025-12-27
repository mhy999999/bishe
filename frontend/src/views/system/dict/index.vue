<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input
        v-model="listQuery.dictType"
        placeholder="字典类型"
        style="width: 240px;"
        class="filter-item"
        @keyup.enter="handleFilter"
      />
      <el-button class="filter-item" type="primary" icon="Search" @click="handleFilter">
        搜索
      </el-button>
      <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="Edit" @click="handleCreate">
        新增字典项
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="字典ID" prop="dictCode" align="center" width="100" />
      <el-table-column label="字典类型" prop="dictType" align="center" width="160" />
      <el-table-column label="字典标签" prop="dictLabel" align="center" />
      <el-table-column label="字典值" prop="dictValue" align="center" />
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
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

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="listQuery.pageNum"
      v-model:limit="listQuery.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="textMap[dialogStatus]" v-model="dialogFormVisible" width="520px">
      <el-form
        ref="dataForm"
        :rules="rules"
        :model="temp"
        label-position="left"
        label-width="90px"
        style="width: 420px; margin-left: 40px;"
      >
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="temp.dictType" />
        </el-form-item>
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="temp.dictLabel" />
        </el-form-item>
        <el-form-item label="字典值" prop="dictValue">
          <el-input v-model="temp.dictValue" />
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
import { getDictList, createDict, updateDict, deleteDict } from '@/api/system'

const list = ref([])
const total = ref(0)
const listLoading = ref(true)

const dialogFormVisible = ref(false)
const dialogStatus = ref('')
const textMap = {
  update: '编辑字典项',
  create: '新增字典项'
}

const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  dictType: undefined
})

const temp = reactive({
  dictCode: undefined,
  dictType: '',
  dictLabel: '',
  dictValue: ''
})

const rules = {
  dictType: [{ required: true, message: '字典类型必填', trigger: 'blur' }],
  dictLabel: [{ required: true, message: '字典标签必填', trigger: 'blur' }],
  dictValue: [{ required: true, message: '字典值必填', trigger: 'blur' }]
}

const getList = () => {
  listLoading.value = true
  getDictList(listQuery).then(pageData => {
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
  temp.dictCode = undefined
  temp.dictType = ''
  temp.dictLabel = ''
  temp.dictValue = ''
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
  await createDict({ ...temp })
  dialogFormVisible.value = false
  ElMessage({ message: '创建成功', type: 'success', duration: 2000 })
  getList()
}

const updateData = async () => {
  await updateDict({ ...temp })
  dialogFormVisible.value = false
  ElMessage({ message: '更新成功', type: 'success', duration: 2000 })
  getList()
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该字典项?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteDict(row.dictCode)
    ElMessage({ type: 'success', message: '删除成功!' })
    getList()
  })
}

onMounted(() => {
  getList()
})
</script>

