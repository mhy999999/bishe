<template>
  <div class="app-container">
    <div v-if="isPendingPage">
      <div class="filter-container">
        <el-input v-model="pendingQuery.batteryId" placeholder="电池ID" style="width: 200px;" class="filter-item"
          @keyup.enter="handlePendingFilter" />
        <el-button class="filter-item" type="primary" icon="Search" style="margin-left: 10px;"
          @click="handlePendingFilter">
          搜索
        </el-button>
      </div>

      <el-table v-loading="pendingLoading" :data="pendingList" border fit highlight-current-row style="width: 100%;">
        <el-table-column label="电池ID" prop="batteryId" align="center" width="180" />
        <el-table-column label="批次ID" prop="batchId" align="center" width="100" />
        <el-table-column label="类型" prop="typeCode" align="center" />
        <el-table-column label="容量(kWh)" prop="capacity" align="center" />
        <el-table-column label="生产时间" prop="createTime" align="center" width="180">
          <template #default="{ row }">
            <span>{{ row.createTime?.replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="100">
          <template #default="{ row }">
            <el-tag type="info">待质检</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleCreate(row)">
              录入质检
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="pendingTotal > 0" :total="pendingTotal" v-model:page="pendingQuery.pageNum"
        v-model:limit="pendingQuery.pageSize" @pagination="getPendingList" />
    </div>

    <div v-else>
      <div class="filter-container">
        <el-input v-model="listQuery.batteryId" placeholder="电池ID" style="width: 200px;" class="filter-item"
          @keyup.enter="handleFilter" />
        <el-select v-model="listQuery.status" placeholder="审核状态" clearable class="filter-item"
          style="width: 150px; margin-left: 10px;">
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
        <el-button class="filter-item" type="primary" icon="Search" style="margin-left: 10px;" @click="handleFilter">
          搜索
        </el-button>
        <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="Edit"
          @click="handleCreate(null)">
          手动录入
        </el-button>
      </div>

      <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;"
        @row-click="handleRecordRowClick">
        <el-table-column label="电池ID" prop="batteryId" align="center" width="180" />
        <el-table-column label="开路电压(V)" prop="ocv" align="center" />
        <el-table-column label="交流内阻(mΩ)" prop="acr" align="center" />
        <el-table-column label="绝缘电阻(MΩ)" prop="insulationRes" align="center" />
        <el-table-column label="气密性" prop="airTightness" align="center" width="100" />
        <el-table-column label="检测人" prop="inspector" align="center" />
        <el-table-column label="检测时间" prop="checkTime" align="center" width="180">
          <template #default="{ row }">
            <span>{{ row.checkTime?.replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" align="center" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">
              {{ statusMap[row.status]?.text || '待审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上链哈希" align="center" width="200">
          <template #default="{ row }">
            <el-tooltip :content="row.dataHash" placement="top" effect="light">
              <el-link type="primary" :underline="false">{{ row.dataHash ? row.dataHash.substring(0, 10) + '...' : '未上链'
                }}</el-link>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
        @pagination="getList" />
    </div>

    <el-dialog title="录入质检记录" v-model="dialogFormVisible">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="100px"
        style="width: 400px; margin-left:50px;">
        <el-form-item label="电池ID" prop="batteryId">
          <el-input v-model="temp.batteryId" :disabled="!!temp.fromPending" />
        </el-form-item>
        <el-form-item label="开路电压(V)" prop="ocv">
          <el-input v-model="temp.ocv" />
        </el-form-item>
        <el-form-item label="交流内阻(mΩ)" prop="acr">
          <el-input v-model="temp.acr" />
        </el-form-item>
        <el-form-item label="绝缘电阻(MΩ)" prop="insulationRes">
          <el-input v-model="temp.insulationRes" />
        </el-form-item>
        <el-form-item label="气密性" prop="airTightness">
          <el-select v-model="temp.airTightness" placeholder="请选择">
            <el-option label="合格" value="PASS" />
            <el-option label="不合格" value="FAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="检测人" prop="inspector">
          <el-input v-model="temp.inspector" />
        </el-form-item>
      </el-form>
      <div style="margin-left: 50px; color: #909399; font-size: 12px; margin-bottom: 20px;">
        <el-icon>
          <InfoFilled />
        </el-icon> 提交后将自动上链存证，生成不可篡改的交易哈希。
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">
            取消
          </el-button>
          <el-button type="primary" @click="createData">
            确认并上链
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getQualityList, saveQuality, getBatteryList } from '@/api/battery'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const isPendingPage = computed(() => route.name === 'QualityPending')

const pendingList = ref([])
const pendingTotal = ref(0)
const pendingLoading = ref(false)
const pendingQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  batteryId: undefined,
  status: 3
})

const list = ref([])
const total = ref(0)
const listLoading = ref(false)
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  batteryId: undefined,
  status: undefined
})

const statusMap = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' }
}

const temp = reactive({
  batteryId: '',
  ocv: '',
  acr: '',
  insulationRes: '',
  airTightness: 'PASS',
  inspector: '',
  checkTime: '',
  fromPending: false
})

const dialogFormVisible = ref(false)

const rules = {
  batteryId: [{ required: true, message: '电池ID必填', trigger: 'blur' }],
  ocv: [{ required: true, message: '开路电压必填', trigger: 'blur' }],
  acr: [{ required: true, message: '交流内阻必填', trigger: 'blur' }],
  insulationRes: [{ required: true, message: '绝缘电阻必填', trigger: 'blur' }],
  airTightness: [{ required: true, message: '气密性必填', trigger: 'change' }]
}

const getPendingList = () => {
  pendingLoading.value = true
  getBatteryList(pendingQuery).then(res => {
    const pageData = res?.data || res
    pendingList.value = pageData?.records || []
    pendingTotal.value = pageData?.total || 0
    pendingLoading.value = false
  }).catch(err => {
    console.error(err)
    pendingList.value = []
    pendingTotal.value = 0
    pendingLoading.value = false
  })
}

const handlePendingFilter = () => {
  pendingQuery.pageNum = 1
  getPendingList()
}

const getList = () => {
  listLoading.value = true
  getQualityList(listQuery).then(res => {
    const pageData = res?.data || res
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
  temp.batteryId = ''
  temp.ocv = ''
  temp.acr = ''
  temp.insulationRes = ''
  temp.airTightness = 'PASS'
  temp.inspector = userStore.name || ''
  temp.checkTime = new Date().toISOString()
  temp.fromPending = false
}

const handleCreate = (row) => {
  resetTemp()
  if (row && row.batteryId) {
    temp.batteryId = row.batteryId
    temp.fromPending = true
  }
  dialogFormVisible.value = true
}

const createData = () => {
  saveQuality(temp).then(() => {
    dialogFormVisible.value = false
    ElMessage({
      message: '录入成功，已上链',
      type: 'success',
      duration: 2000
    })
    if (isPendingPage.value) {
      getPendingList()
    } else {
      getList()
    }
  })
}

const handleRecordRowClick = (row) => {
  if (!row?.dataHash) return
  router.push({ name: 'Trace', query: { txHash: row.dataHash } })
}

onMounted(() => {
  if (isPendingPage.value) {
    getPendingList()
  } else {
    getList()
  }
})

watch(isPendingPage, (val) => {
  if (val) {
    getPendingList()
  } else {
    getList()
  }
})
</script>
