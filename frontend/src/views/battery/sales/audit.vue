<template>
  <div class="app-container">
    <div class="filter-container">
      <el-tag type="warning" class="filter-item" style="margin-right: 10px;">待审核列表</el-tag>
      <el-button class="filter-item" type="primary" icon="Refresh" @click="handleFilter">
        刷新
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="销售ID" prop="salesId" align="center" width="80" />
      <el-table-column label="电池ID" prop="batteryId" align="center" />
      <el-table-column label="买家姓名" prop="buyerName" align="center" />
      <el-table-column label="售价" prop="salesPrice" align="center" />
      <el-table-column label="销售员" prop="salesPerson" align="center" />
      <el-table-column label="销售日期" align="center">
        <template #default="{ row }">
          <span>{{ row.salesDate?.replace('T', ' ') }}</span>
        </template>
      </el-table-column>
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

    <el-dialog title="销售审核" v-model="dialogAuditVisible" width="400px">
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
import { getSalesList, auditSales } from '@/api/battery'
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
  salesId: undefined,
  status: 1,
  auditOpinion: ''
})

const handleAudit = (row) => {
  auditTemp.salesId = row.salesId
  auditTemp.status = 1
  auditTemp.auditOpinion = ''
  dialogAuditVisible.value = true
}

const submitAudit = () => {
  auditSales(auditTemp).then(() => {
    dialogAuditVisible.value = false
    ElMessage.success('审核完成')
    getList()
  }).catch((err) => {
    console.error(err)
  })
}

const handleFilter = () => {
  listQuery.pageNum = 1
  getList()
}

const getList = () => {
  listLoading.value = true
  getSalesList(listQuery).then(response => {
    const pageData = response.data || response
    if (pageData) {
      list.value = pageData.records
      total.value = pageData.total
    }
    listLoading.value = false
  }).catch((err) => {
    console.error(err)
    list.value = []
    total.value = 0
    listLoading.value = false
  })
}

onMounted(() => {
  getList()
})
</script>
