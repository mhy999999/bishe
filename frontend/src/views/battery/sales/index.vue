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
        新增销售记录
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

    <el-dialog title="新增销售记录" v-model="dialogFormVisible">
      <el-form ref="dataForm" :model="temp" :rules="rules" label-width="100px">
        <el-form-item label="电池ID" prop="batteryId">
          <el-select v-model="temp.batteryId" filterable placeholder="请选择电池ID" :loading="batteryLoading" style="width: 100%;">
            <el-option v-for="item in batteryOptions" :key="item.batteryId" :label="item.batteryId" :value="item.batteryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="买家姓名" prop="buyerName">
          <el-input v-model="temp.buyerName" />
        </el-form-item>
        <el-form-item label="售价" prop="salesPrice">
          <el-input v-model="temp.salesPrice" type="number" />
        </el-form-item>
        <el-form-item label="销售员">
          <el-input v-model="temp.salesPerson" disabled placeholder="自动使用当前账号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">取消</el-button>
          <el-button type="primary" @click="createData">确认</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="审核销售记录" v-model="dialogAuditVisible">
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
import { getSalesList, saveSales, auditSales, getBatteryList } from '@/api/battery'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const dataForm = ref()
const userStore = useUserStore()

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
  buyerName: '',
  salesPrice: '',
  salesPerson: ''
})

const batteryOptions = ref([])
const batteryLoading = ref(false)

const rules = {
  batteryId: [{ required: true, message: '电池ID必填', trigger: 'change' }],
  buyerName: [{ required: true, message: '买家姓名必填', trigger: 'blur' }],
  salesPrice: [{
    required: true,
    trigger: 'blur',
    validator: (rule, value, callback) => {
      const num = Number(value)
      if (!value && value !== 0) {
        callback(new Error('售价必填'))
        return
      }
      if (Number.isNaN(num) || num <= 0) {
        callback(new Error('售价必须大于0'))
        return
      }
      callback()
    }
  }]
}

const dialogAuditVisible = ref(false)
const auditTemp = reactive({
  salesId: undefined,
  status: 1,
  auditOpinion: ''
})

const loadSellableBatteries = () => {
  batteryLoading.value = true
  return getBatteryList({
    pageNum: 1,
    pageSize: 1000,
    status: 1
  }).then(response => {
    const pageData = response?.data || response
    batteryOptions.value = pageData?.records || []
    batteryLoading.value = false
  }).catch((err) => {
    console.error(err)
    batteryOptions.value = []
    batteryLoading.value = false
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
  }).catch(() => {
    list.value = []
    total.value = 0
    listLoading.value = false
  })
}

const handleCreate = async () => {
  temp.batteryId = ''
  temp.buyerName = ''
  temp.salesPrice = ''
  temp.salesPerson = userStore.name || ''
  await loadSellableBatteries()
  dialogFormVisible.value = true
}

const createData = () => {
  if (!dataForm.value) {
    return
  }
  if (!temp.salesPerson) {
    temp.salesPerson = userStore.name || ''
  }
  dataForm.value.validate((valid) => {
    if (!valid) {
      return
    }
    saveSales(temp).then(() => {
      dialogFormVisible.value = false
      ElMessage.success('创建成功，已提交审核')
      getList()
    }).catch((err) => {
      console.error(err)
    })
  })
}

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

onMounted(() => {
  getList()
})
</script>
