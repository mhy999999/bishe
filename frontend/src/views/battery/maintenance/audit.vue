<template>
  <div class="app-container">
    <div class="filter-container">
      <el-tag type="warning" class="filter-item" style="margin-right: 10px;">待审核列表</el-tag>
      <el-button class="filter-item" type="primary" icon="Refresh" @click="handleFilter">
        刷新
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="记录ID" prop="recordId" align="center" width="80" />
      <el-table-column label="电池ID" prop="batteryId" align="center" />
      <el-table-column label="故障类型" prop="faultType" align="center" />
      <el-table-column label="维修内容" prop="description" align="center" />
      <el-table-column label="解决方案" prop="solution" align="center" />
      <el-table-column label="维修人" prop="maintainer" align="center" />
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleAudit(row)">
            审核
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog title="维修审核" v-model="dialogAuditVisible" width="400px">
      <el-form :model="auditTemp" label-width="80px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditTemp.status">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditTemp.auditOpinion" type="textarea" placeholder="请输入审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogAuditVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAudit">提交</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMaintenanceList, auditMaintenance } from '@/api/battery'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'

const list = ref([])
const total = ref(0)
const listLoading = ref(true)
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  status: 0 // 只显示待审核
})

const dialogAuditVisible = ref(false)
const auditTemp = reactive({
  recordId: undefined,
  status: 1,
  auditOpinion: ''
})

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

onMounted(() => {
  getList()
})
</script>
