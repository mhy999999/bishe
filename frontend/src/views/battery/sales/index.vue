<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.batteryId" placeholder="电池ID" clearable class="filter-item"
        style="width: 200px; margin-right: 10px;" @keyup.enter="handleFilter" />
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
      <el-table-column label="上链哈希" align="center" width="200">
        <template #default="{ row }">
          <template v-if="row.txHash">
            <el-tooltip :content="row.txHash" placement="top" effect="light">
              <el-link type="primary" underline="never" @click.stop="openTxHash(row.txHash)">
                {{ row.txHash.substring(0, 10) + '...' }}
              </el-link>
            </el-tooltip>
          </template>
          <span v-else>未上链</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type || 'info'">
            {{ statusMap[row.status]?.text || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="驳回原因" prop="auditOpinion" align="center" />
      <el-table-column label="材料说明" prop="materialDesc" align="center" />
      <el-table-column label="材料文件" align="center" width="180">
        <template #default="{ row }">
          <div v-if="parseMaterialUrls(row.materialUrl).length">
            <el-link v-for="(url, idx) in parseMaterialUrls(row.materialUrl)" :key="url + idx"
              @click.stop="openMaterialPreview(url)" type="primary" underline="never" style="margin-right: 8px;">
              文件{{ idx + 1 }}
            </el-link>
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200">
        <template #default="{ row }">
          <template v-if="row.status === 1">
            <el-button type="primary" size="small" @click="handleBinding(row)">车电绑定</el-button>
          </template>
          <template v-else-if="row.status === 2">
            <el-button type="primary" size="small" @click="handleResubmit(row)">重新提交</el-button>
            <el-button type="danger" size="small" @click="handleCancel(row)">取消订单</el-button>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog :title="dialogMode === 'create' ? '新增销售记录' : '重新提交销售记录'" v-model="dialogFormVisible">
      <el-form ref="dataForm" :model="temp" :rules="rules" label-width="100px">
        <el-form-item label="电池ID" prop="batteryId">
          <el-select v-model="temp.batteryId" filterable placeholder="请选择电池ID" :loading="batteryLoading"
            style="width: 100%;">
            <el-option v-for="item in batteryOptions" :key="item.batteryId" :label="item.batteryId"
              :value="item.batteryId" />
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
        <el-form-item label="材料说明">
          <el-input v-model="temp.materialDesc" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="材料文件">
          <el-upload v-model:file-list="materialFileList" :http-request="handleMaterialUpload"
            :on-remove="handleMaterialRemove" multiple :limit="3" :on-exceed="handleMaterialExceed">
            <el-button type="primary">上传文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">取消</el-button>
          <el-button type="primary" @click="submitData">确认</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="车电绑定" v-model="bindDialogVisible" width="560px">
      <el-form ref="bindForm" :model="bindTemp" :rules="bindRules" label-width="110px">
        <el-form-item label="VIN码" prop="vin">
          <el-input v-model="bindTemp.vin" placeholder="请输入 17 位 VIN（自动转大写）" clearable maxlength="17" show-word-limit
            @update:modelValue="handleBindVinChange" />
        </el-form-item>
        <el-form-item label="电池ID" prop="batteryId">
          <el-input v-model="bindTemp.batteryId" disabled />
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="bindTemp.brand" placeholder="请输入车辆品牌" clearable />
        </el-form-item>
        <el-form-item label="型号" prop="model">
          <el-input v-model="bindTemp.model" placeholder="请输入车型/型号" clearable />
        </el-form-item>
        <el-form-item label="车牌号">
          <el-input v-model="bindTemp.plateNo" placeholder="可选" clearable />
        </el-form-item>
        <el-form-item label="车主ID" prop="ownerId">
          <el-input v-model="bindTemp.ownerId" placeholder="默认当前账号ID，可修改" clearable />
        </el-form-item>
      </el-form>
      <div style="margin-left: 50px; color: #909399; font-size: 12px; margin-bottom: 12px;">
        绑定操作将触发 bindVehicle 上链交易。
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button :disabled="bindSubmitting" @click="bindDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="bindSubmitting" @click="submitBind">确认绑定</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getSalesList, saveSales, updateSales, getBatteryList, uploadSalesMaterial, cancelSales } from '@/api/battery'
import { saveVehicle } from '@/api/trace'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useRouter, useRoute } from 'vue-router'
import { getToken } from '@/utils/auth'

const dataForm = ref()
const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const list = ref([])
const total = ref(0)
const listLoading = ref(true)
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  batteryId: undefined,
  status: undefined
})

