<template>
  <div class="app-container">
    <div class="filter-container">
      <el-select v-model="listQuery.status" placeholder="审核状态" clearable class="filter-item"
        style="width: 150px; margin-right: 10px;">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
      <el-button class="filter-item" type="primary" icon="Search" @click="handleFilter">
        搜索
      </el-button>
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
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" type="primary" size="small" @click="handleAudit(row)">
            审核
          </el-button>
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

    <el-dialog title="审核维修记录" v-model="dialogAuditVisible">
      <el-form :model="auditTemp" label-width="100px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditTemp.status">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditTemp.auditOpinion" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogAuditVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAudit">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMaintenanceList, saveMaintenance, auditMaintenance } from '@/api/battery'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'

const list = ref([])
const total = ref(0)
const listLoading = ref(true)
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  status: undefined
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

const dialogAuditVisible = ref(false)
const auditTemp = reactive({
  recordId: undefined,
  status: 1,
  auditOpinion: ''
})

const handleFilter = () => {
  listQuery.pageNum = 1
  getList()
}

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

const handleAudit = (row) => {
  auditTemp.recordId = row.recordId
  auditTemp.status = 1
  auditTemp.auditOpinion = ''
  dialogAuditVisible.value = true
}

const submitAudit = () => {
  auditMaintenance(auditTemp).then(() => {
    dialogAuditVisible.value = false
    ElMessage.success('审核完成')
    getList()
  })
}

onMounted(() => {
  getList()
})
</script>
