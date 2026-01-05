<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.vin" placeholder="车辆VIN" style="width: 200px;" class="filter-item"
        @keyup.enter="handleFilter" />
      <el-button class="filter-item" type="primary" icon="Search" @click="handleFilter">
        搜索
      </el-button>
      <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="Link" @click="handleCreate">
        车辆绑定
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="车辆ID" prop="vehicleId" align="center" width="80" />
      <el-table-column label="VIN码" prop="vin" align="center" width="180" />
      <el-table-column label="绑定电池ID" prop="batteryId" align="center" width="180" />
      <el-table-column label="品牌" prop="brand" align="center" />
      <el-table-column label="型号" prop="model" align="center" />
      <el-table-column label="车牌号" prop="plateNo" align="center" />
      <el-table-column label="绑定时间" prop="bindTime" align="center">
        <template #default="{ row }">
          <span>{{ row.bindTime?.replace('T', ' ') }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog title="车辆电池绑定" v-model="dialogFormVisible">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="100px"
        style="width: 400px; margin-left:50px;">
        <el-form-item label="VIN码" prop="vin">
          <el-input v-model="temp.vin" />
        </el-form-item>
        <el-form-item label="电池ID" prop="batteryId">
          <el-input v-model="temp.batteryId" placeholder="请输入要绑定的电池ID" />
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="temp.brand" />
        </el-form-item>
        <el-form-item label="型号" prop="model">
          <el-input v-model="temp.model" />
        </el-form-item>
        <el-form-item label="车牌号" prop="plateNo">
          <el-input v-model="temp.plateNo" />
        </el-form-item>
        <el-form-item label="车主ID" prop="ownerId">
          <el-input v-model="temp.ownerId" />
        </el-form-item>
      </el-form>
      <div style="margin-left: 50px; color: #909399; font-size: 12px; margin-bottom: 20px;">
        <el-icon>
          <InfoFilled />
        </el-icon> 绑定操作将触发 bindVehicle 上链交易。
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">
            取消
          </el-button>
          <el-button type="primary" @click="createData">
            确认绑定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getVehicleList, saveVehicle } from '@/api/trace'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'

const list = ref(null)
const total = ref(0)
const listLoading = ref(true)
const route = useRoute()
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  vin: undefined
})

const temp = reactive({
  vin: '',
  batteryId: '',
  brand: '',
  model: '',
  plateNo: '',
  ownerId: ''
})

const dialogFormVisible = ref(false)

const rules = {
  vin: [{ required: true, message: 'VIN必填', trigger: 'blur' }],
  batteryId: [{ required: true, message: '电池ID必填', trigger: 'blur' }]
}

const getList = () => {
  listLoading.value = true
  getVehicleList(listQuery).then(response => {
    if (response.data) {
      list.value = response.data.records
      total.value = response.data.total
    } else {
      list.value = []
      total.value = 0
    }
    listLoading.value = false
  }).catch(err => {
    console.error(err)
    listLoading.value = false
  })
}

const handleFilter = () => {
  listQuery.pageNum = 1
  getList()
}

const resetTemp = () => {
  temp.vin = ''
  temp.batteryId = ''
  temp.brand = ''
  temp.model = ''
  temp.plateNo = ''
  temp.ownerId = ''
}

const handleCreate = () => {
  resetTemp()
  dialogFormVisible.value = true
}

const createData = () => {
  saveVehicle(temp).then(() => {
    dialogFormVisible.value = false
    ElMessage({
      message: '绑定成功',
      type: 'success',
      duration: 2000
    })
    getList()
  })
}

onMounted(() => {
  getList()

  const batteryId = route.query?.batteryId
  if (batteryId != null && String(batteryId).trim()) {
    temp.batteryId = String(batteryId).trim()
  }
  const action = String(route.query?.action || '').trim().toLowerCase()
  const openCreate = route.query?.openCreate
  const create = route.query?.create
  const shouldOpenCreate = action === 'create' || openCreate === '1' || create === '1'
  if (shouldOpenCreate) {
    dialogFormVisible.value = true
  }
})
</script>