const statusMap = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' },
  3: { text: '已取消', type: 'info' }
}

const dialogFormVisible = ref(false)
const dialogMode = ref('create')
const temp = reactive({
  salesId: undefined,
  batteryId: '',
  buyerName: '',
  salesPrice: '',
  salesPerson: '',
  materialDesc: '',
  materialUrl: ''
})

const batteryOptions = ref([])
const batteryLoading = ref(false)

const materialFileList = ref([])
const materialUrls = ref([])

const bindDialogVisible = ref(false)
const bindForm = ref()
const bindSubmitting = ref(false)
const bindTemp = reactive({
  vin: '',
  batteryId: '',
  brand: '',
  model: '',
  plateNo: '',
  ownerId: ''
})

const rules = {
  batteryId: [{ required: true, message: '电池ID必填', trigger: 'change' }],
  buyerName: [{ required: true, message: '买家姓名必填', trigger: 'blur' }],
  materialDesc: [{ required: true, message: '材料说明必填', trigger: 'blur' }],
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

const parseMaterialUrls = (raw) => {
  if (!raw) return []
  if (Array.isArray(raw)) return raw.filter(Boolean)
  if (typeof raw !== 'string') return []
  const text = raw.trim()
  if (!text) return []
  try {
    const parsed = JSON.parse(text)
    if (Array.isArray(parsed)) return parsed.filter(Boolean)
  } catch (e) {
  }
  if (text.includes(',')) {
    return text.split(',').map(s => s.trim()).filter(Boolean)
  }
  return [text]
}

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

const bindRules = {
  vin: [{ trigger: 'blur', validator: validateVin }],
  batteryId: [{ required: true, message: '电池ID必填', trigger: 'blur' }],
  brand: [{ required: true, message: '品牌必填', trigger: 'blur' }],
  model: [{ required: true, message: '型号必填', trigger: 'blur' }],
  ownerId: [{ trigger: 'blur', validator: validateOwnerId }]
}

const handleBindVinChange = (val) => {
  const next = normalizeVin(val)
  if (next !== bindTemp.vin) bindTemp.vin = next
}

const resetBindTemp = (batteryId) => {
  bindTemp.vin = ''
  bindTemp.batteryId = String(batteryId || '').trim()
  bindTemp.brand = ''
  bindTemp.model = ''
  bindTemp.plateNo = ''
  bindTemp.ownerId = getCurrentUserIdFromToken()
}

const openMaterialPreview = (rawUrl) => {
  router.push({
    name: 'SalesMaterialPreview',
    query: { url: String(rawUrl || ''), from: router.currentRoute.value.fullPath }
  })
}

const openTxHash = (txHash) => {
  router.push({ name: 'Trace', query: { txHash: String(txHash || '') } })
}

const syncMaterialFileList = () => {
  materialFileList.value = materialUrls.value.map((url, idx) => ({
    name: `文件${idx + 1}`,
    url,
    status: 'success'
  }))
}

const handleMaterialExceed = () => {
  ElMessage.warning('材料文件最多上传 3 个')
}

const handleMaterialUpload = (options) => {
  if (materialUrls.value.length >= 3) {
    ElMessage.warning('材料文件最多上传 3 个')
    options.onError && options.onError(new Error('材料文件数量超限'))
    return
  }
  const formData = new FormData()
  formData.append('file', options.file)
  uploadSalesMaterial(formData).then((url) => {
    if (materialUrls.value.length >= 3) {
      ElMessage.warning('材料文件最多上传 3 个')
      options.onError && options.onError(new Error('材料文件数量超限'))
      return
    }
    materialUrls.value.push(url)
    syncMaterialFileList()
    options.onSuccess && options.onSuccess({ url }, options.file)
  }).catch((err) => {
    options.onError && options.onError(err)
  })
}

const handleMaterialRemove = (uploadFile, uploadFiles) => {
  materialUrls.value = (uploadFiles || []).map(f => f.url).filter(Boolean)
  syncMaterialFileList()
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

const handleCreate = async (prefillBatteryId) => {
  const prefill = (typeof prefillBatteryId === 'string' || typeof prefillBatteryId === 'number')
    ? String(prefillBatteryId).trim()
    : ''
  dialogMode.value = 'create'
  temp.salesId = undefined
  temp.batteryId = ''
  temp.buyerName = ''
  temp.salesPrice = ''
  temp.salesPerson = userStore.name || ''
  temp.materialDesc = ''
  temp.materialUrl = ''
  materialUrls.value = []
  syncMaterialFileList()
  await loadSellableBatteries()
  if (prefill) temp.batteryId = prefill
  dialogFormVisible.value = true
}

const handleResubmit = async (row) => {
  dialogMode.value = 'resubmit'
  temp.salesId = row.salesId
  temp.batteryId = row.batteryId || ''
  temp.buyerName = row.buyerName || ''
  temp.salesPrice = row.salesPrice != null ? String(row.salesPrice) : ''
  temp.salesPerson = userStore.name || row.salesPerson || ''
  temp.materialDesc = row.materialDesc || ''
  materialUrls.value = parseMaterialUrls(row.materialUrl)
  syncMaterialFileList()
  await loadSellableBatteries()
  dialogFormVisible.value = true
}

const handleCancel = (row) => {
  const id = row?.salesId
  if (!id) return
  ElMessageBox.confirm(`确认取消销售订单 ${id} 吗？`, '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    cancelSales(id).then(() => {
      ElMessage.success('已取消')
      getList()
    }).catch((err) => {
      console.error(err)
    })
  }).catch(() => { })
}

