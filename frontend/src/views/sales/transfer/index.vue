<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.batteryId" placeholder="电池ID" style="width: 200px;" class="filter-item"
        @keyup.enter="handleFilter" />
      <el-button class="filter-item" type="primary" icon="Search" @click="handleFilter">
        搜索
      </el-button>
      <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="Refresh" @click="handleCreate">
        发起流转
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="电池ID" prop="batteryId" align="center" width="180" />
      <el-table-column label="原拥有者" prop="fromOwner" align="center" />
      <el-table-column label="新拥有者" prop="toOwner" align="center" />
      <el-table-column label="动作类型" prop="actionType" align="center">
        <template #default="{ row }">
          <el-tag>{{ row.actionType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发生时间" prop="createTime" align="center" width="180">
        <template #default="{ row }">
          <span>{{ row.createTime?.replace('T', ' ') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="交易哈希" align="center" width="200">
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
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog title="电池权属转移" v-model="dialogFormVisible">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="100px"
        style="width: 400px; margin-left:50px;">
        <el-form-item label="电池ID" prop="batteryId">
          <el-select v-model="temp.batteryId" filterable placeholder="请选择电池" @change="onBatteryChange">
            <el-option v-for="item in batteryOptions" :key="item.batteryId" :label="item.batteryId"
              :value="item.batteryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="原拥有者ID" prop="fromOwner">
          <el-select v-model="temp.fromOwner" filterable placeholder="请选择原拥有者" :disabled="true">
            <el-option v-for="item in ownerOptions" :key="item.userId" :label="item.label" :value="item.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="新拥有者ID" prop="toOwner">
          <el-select v-model="temp.toOwner" filterable placeholder="请选择新拥有者">
            <el-option v-for="item in ownerOptions" :key="'to-' + item.userId" :label="item.label"
              :value="item.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="流转类型" prop="actionType">
          <el-select v-model="temp.actionType" placeholder="请选择">
            <el-option label="出厂销售" value="SALE_TO_DEALER" />
            <el-option label="经销商销售" value="SALE_TO_CUSTOMER" />
            <el-option label="回收转让" value="TRANSFER_TO_RECYCLER" />
          </el-select>
        </el-form-item>
      </el-form>
      <div style="margin-left: 50px; color: #909399; font-size: 12px; margin-bottom: 20px;">
        <el-icon>
          <InfoFilled />
        </el-icon> 确认后将生成溯源记录并上链。
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">
            取消
          </el-button>
          <el-button type="primary" @click="createData">
            确认流转
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getTransferList, saveTransfer, getVehicleList } from '@/api/trace'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { getUserList } from '@/api/system'

const list = ref([])
const total = ref(0)
const listLoading = ref(true)
const route = useRoute()
const router = useRouter()
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  batteryId: undefined
})

const temp = reactive({
  batteryId: '',
  fromOwner: undefined,
  toOwner: undefined,
  actionType: ''
})

const dialogFormVisible = ref(false)

const batteryOptions = ref([])
const ownerOptions = ref([])

const rules = {
  batteryId: [{ required: true, message: '电池ID必填', trigger: 'change' }],
  fromOwner: [{ required: true, message: '原拥有者必填', trigger: 'change' }],
  toOwner: [{ required: true, message: '新拥有者必填', trigger: 'change' }]
}

const getList = () => {
  listLoading.value = true
  getTransferList(listQuery).then(response => {
    const pageData = response?.data || response
    list.value = pageData?.records || []
    total.value = pageData?.total || 0
    listLoading.value = false
  }).catch(() => {
    list.value = []
    total.value = 0
    listLoading.value = false
  })
}

const openTxHash = (txHash) => {
  const hash = String(txHash || '').trim()
  if (!hash) return
  router.push({ name: 'Trace', query: { txHash: hash } })
}

const handleFilter = () => {
  listQuery.pageNum = 1
  getList()
}

const resetTemp = () => {
  temp.batteryId = ''
  temp.fromOwner = undefined
  temp.toOwner = undefined
  temp.actionType = 'SALE_TO_DEALER'
}

const ensureBatteryOptions = async () => {
  if (batteryOptions.value.length > 0) return
  const res = await getVehicleList({ pageNum: 1, pageSize: 1000 })
  const pageData = res?.records || res?.data?.records || []
  batteryOptions.value = (pageData || []).map(item => {
    const batteryId = String(item.batteryId || '').trim()
    const ownerId = item.ownerId
    return {
      batteryId,
      ownerId
    }
  }).filter(item => item.batteryId && item.ownerId != null)
}

const onBatteryChange = (val) => {
  const id = String(val || '').trim()
  if (!id) {
    temp.fromOwner = undefined
    temp.toOwner = undefined
    return
  }
  const match = batteryOptions.value.find(item => item.batteryId === id)
  if (match && match.ownerId != null) {
    temp.fromOwner = match.ownerId
  }
  temp.toOwner = undefined
}

const ensureOwnerOptions = async () => {
  if (ownerOptions.value.length > 0) return
  const res = await getUserList({ pageNum: 1, pageSize: 1000 })
  const pageData = res || {}
  const rows = pageData.records || []
  ownerOptions.value = rows.map(item => {
    const id = item.userId
    const username = String(item.username || '').trim()
    const nickname = String(item.nickname || '').trim()
    const labelParts = [id, username, nickname].filter(Boolean)
    return {
      userId: id,
      label: labelParts.join(' / ')
    }
  }).filter(item => item.userId != null)
}

const handleCreate = () => {
  resetTemp()
  ensureBatteryOptions()
  ensureOwnerOptions()
  dialogFormVisible.value = true
}

const createData = () => {
  saveTransfer(temp).then(() => {
    dialogFormVisible.value = false
    ElMessage({
      message: '流转记录已上链',
      type: 'success',
      duration: 2000
    })
    getList()
  })
}

onMounted(() => {
  getList()

  ensureBatteryOptions()
  ensureOwnerOptions()

  const batteryId = route.query?.batteryId
  if (batteryId != null && String(batteryId).trim()) {
    listQuery.batteryId = String(batteryId).trim()
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
