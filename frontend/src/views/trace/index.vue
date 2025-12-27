<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.txHash" placeholder="交易哈希 (TxHash)" style="width: 300px;" class="filter-item"
        @keyup.enter="handleFilter" />
      <el-button class="filter-item" type="primary" icon="Search" @click="handleFilter">
        搜索交易
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="交易哈希" prop="txHash" align="center" width="220">
        <template #default="{ row }">
          <el-tooltip :content="row.txHash" placement="top">
            <span class="link-type">{{ row.txHash?.substring(0, 18) }}...</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="区块序号" prop="blockHeight" align="center" width="120">
        <template #header>
          <span>区块序号</span>
          <el-tooltip content="交易被打包进区块链的第几个区块，可理解为上链顺序" placement="top">
            <el-icon class="help-icon">
              <InfoFilled />
            </el-icon>
          </el-tooltip>
        </template>
        <template #default="{ row }">
          <el-tooltip :content="'第' + row.blockHeight + '个区块（上链顺序）'" placement="top">
            <el-tag type="info">#{{ row.blockHeight }}</el-tag>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="调用方法" prop="methodName" align="center" width="150" />
      <el-table-column label="上链参数 (Params)" prop="params" align="center">
        <template #default="{ row }">
          <el-popover placement="top-start" title="上链参数" :width="520" trigger="hover">
            <template #reference>
              <span class="params-summary">{{ getParamsSummary(row) }}</span>
            </template>
            <div class="params-popover">
              <div class="params-actions">
                <el-button link type="primary" size="small" @click="copyText(row.params)">复制原始JSON</el-button>
              </div>
              <el-descriptions v-if="getParamItems(row).length" :column="1" border size="small">
                <el-descriptions-item v-for="item in getParamItems(row)" :key="item.key" :label="item.label">
                  <el-tag v-if="item.tag" :type="item.tag.type" size="small">{{ item.tag.text }}</el-tag>
                  <span v-else>{{ item.text }}</span>
                </el-descriptions-item>
              </el-descriptions>
              <pre v-else class="params-raw">{{ formatJson(row.params) }}</pre>
            </div>
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column label="合约地址" prop="contractAddr" align="center" width="150">
        <template #default="{ row }">
          <el-tooltip :content="row.contractAddr" placement="top">
            <span>{{ row.contractAddr?.substring(0, 10) }}...</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" align="center" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上链时间" prop="createTime" align="center" width="180">
        <template #default="{ row }">
          <span>{{ row.createTime?.replace('T', ' ') }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getChainList } from '@/api/trace'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'

const route = useRoute()
const list = ref([])
const total = ref(0)
const listLoading = ref(true)
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  txHash: undefined
})

