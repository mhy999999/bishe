<template>
  <div class="app-container">
    <div class="filter-container">
      <el-select v-model="listQuery.businessType" placeholder="业务类型" clearable class="filter-item" style="width: 200px;">
        <el-option label="质检记录" value="QUALITY" />
        <el-option label="维修保养" value="MAINTENANCE" />
        <el-option label="销售记录" value="SALES" />
      </el-select>
      <el-select v-model="listQuery.status" placeholder="审核状态" clearable class="filter-item" style="width: 200px; margin-left: 10px;">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
      <el-button class="filter-item" type="primary" icon="Search" style="margin-left: 10px;" @click="handleFilter">
        搜索
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="申请时间" prop="applyTime" align="center" width="180">
        <template #default="{ row }">
          <span>{{ row.applyTime?.replace('T', ' ') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="业务类型" align="center">
        <template #default="{ row }">
          <el-tag>{{ typeMap[row.businessType] || row.businessType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="业务ID" prop="businessId" align="center" />
      <el-table-column label="申请人" prop="applyUser" align="center" />
      <el-table-column label="状态" align="center">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type">
            {{ statusMap[row.status]?.text }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审核意见" prop="auditOpinion" align="center" />
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" type="primary" size="small" @click="handleAudit(row)">
            审核
          </el-button>
          <span v-else>{{ row.auditTime?.replace('T', ' ') }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog title="审核处理" v-model="dialogAuditVisible" width="400px">
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
import { getAuditList, processAudit } from '@/api/battery'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'

const list = ref(null)
const total = ref(0)
const listLoading = ref(true)
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  businessType: undefined,
  status: undefined
})

const typeMap = {
  QUALITY: '质检记录',
  MAINTENANCE: '维修保养',
  SALES: '销售记录'
}

const statusMap = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' }
}

const dialogAuditVisible = ref(false)
const auditTemp = reactive({
  auditId: undefined,
  status: 1,
  auditOpinion: ''
})

const getList = () => {
  listLoading.value = true
  getAuditList(listQuery).then(response => {
    const pageData = response.data || response
    if (pageData) {
      list.value = pageData.records
      total.value = pageData.total
    }
    listLoading.value = false
  })
}

const handleFilter = () => {
  listQuery.pageNum = 1
  getList()
}

const handleAudit = (row) => {
  auditTemp.auditId = row.auditId
  auditTemp.status = 1
  auditTemp.auditOpinion = ''
  dialogAuditVisible.value = true
}

const submitAudit = () => {
  processAudit(auditTemp).then(() => {
    dialogAuditVisible.value = false
    ElMessage.success('审核完成')
    getList()
  })
}

onMounted(() => {
  getList()
})
</script>
