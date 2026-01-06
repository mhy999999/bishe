<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.vin" placeholder="车辆VIN" style="width: 200px;" class="filter-item" clearable
        maxlength="17" @update:modelValue="handleVinFilterChange" @keyup.enter="handleFilter" />
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
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="110px"
        style="width: 440px; margin-left:50px;">
        <el-form-item label="VIN码" prop="vin">
          <el-input v-model="temp.vin" placeholder="请输入 17 位 VIN（自动转大写）" clearable maxlength="17" show-word-limit
            @update:modelValue="handleVinChange" />
        </el-form-item>
        <el-form-item label="电池ID" prop="batteryId">
          <el-input v-model="temp.batteryId" :disabled="batteryIdLocked"
            :placeholder="batteryIdLocked ? '' : '请输入要绑定的电池ID'" />
        </el-form-item>
        <div v-if="batteryIdLocked" style="margin: -12px 0 12px 110px; color: #909399; font-size: 12px;">
          已从销售记录自动带入电池ID
        </div>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="temp.brand" placeholder="请输入车辆品牌" clearable />
        </el-form-item>
        <el-form-item label="型号" prop="model">
          <el-input v-model="temp.model" placeholder="请输入车型/型号" clearable />
        </el-form-item>
        <el-form-item label="车牌号" prop="plateNo">
          <el-input v-model="temp.plateNo" placeholder="可选" clearable />
        </el-form-item>
        <el-form-item label="车主ID" prop="ownerId">
          <el-input v-model="temp.ownerId" placeholder="默认当前账号ID，可修改" clearable />
        </el-form-item>
      </el-form>
      <div style="margin-left: 50px; color: #909399; font-size: 12px; margin-bottom: 20px;">
        <el-icon>
          <InfoFilled />
        </el-icon> 绑定操作将触发 bindVehicle 上链交易。
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button :disabled="submitting" @click="dialogFormVisible = false">
            取消
          </el-button>
          <el-button type="primary" :loading="submitting" @click="createData">
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { getToken } from '@/utils/auth'

const dataForm = ref()
const list = ref([])
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
const batteryIdLocked = ref(false)
const submitting = ref(false)

const normalizeVin = (raw) => {
  const s = String(raw ?? '')
  return s.replace(/\s+/g, '').replace(/-/g, '').toUpperCase().slice(0, 17)
}

const getCurrentUserIdFromToken = () => {
  const token = getToken()
  if (!token) return ''
  const parts = String(token).split('.')
  if (parts.length < 2) return ''
  try {
    let payload = parts[1]
    payload = payload.replace(/-/g, '+').replace(/_/g, '/')
    payload = payload + '='.repeat((4 - (payload.length % 4)) % 4)
    const json = JSON.parse(atob(payload))
    return json?.userId != null ? String(json.userId) : ''
  } catch {
    return ''
  }
}

const handleVinChange = (val) => {
  const next = normalizeVin(val)
  if (next !== temp.vin) temp.vin = next
}

const handleVinFilterChange = (val) => {
  const next = normalizeVin(val)
  if (next !== listQuery.vin) listQuery.vin = next
}

const validateVin = (rule, value, callback) => {
  const vin = normalizeVin(value)
  if (!vin) {
    callback(new Error('VIN必填'))
    return
  }
  if (vin.length !== 17) {
    callback(new Error('VIN长度应为17位'))
    return
  }
  if (!/^[A-HJ-NPR-Z0-9]{17}$/.test(vin)) {
    callback(new Error('VIN格式不正确'))
    return
  }
  callback()
}

const validateOwnerId = (rule, value, callback) => {
  const s = String(value ?? '').trim()
  if (!s) {
    callback(new Error('车主ID必填'))
    return
  }
  if (!/^\d+$/.test(s)) {
    callback(new Error('车主ID必须为数字'))
    return
  }
  callback()
}

const rules = {
  vin: [{ trigger: 'blur', validator: validateVin }],
  batteryId: [{ required: true, message: '电池ID必填', trigger: 'blur' }],
  brand: [{ required: true, message: '品牌必填', trigger: 'blur' }],
  model: [{ required: true, message: '型号必填', trigger: 'blur' }],
  ownerId: [{ trigger: 'blur', validator: validateOwnerId }]
}

const getList = () => {
  listLoading.value = true
  getVehicleList(listQuery).then(response => {
    const pageData = response?.data || response
    list.value = pageData?.records || []
    total.value = pageData?.total || 0
    listLoading.value = false
  }).catch(err => {
    console.error(err)
    list.value = []
    total.value = 0
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
  temp.ownerId = getCurrentUserIdFromToken()
}

const handleCreate = (prefillBatteryId) => {
  resetTemp()
  const prefill = (typeof prefillBatteryId === 'string' || typeof prefillBatteryId === 'number')
    ? String(prefillBatteryId).trim()
    : ''
  batteryIdLocked.value = !!prefill
  if (prefill) {
    temp.batteryId = prefill
  }
  dialogFormVisible.value = true
}

const createData = () => {
  if (!dataForm.value || submitting.value) return
  temp.vin = normalizeVin(temp.vin)
  dataForm.value.validate((valid) => {
    if (!valid) return

    const vin = temp.vin
    const batteryId = String(temp.batteryId || '').trim()
    if (!vin || !batteryId) return

    const brand = String(temp.brand || '').trim()
    const model = String(temp.model || '').trim()
    const plateNo = String(temp.plateNo || '').trim()
    const ownerIdStr = String(temp.ownerId || '').trim()
    const ownerId = Number(ownerIdStr)
    if (!brand || !model || !ownerIdStr || Number.isNaN(ownerId)) return

    ElMessageBox.confirm(
      `确认将电池 ${batteryId} 绑定到 VIN ${vin} 吗？该操作会触发上链交易。`,
      '确认绑定',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      submitting.value = true
      saveVehicle({
        vin,
        batteryId,
        brand,
        model,
        plateNo: plateNo || undefined,
        ownerId
      }).then(() => {
        dialogFormVisible.value = false
        ElMessage({
          message: '绑定成功',
          type: 'success',
          duration: 2000
        })
        getList()
      }).catch((err) => {
        console.error(err)
      }).finally(() => {
        submitting.value = false
      })
    }).catch(() => { })
  })
}

onMounted(() => {
  getList()

  const vin = route.query?.vin
  if (vin != null && String(vin).trim()) {
    listQuery.vin = normalizeVin(vin)
    getList()
  }

  const batteryId = route.query?.batteryId
  const action = String(route.query?.action || '').trim().toLowerCase()
  const openCreate = route.query?.openCreate
  const create = route.query?.create
  const shouldOpenCreate = action === 'create' || openCreate === '1' || create === '1'
  if (shouldOpenCreate) {
    handleCreate(batteryId)
    return
  }
  if (batteryId != null && String(batteryId).trim()) {
    temp.batteryId = String(batteryId).trim()
    batteryIdLocked.value = true
  }
})
</script>
