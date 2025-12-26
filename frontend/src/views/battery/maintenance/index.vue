<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button class="filter-item" type="primary" icon="Edit" @click="handleCreate">
        新增维修记录
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="记录ID" prop="recordId" align="center" width="80" />
      <el-table-column label="电池ID" prop="batteryId" align="center" />
      <el-table-column label="故障类型" prop="faultType" align="center" />
      <el-table-column label="维修内容" prop="description" align="center" />
      <el-table-column label="解决方案" prop="solution" align="center" />
      <el-table-column label="维修人" prop="maintainer" align="center" />
      <el-table-column label="状态" align="center">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type || 'info'">
            {{ statusMap[row.status]?.text || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog title="新增维修记录" v-model="dialogFormVisible">
      <el-form ref="dataForm" :model="temp" label-width="100px">
        <el-form-item label="电池ID">
          <el-input v-model="temp.batteryId" />
        </el-form-item>
        <el-form-item label="故障类型">
          <el-input v-model="temp.faultType" />
        </el-form-item>
        <el-form-item label="维修内容">
          <el-input v-model="temp.description" type="textarea" />
        </el-form-item>
        <el-form-item label="解决方案">
          <el-input v-model="temp.solution" type="textarea" />
        </el-form-item>
        <el-form-item label="维修人">
          <el-input v-model="temp.maintainer" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">取消</el-button>
          <el-button type="primary" @click="createData">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMaintenanceList, saveMaintenance } from '@/api/battery'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'

const list = ref([])
const total = ref(0)
const listLoading = ref(true)
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10
})

const statusMap = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' }
}

const dialogFormVisible = ref(false)
const temp = reactive({
  batteryId: '',
  faultType: '',
  description: '',
  solution: '',
  maintainer: ''
})

const getList = () => {
  listLoading.value = true
  getMaintenanceList(listQuery).then(response => {
    const pageData = response.data || response
    if (pageData) {
      list.value = pageData.records || []
      total.value = pageData.total || 0
    } else {
      list.value = []
      total.value = 0
    }
    listLoading.value = false
  }).catch(() => {
    list.value = []
    total.value = 0
    listLoading.value = false
  })
}

const handleCreate = () => {
  temp.batteryId = ''
  temp.faultType = ''
  temp.description = ''
  temp.solution = ''
  temp.maintainer = ''
  dialogFormVisible.value = true
}

const createData = () => {
  saveMaintenance(temp).then(() => {
    dialogFormVisible.value = false
    ElMessage.success('创建成功，已提交审核')
    getList()
  })
}

onMounted(() => {
  getList()
})
</script>