const getList = () => {
  listLoading.value = true
  getChainList(listQuery).then(response => {
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

const handleFilter = () => {
  listQuery.pageNum = 1
  getList()
}

const formatJson = (jsonStr) => {
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch (e) {
    return jsonStr
  }
}

const parseJson = (jsonStr) => {
  if (!jsonStr) return null
  if (typeof jsonStr === 'object') return jsonStr
  if (typeof jsonStr !== 'string') return null
  try {
    return JSON.parse(jsonStr)
  } catch (e) {
    return null
  }
}

const pad2 = (n) => String(n).padStart(2, '0')

const formatDateTime = (val) => {
  if (val == null || val === '') return ''
  let date
  if (typeof val === 'number') {
    date = new Date(val)
  } else if (typeof val === 'string') {
    const ts = Number(val)
    date = Number.isFinite(ts) ? new Date(ts) : new Date(val)
  } else {
    return String(val)
  }
  if (Number.isNaN(date.getTime())) return String(val)
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`
}

const buildStatusTag = (val, ctx) => {
  const n = typeof val === 'number' ? val : Number(val)
  const isQuality = ctx && ['ocv', 'acr', 'insulationRes', 'airTightness', 'inspector', 'checkTime'].some(k => Object.prototype.hasOwnProperty.call(ctx, k))
  if (isQuality && Number.isFinite(n)) {
    const map = {
      0: { text: '待审核', type: 'warning' },
      1: { text: '已通过', type: 'success' },
      2: { text: '已驳回', type: 'danger' }
    }
    return map[n] ? { text: map[n].text, type: map[n].type } : null
  }
  return Number.isFinite(n) ? { text: String(n), type: 'info' } : null
}

const getParamLabel = (key) => {
  const map = {
    batteryId: '电池ID',
    batchId: '批次ID',
    ocv: '开路电压(OCV)',
    acr: '交流内阻(ACR)',
    insulationRes: '绝缘电阻',
    airTightness: '气密性',
    inspector: '检验员',
    checkTime: '检验时间',
    status: '状态'
  }
  return map[key] || key
}

const formatParamItem = (key, val, ctx) => {
  if (key === 'status') {
    const tag = buildStatusTag(val, ctx)
    if (tag) return { text: '', tag }
  }
  if (key === 'airTightness') {
    const upper = String(val || '').toUpperCase()
    if (upper === 'PASS') return { text: '', tag: { text: '通过', type: 'success' } }
    if (upper === 'FAIL') return { text: '', tag: { text: '不通过', type: 'danger' } }
  }
  if (/time|date/i.test(key)) {
    return { text: formatDateTime(val), tag: null }
  }
  if (typeof val === 'object' && val !== null) {
    return { text: JSON.stringify(val), tag: null }
  }
  return { text: val == null ? '' : String(val), tag: null }
}

const getParamItems = (row) => {
  const obj = parseJson(row?.params)
  if (!obj || typeof obj !== 'object' || Array.isArray(obj)) return []
  return Object.keys(obj).map(key => {
    const formatted = formatParamItem(key, obj[key], obj)
    return { key, label: getParamLabel(key), text: formatted.text, tag: formatted.tag }
  })
}

const getParamsSummary = (row) => {
  const obj = parseJson(row?.params)
  if (!obj || typeof obj !== 'object' || Array.isArray(obj)) {
    const s = row?.params ? String(row.params) : ''
    return s.length > 40 ? `${s.slice(0, 40)}...` : (s || '-')
  }
  const parts = []
  if (obj.batteryId != null) parts.push(`电池ID:${obj.batteryId}`)
  if (obj.batchId != null) parts.push(`批次ID:${obj.batchId}`)
  if (obj.checkTime != null) parts.push(`时间:${formatDateTime(obj.checkTime)}`)
  if (obj.status != null) {
    const tag = buildStatusTag(obj.status, obj)
    parts.push(`状态:${tag?.text ?? obj.status}`)
  }
  const joined = parts.filter(Boolean).join('，')
  if (joined) return joined
  const raw = JSON.stringify(obj)
  return raw.length > 40 ? `${raw.slice(0, 40)}...` : raw
}

const copyText = async (text) => {
  const value = text == null ? '' : String(text)
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(value)
    } else {
      const el = document.createElement('textarea')
      el.value = value
      el.style.position = 'fixed'
      el.style.left = '-9999px'
      document.body.appendChild(el)
      el.select()
      document.execCommand('copy')
      document.body.removeChild(el)
    }
    ElMessage.success('已复制')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

onMounted(() => {
  if (route.query?.txHash) {
    listQuery.txHash = route.query.txHash
  }
  getList()
})

watch(
  () => route.query?.txHash,
  (txHash) => {
    listQuery.txHash = txHash || undefined
    listQuery.pageNum = 1
    getList()
  }
)
</script>

<style scoped>
.link-type {
  color: #409eff;
  cursor: pointer;
}

.params-summary {
  font-family: monospace;
  color: #606266;
  display: inline-block;
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.params-popover {
  max-height: 360px;
  overflow: auto;
}

.params-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 6px;
}

.params-raw {
  margin: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}

.help-icon {
  margin-left: 4px;
  color: #909399;
  vertical-align: -2px;
}
</style>