const handleBinding = (row) => {
  const batteryId = row?.batteryId
  if (!batteryId) return
  resetBindTemp(batteryId)
  bindDialogVisible.value = true
}

const submitBind = () => {
  if (!bindForm.value || bindSubmitting.value) return
  bindTemp.vin = normalizeVin(bindTemp.vin)
  bindForm.value.validate((valid) => {
    if (!valid) return
    const vin = bindTemp.vin
    const batteryId = String(bindTemp.batteryId || '').trim()
    if (!vin || !batteryId) return

    const brand = String(bindTemp.brand || '').trim()
    const model = String(bindTemp.model || '').trim()
    const plateNo = String(bindTemp.plateNo || '').trim()
    const ownerIdStr = String(bindTemp.ownerId || '').trim()
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
      bindSubmitting.value = true
      saveVehicle({
        vin,
        batteryId,
        brand,
        model,
        plateNo: plateNo || undefined,
        ownerId
      }).then(() => {
        bindDialogVisible.value = false
        ElMessage.success('绑定成功')
        router.push({ name: 'VehicleBinding', query: { vin } })
      }).catch((err) => {
        console.error(err)
      }).finally(() => {
        bindSubmitting.value = false
      })
    }).catch(() => { })
  })
}

const initFromRoute = async () => {
  const batteryId = route.query?.batteryId
  if (batteryId != null && String(batteryId).trim()) {
    listQuery.batteryId = String(batteryId).trim()
  }

  const action = String(route.query?.action || '').trim().toLowerCase()
  const openCreate = route.query?.openCreate
  const create = route.query?.create
  const shouldOpenCreate = action === 'create' || openCreate === '1' || create === '1'
  if (shouldOpenCreate) {
    await handleCreate(batteryId)
  }
}

const submitData = () => {
  if (!dataForm.value) {
    return
  }
  if (!temp.salesPerson) {
    temp.salesPerson = userStore.name || ''
  }
  temp.materialUrl = JSON.stringify(materialUrls.value)
  dataForm.value.validate((valid) => {
    if (!valid) {
      return
    }
    const req = dialogMode.value === 'create' ? saveSales(temp) : updateSales(temp)
    req.then(() => {
      dialogFormVisible.value = false
      ElMessage.success(dialogMode.value === 'create' ? '创建成功，已提交审核' : '重新提交成功，已提交审核')
      getList()
    }).catch((err) => {
      console.error(err)
    })
  })
}

onMounted(async () => {
  await initFromRoute()
  getList()
})
</script>
