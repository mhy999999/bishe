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
      <el-table-column label="驳回原因" prop="auditOpinion" align="center" />
      <el-table-column label="材料说明" prop="materialDesc" align="center" />
      <el-table-column label="材料文件" align="center" width="180">
        <template #default="{ row }">
          <div v-if="parseMaterialUrls(row.materialUrl).length">
            <el-link v-for="(url, idx) in parseMaterialUrls(row.materialUrl)" :key="url + idx" :href="normalizeMaterialLink(url)"
              target="_blank" type="primary" :underline="false" style="margin-right: 8px;">
              文件{{ idx + 1 }}
            </el-link>
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120">
        <template #default="{ row }">
          <el-button v-if="row.status === 2" type="primary" size="small" @click="handleResubmit(row)">
            重新提交
          </el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog :title="dialogMode === 'create' ? '新增销售记录' : '重新提交销售记录'" v-model="dialogFormVisible">
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
        <el-form-item label="材料说明">
          <el-input v-model="temp.materialDesc" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="材料文件">
          <el-upload v-model:file-list="materialFileList" :http-request="handleMaterialUpload" :on-remove="handleMaterialRemove"
            :limit="5">
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

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getSalesList, saveSales, updateSales, getBatteryList, uploadSalesMaterial } from '@/api/battery'
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

const normalizeMaterialLink = (rawUrl) => {
  if (!rawUrl) return ''
  const url = String(rawUrl).trim()
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url

  const apiBase = (import.meta.env.VITE_APP_BASE_API || '/api').replace(/\/$/, '')
  if (url.startsWith('/api/')) return url
  if (url.startsWith('/files/')) return `${apiBase}${url}`
  if (url.startsWith('files/')) return `${apiBase}/${url}`
  if (url.startsWith('/')) return url
  return `${apiBase}/${url}`
}

const syncMaterialFileList = () => {
  materialFileList.value = materialUrls.value.map((url, idx) => ({
    name: `文件${idx + 1}`,
    url,
    status: 'success'
  }))
}

const handleMaterialUpload = (options) => {
  const formData = new FormData()
  formData.append('file', options.file)
  uploadSalesMaterial(formData).then((url) => {
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

const handleCreate = async () => {
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

onMounted(() => {
  getList()
})
</script>
