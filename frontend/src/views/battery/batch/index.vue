<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button class="filter-item" type="primary" icon="Edit" @click="handleCreate">
        新增批次
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="批次ID" prop="batchId" align="center" width="80" />
      <el-table-column label="批次号" prop="batchNo" align="center" />
      <el-table-column label="生产商ID" prop="manufacturerId" align="center" />
      <el-table-column label="生产日期" prop="produceDate" align="center" />
      <el-table-column label="最大生产数量" prop="quantity" align="center" />
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleViewBatteries(row)">
            查看电池
          </el-button>
          <el-button type="warning" size="small" @click="handleEndProduction(row)">
            生产结束
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog title="批次电池列表" v-model="batteryDialogVisible" width="70%">
      <el-table :data="batteryList" border fit highlight-current-row style="width: 100%;">
        <el-table-column label="电池ID" prop="batteryId" align="center" />
        <el-table-column label="类型" prop="typeCode" align="center" />
        <el-table-column label="容量" prop="capacity" align="center" />
        <el-table-column label="电压" prop="voltage" align="center" />
        <el-table-column label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="batteryDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="新增批次" v-model="dialogFormVisible">
      <el-form ref="dataForm" :model="temp" label-width="100px">
        <el-form-item label="批次号">
          <el-input v-model="temp.batchNo" />
        </el-form-item>
        <el-form-item label="生产商">
          <el-select v-model="temp.manufacturerId" placeholder="请选择生产商" filterable style="width: 100%;">
            <el-option v-for="item in manufacturerOptions" :key="item.deptId" :label="item.deptName"
              :value="item.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="生产日期">
          <el-date-picker v-model="temp.produceDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="最大生产数量">
          <el-input v-model="temp.quantity" type="number" />
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
import { getBatchList, saveBatch, endBatch, getBatteryList } from '@/api/battery'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeptList } from '@/api/system'
import { BATTERY_STATUS_MAP } from '@/constants/batteryStatus'

const list = ref([])
const total = ref(0)
const listLoading = ref(true)
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10
})

const dialogFormVisible = ref(false)
const batteryDialogVisible = ref(false)
const batteryList = ref([])

const statusMap = BATTERY_STATUS_MAP

const temp = reactive({
  batchNo: '',
  manufacturerId: '',
  produceDate: '',
  quantity: ''
})

const manufacturerOptions = ref([])

const loadManufacturerOptions = () => {
  getDeptList().then((res) => {
    const list = res?.data || res || []
    manufacturerOptions.value = Array.isArray(list) ? list : []
  }).catch(() => {
    manufacturerOptions.value = []
  })
}

const getList = () => {
  listLoading.value = true
  getBatchList(listQuery).then(response => {
    const pageData = response.data || response
    if (pageData) {
      list.value = pageData.records || []
      total.value = pageData.total || 0
    }
    listLoading.value = false
  }).catch(() => {
    list.value = []
    total.value = 0
    listLoading.value = false
  })
}

const handleCreate = () => {
  temp.batchNo = ''
  temp.manufacturerId = ''
  temp.produceDate = new Date().toISOString().split('T')[0]
  temp.quantity = ''
  dialogFormVisible.value = true
}

const createData = () => {
  saveBatch(temp).then(() => {
    dialogFormVisible.value = false
    ElMessage.success('创建成功')
    getList()
  })
}

const handleViewBatteries = (row) => {
  batteryList.value = []
  batteryDialogVisible.value = true
  getBatteryList({
    pageNum: 1,
    pageSize: 1000,
    batchId: row.batchId
  }).then(res => {
    const data = res.data || res
    batteryList.value = data.records || []
  })
}

const handleEndProduction = (row) => {
  ElMessageBox.confirm('确认结束该批次的生产吗？结束后将自动生成质检记录。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    endBatch(row.batchId).then(() => {
      ElMessage.success('操作成功')
    })
  })
}

onMounted(() => {
  getList()
  loadManufacturerOptions()
})
</script>
