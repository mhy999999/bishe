<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.batteryId" placeholder="电池ID" clearable class="filter-item"
        style="width: 200px; margin-right: 10px;" @keyup.enter="handleFilter" />
      <el-select v-model="listQuery.status" placeholder="状态" clearable class="filter-item"
        style="width: 150px; margin-right: 10px;">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
        <el-option label="已完成" :value="3" />
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
      <el-table-column label="解决方案" align="center">
        <template #default="{ row }">
          <span>{{ row?.solution || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="维修人" prop="maintainer" align="center" />
      <el-table-column label="审核意见" align="center" min-width="180">
        <template #default="{ row }">
          <span>{{ row?.auditOpinion || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审核人" align="center" width="120">
        <template #default="{ row }">
          <span>{{ row?.auditor || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审核时间" align="center" width="180">
        <template #default="{ row }">
          <span>{{ row?.auditTime || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="故障材料" align="center" min-width="160">
        <template #default="{ row }">
          <template v-if="parseMaterialUrls(row?.issueMaterialUrl).length > 0">
            <el-link v-for="(url, idx) in parseMaterialUrls(row?.issueMaterialUrl)" :key="url" type="primary"
              :underline="false" style="margin: 0 6px;" @click="openMaterialPreview(url)">
              文件{{ idx + 1 }}
            </el-link>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="完工材料" align="center" min-width="160">
        <template #default="{ row }">
          <template v-if="parseMaterialUrls(row?.completionMaterialUrl).length > 0">
            <el-link v-for="(url, idx) in parseMaterialUrls(row?.completionMaterialUrl)" :key="url" type="primary"
              :underline="false" style="margin: 0 6px;" @click="openMaterialPreview(url)">
              文件{{ idx + 1 }}
            </el-link>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type || 'info'">
            {{ statusMap[row.status]?.text || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="240" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button v-if="row.status === 1" type="success" size="small" @click="handleComplete(row)">
            提交完工
          </el-button>
          <template v-else-if="row.status === 2">
            <el-button type="primary" size="small" @click="handleRedo(row)">
              重新维修
            </el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog :title="dialogMode === 'edit' ? '重新维修' : '新增维修记录'" v-model="dialogFormVisible">
      <el-form ref="dataForm" :model="temp" :rules="rules" label-width="100px">
        <el-form-item label="电池ID" prop="batteryId">
          <el-select v-model="temp.batteryId" filterable placeholder="请选择电池ID" :loading="batteryLoading"
            style="width: 100%;">
            <el-option v-for="item in batteryOptions" :key="item.batteryId" :label="item.batteryId"
              :value="item.batteryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障类型" prop="faultType">
          <el-input v-model="temp.faultType" />
        </el-form-item>
        <el-form-item label="维修内容" prop="description">
          <el-input v-model="temp.description" type="textarea" />
        </el-form-item>
        <el-form-item label="维修人">
          <el-input v-model="temp.maintainer" placeholder="默认使用当前账号" />
        </el-form-item>
        <el-form-item label="故障材料说明" prop="issueMaterialDesc">
          <el-input v-model="temp.issueMaterialDesc" type="textarea" />
        </el-form-item>
        <el-form-item label="故障材料文件">
          <el-upload v-model:file-list="issueMaterialFileList" :http-request="handleIssueMaterialUpload"
            :on-remove="handleIssueMaterialRemove" multiple :limit="5" :on-exceed="handleIssueMaterialExceed">
            <el-button type="primary">上传文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">取消</el-button>
          <el-button type="primary" @click="createData">确认</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="提交完工材料" v-model="dialogCompleteVisible">
      <el-form :model="completeTemp" label-width="100px">
        <el-form-item label="解决方案">
          <el-input v-model="completeTemp.solution" type="textarea" />
        </el-form-item>
        <el-form-item label="完工材料说明">
          <el-input v-model="completeTemp.completionMaterialDesc" type="textarea" />
        </el-form-item>
        <el-form-item label="完工材料文件">
          <el-upload v-model:file-list="completionMaterialFileList" :http-request="handleCompletionMaterialUpload"
            :on-remove="handleCompletionMaterialRemove" multiple :limit="5" :on-exceed="handleCompletionMaterialExceed">
            <el-button type="primary">上传文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogCompleteVisible = false">取消</el-button>
          <el-button type="primary" @click="submitComplete">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMaintenanceList, saveMaintenance, updateMaintenance, uploadMaintenanceMaterial, completeMaintenance, getBatteryList } from '@/api/battery'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useRoute, useRouter } from 'vue-router'

const dataForm = ref()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

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
  3: { text: '已完成', type: 'info' }
}

const dialogFormVisible = ref(false)
const dialogMode = ref('create')
const temp = reactive({
  recordId: undefined,
  batteryId: '',
  faultType: '',
  description: '',
  maintainer: '',
  issueMaterialDesc: '',
  issueMaterialUrl: ''
})

const batteryOptions = ref([])
const batteryLoading = ref(false)

const loadMaintainableBatteries = () => {
  batteryLoading.value = true
  return getBatteryList({ pageNum: 1, pageSize: 1000 }).then((response) => {
    const pageData = response?.data || response
    const records = pageData?.records || []
    batteryOptions.value = records.filter(b => b && b.batteryId && b.status !== 2 && b.status !== 6)
    batteryLoading.value = false
  }).catch((err) => {
    console.error(err)
    batteryOptions.value = []
    batteryLoading.value = false
  })
}

const rules = {
  batteryId: [{ required: true, message: '电池ID必填', trigger: 'blur' }],
  faultType: [{ required: true, message: '故障类型必填', trigger: 'blur' }],
  description: [{ required: true, message: '维修内容必填', trigger: 'blur' }],
  issueMaterialDesc: [{ required: true, message: '故障材料说明必填', trigger: 'blur' }]
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

const openMaterialPreview = (rawUrl) => {
  router.push({
    name: 'SalesMaterialPreview',
    query: { url: String(rawUrl || ''), from: router.currentRoute.value.fullPath }
  })
}

const issueMaterialUrls = ref([])
const issueMaterialFileList = ref([])

const syncIssueMaterialFileList = () => {
  issueMaterialFileList.value = issueMaterialUrls.value.map((url, idx) => ({
    name: `文件${idx + 1}`,
    url,
    status: 'success'
  }))
}

const handleIssueMaterialExceed = () => {
  ElMessage.warning('故障材料文件最多上传 5 个')
}

const handleIssueMaterialUpload = (options) => {
  if (issueMaterialUrls.value.length >= 5) {
    ElMessage.warning('故障材料文件最多上传 5 个')
    options.onError && options.onError(new Error('文件数量超限'))
    return
  }
  const formData = new FormData()
  formData.append('file', options.file)
  uploadMaintenanceMaterial('issue', formData).then((url) => {
    if (issueMaterialUrls.value.length >= 5) {
      ElMessage.warning('故障材料文件最多上传 5 个')
      options.onError && options.onError(new Error('文件数量超限'))
      return
    }
    issueMaterialUrls.value.push(url)
    syncIssueMaterialFileList()
    options.onSuccess && options.onSuccess({ url }, options.file)
  }).catch((err) => {
    options.onError && options.onError(err)
  })
}

const handleIssueMaterialRemove = (uploadFile, uploadFiles) => {
  issueMaterialUrls.value = (uploadFiles || []).map(f => f.url).filter(Boolean)
  syncIssueMaterialFileList()
}

const dialogCompleteVisible = ref(false)
const completeTemp = reactive({
  recordId: undefined,
  solution: '',
  completionMaterialDesc: '',
  completionMaterialUrl: ''
})
const completionMaterialUrls = ref([])
const completionMaterialFileList = ref([])

const syncCompletionMaterialFileList = () => {
  completionMaterialFileList.value = completionMaterialUrls.value.map((url, idx) => ({
    name: `文件${idx + 1}`,
    url,
    status: 'success'
  }))
}

const handleCompletionMaterialExceed = () => {
  ElMessage.warning('完工材料文件最多上传 5 个')
}

const handleCompletionMaterialUpload = (options) => {
  if (completionMaterialUrls.value.length >= 5) {
    ElMessage.warning('完工材料文件最多上传 5 个')
    options.onError && options.onError(new Error('文件数量超限'))
    return
  }
  const formData = new FormData()
  formData.append('file', options.file)
  uploadMaintenanceMaterial('completion', formData).then((url) => {
    if (completionMaterialUrls.value.length >= 5) {
      ElMessage.warning('完工材料文件最多上传 5 个')
      options.onError && options.onError(new Error('文件数量超限'))
      return
    }
    completionMaterialUrls.value.push(url)
    syncCompletionMaterialFileList()
    options.onSuccess && options.onSuccess({ url }, options.file)
  }).catch((err) => {
    options.onError && options.onError(err)
  })
}

const handleCompletionMaterialRemove = (uploadFile, uploadFiles) => {
  completionMaterialUrls.value = (uploadFiles || []).map(f => f.url).filter(Boolean)
  syncCompletionMaterialFileList()
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

const handleCreate = async (prefillBatteryId) => {
  const prefill = (typeof prefillBatteryId === 'string' || typeof prefillBatteryId === 'number')
    ? String(prefillBatteryId).trim()
    : ''
  dialogMode.value = 'create'
  temp.recordId = undefined
  temp.batteryId = ''
  temp.faultType = ''
  temp.description = ''
  temp.maintainer = userStore.name || ''
  temp.issueMaterialDesc = ''
  temp.issueMaterialUrl = ''
  issueMaterialUrls.value = []
  syncIssueMaterialFileList()
  await loadMaintainableBatteries()
  if (prefill) temp.batteryId = prefill
  dialogFormVisible.value = true
}

const handleRedo = async (row) => {
  dialogMode.value = 'edit'
  temp.recordId = row?.recordId
  temp.batteryId = row?.batteryId || ''
  temp.faultType = row?.faultType || ''
  temp.description = row?.description || ''
  temp.maintainer = row?.maintainer || userStore.name || ''
  temp.issueMaterialDesc = row?.issueMaterialDesc || ''
  issueMaterialUrls.value = parseMaterialUrls(row?.issueMaterialUrl)
  syncIssueMaterialFileList()
  await loadMaintainableBatteries()
  dialogFormVisible.value = true
}

const createData = () => {
  if (!dataForm.value) {
    return
  }
  dataForm.value.validate((valid) => {
    if (!valid) {
      return
    }
    if (issueMaterialUrls.value.length <= 0) {
      ElMessage.warning('请至少上传 1 个故障材料文件')
      return
    }
    temp.issueMaterialUrl = JSON.stringify(issueMaterialUrls.value)
    const action = dialogMode.value === 'edit' ? updateMaintenance : saveMaintenance
    action(temp).then(() => {
      dialogFormVisible.value = false
      ElMessage.success(dialogMode.value === 'edit' ? '已重新提交维修审核' : '创建成功，已提交审核')
      getList()
    }).catch((err) => {
      console.error(err)
    })
  })
}

const handleComplete = (row) => {
  completeTemp.recordId = row?.recordId
  completeTemp.solution = ''
  completeTemp.completionMaterialDesc = ''
  completeTemp.completionMaterialUrl = ''
  completionMaterialUrls.value = []
  syncCompletionMaterialFileList()
  dialogCompleteVisible.value = true
}

const submitComplete = () => {
  const solution = String(completeTemp.solution || '').trim()
  if (!solution) {
    ElMessage.warning('解决方案不能为空')
    return
  }
  const desc = String(completeTemp.completionMaterialDesc || '').trim()
  if (!desc) {
    ElMessage.warning('完工材料说明不能为空')
    return
  }
  if (completionMaterialUrls.value.length <= 0) {
    ElMessage.warning('请至少上传 1 个完工材料文件')
    return
  }
  completeTemp.solution = solution
  completeTemp.completionMaterialDesc = desc
  completeTemp.completionMaterialUrl = JSON.stringify(completionMaterialUrls.value)
  completeMaintenance(completeTemp).then(() => {
    dialogCompleteVisible.value = false
    ElMessage.success('已提交完工材料')
    getList()
  }).catch((err) => {
    console.error(err)
  })
}

onMounted(() => {
  const batteryId = route.query?.batteryId
  if (batteryId != null && String(batteryId).trim()) {
    listQuery.batteryId = String(batteryId).trim()
  }
  getList()

  const action = String(route.query?.action || '').trim().toLowerCase()
  const openCreate = route.query?.openCreate
  const create = route.query?.create
  const shouldOpenCreate = action === 'create' || openCreate === '1' || create === '1'
  if (shouldOpenCreate) {
    handleCreate(batteryId)
  }
})
</script>
