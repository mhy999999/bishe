<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="按电池ID溯源" name="battery">
        <div class="filter-container">
          <el-input v-model="batteryQuery.batteryIdInput" placeholder="输入电池ID" style="width: 220px;" class="filter-item"
            @keyup.enter="handleBatteryFilter" />
          <el-select v-model="batteryQuery.batteryId" clearable filterable placeholder="选择电池ID"
            style="width: 220px; margin-left: 10px;" class="filter-item" @change="handleBatteryFilter">
            <el-option v-for="item in batteryOptions" :key="item.batteryId" :label="item.batteryId"
              :value="item.batteryId" />
          </el-select>
          <el-button class="filter-item" type="primary" icon="Search" @click="handleBatteryFilter">
            查询溯源
          </el-button>
        </div>

        <el-table v-loading="traceLoading" :data="traceEvents" border fit highlight-current-row style="width: 100%;">
          <el-table-column label="时间" prop="timeText" align="center" width="180" />
          <el-table-column label="事件" prop="typeText" align="center" width="120">
            <template #default="{ row }">
              <el-tag :type="row.typeTag || 'info'">{{ row.typeText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="内容" prop="content" align="left" />
          <el-table-column label="TxHash" align="center" width="200">
            <template #default="{ row }">
              <template v-if="row.txHash">
                <el-tooltip :content="row.txHash" placement="top" effect="light">
                  <el-link type="primary" :underline="false" @click="openTxHash(row.txHash)">
                    {{ row.txHash.substring(0, 10) + '...' }}
                  </el-link>
                </el-tooltip>
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="按交易查询" name="tx">
        <div class="filter-container">
          <el-input v-model="chainQuery.txHash" placeholder="交易哈希 (TxHash)" style="width: 300px;" class="filter-item"
            @keyup.enter="handleTxFilter" />
          <el-button class="filter-item" type="primary" icon="Search" @click="handleTxFilter">
            搜索交易
          </el-button>
        </div>

        <el-table v-loading="chainLoading" :data="chainList" border fit highlight-current-row style="width: 100%;">
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

        <pagination v-show="chainTotal > 0" :total="chainTotal" v-model:page="chainQuery.pageNum"
          v-model:limit="chainQuery.pageSize" @pagination="getChainTxList" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBatteryList } from '@/api/battery'
import { getChainList, getMaintenanceList, getRecyclingList, getSalesList, getTransferList } from '@/api/trace'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const activeTab = ref('battery')

const chainList = ref([])
const chainTotal = ref(0)
const chainLoading = ref(true)
const chainQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  txHash: undefined
})

const batteryOptions = ref([])
const batteryQuery = reactive({
  batteryId: undefined,
  batteryIdInput: ''
})
const traceEvents = ref([])
const traceLoading = ref(false)

const resolveBatteryId = () => {
  const fromInput = String(batteryQuery.batteryIdInput || '').trim()
  if (fromInput) return fromInput
  const fromSelect = batteryQuery.batteryId
  const value = String(fromSelect || '').trim()
  return value || undefined
}

const loadBatteryOptions = () => {
  return getBatteryList({
    pageNum: 1,
    pageSize: 1000
  }).then(res => {
    const pageData = res?.data || res
    batteryOptions.value = pageData?.records || []
  }).catch(() => {
    batteryOptions.value = []
  })
}

const getChainTxList = () => {
  chainLoading.value = true
  getChainList(chainQuery).then(response => {
    const pageData = response.data || response
    if (pageData) {
      chainList.value = pageData.records || []
      chainTotal.value = pageData.total || 0
    } else {
      chainList.value = []
      chainTotal.value = 0
    }
    chainLoading.value = false
  }).catch(() => {
    chainList.value = []
    chainTotal.value = 0
    chainLoading.value = false
  })
}

const handleTxFilter = () => {
  chainQuery.pageNum = 1
  const query = { ...route.query }
  if (chainQuery.txHash) query.txHash = chainQuery.txHash
  else delete query.txHash
  delete query.batteryId
  router.replace({ name: route.name, query })
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

const parseToDate = (val) => {
  if (!val) return null
  if (val instanceof Date) return val
  if (typeof val === 'number') return new Date(val)
  if (typeof val === 'string') {
    const ts = Number(val)
    if (Number.isFinite(ts)) return new Date(ts)
    const d = new Date(val.replace('T', ' '))
    return Number.isNaN(d.getTime()) ? null : d
  }
  return null
}

const sortByTimeAsc = (a, b) => {
  const t1 = a?.time ? a.time.getTime() : 0
  const t2 = b?.time ? b.time.getTime() : 0
  return t1 - t2
}

const buildTraceEvents = ({ transfers, sales, maintenance, recycling }) => {
  const events = []

    ; (transfers || []).forEach(r => {
      const time = parseToDate(r.createTime)
      const content = `类型：${r.actionType || '-'}，原拥有者：${r.fromOwner ?? '-'}，新拥有者：${r.toOwner ?? '-'}`
      events.push({
        time,
        timeText: time ? formatDateTime(time) : '',
        typeText: '流转',
        typeTag: 'info',
        content,
        txHash: r.txHash || ''
      })
    })

    ; (sales || []).forEach(r => {
      const time = parseToDate(r.salesDate)
      const price = r.salesPrice != null ? String(r.salesPrice) : '-'
      const content = `买家：${r.buyerName || '-'}，售价：${price}，销售员：${r.salesPerson || '-'}`
      events.push({
        time,
        timeText: time ? formatDateTime(time) : '',
        typeText: '销售',
        typeTag: 'success',
        content,
        txHash: r.txHash || ''
      })
    })

    ; (maintenance || []).forEach(r => {
      const time = parseToDate(r.createTime)
      const content = `故障：${r.faultType || '-'}，维修人：${r.maintainer || '-'}，内容：${r.description || '-'}`
      events.push({
        time,
        timeText: time ? formatDateTime(time) : '',
        typeText: '维修',
        typeTag: 'warning',
        content,
        txHash: r.txHash || ''
      })
    })

    ; (recycling || []).forEach(r => {
      const time = parseToDate(r.createTime)
      const value = r.residualValue != null ? String(r.residualValue) : '-'
      const content = `外观：${r.appearance || '-'}，残值：${value}，建议：${r.suggestion || '-'}`
      events.push({
        time,
        timeText: time ? formatDateTime(time) : '',
        typeText: '回收',
        typeTag: 'danger',
        content,
        txHash: r.dataHash || ''
      })
    })

  events.sort(sortByTimeAsc)
  return events
}

const fetchTraceByBatteryId = (batteryId) => {
  const id = String(batteryId || '').trim()
  if (!id) {
    traceEvents.value = []
    return Promise.resolve()
  }
  traceLoading.value = true
  return Promise.all([
    getTransferList({ pageNum: 1, pageSize: 1000, batteryId: id }),
    getSalesList({ pageNum: 1, pageSize: 1000, batteryId: id }),
    getMaintenanceList({ pageNum: 1, pageSize: 1000, batteryId: id }),
    getRecyclingList({ pageNum: 1, pageSize: 1000, batteryId: id })
  ]).then(([tRes, sRes, mRes, rRes]) => {
    const tPage = tRes?.data || tRes
    const sPage = sRes?.data || sRes
    const mPage = mRes?.data || mRes
    const rPage = rRes?.data || rRes
    traceEvents.value = buildTraceEvents({
      transfers: tPage?.records || [],
      sales: sPage?.records || [],
      maintenance: mPage?.records || [],
      recycling: rPage?.records || []
    })
    traceLoading.value = false
  }).catch(() => {
    traceEvents.value = []
    traceLoading.value = false
  })
}

const handleBatteryFilter = () => {
  const id = resolveBatteryId()
  const input = String(batteryQuery.batteryIdInput || '').trim()
  if (input && batteryQuery.batteryId && String(batteryQuery.batteryId) !== input) {
    batteryQuery.batteryId = undefined
  }
  if (batteryQuery.batteryId) {
    batteryQuery.batteryIdInput = String(batteryQuery.batteryId || '')
  } else {
    batteryQuery.batteryIdInput = id || ''
  }
  const query = { ...route.query }
  if (id) query.batteryId = id
  else delete query.batteryId
  delete query.txHash
  router.replace({ name: route.name, query })
}

const openTxHash = (txHash) => {
  const hash = String(txHash || '').trim()
  if (!hash) return
  activeTab.value = 'tx'
  router.push({ name: route.name, query: { txHash: hash } })
}

onMounted(() => {
  if (route.query?.batteryId) {
    activeTab.value = 'battery'
    batteryQuery.batteryId = undefined
    batteryQuery.batteryIdInput = String(route.query.batteryId || '')
  } else if (route.query?.txHash) {
    activeTab.value = 'tx'
    chainQuery.txHash = route.query.txHash
  }
  loadBatteryOptions()
  if (activeTab.value === 'tx') {
    getChainTxList()
  } else {
    const id = resolveBatteryId()
    if (id) fetchTraceByBatteryId(id)
  }
})

watch(
  () => route.query?.txHash,
  (txHash) => {
    if (!txHash) return
    activeTab.value = 'tx'
    chainQuery.txHash = txHash || undefined
    chainQuery.pageNum = 1
    getChainTxList()
  }
)

watch(
  () => route.query?.batteryId,
  (batteryId) => {
    if (!batteryId) {
      batteryQuery.batteryId = undefined
      batteryQuery.batteryIdInput = ''
      traceEvents.value = []
      return
    }
    activeTab.value = 'battery'
    batteryQuery.batteryIdInput = String(batteryId || '')
    batteryQuery.batteryId = undefined
    fetchTraceByBatteryId(String(batteryId || ''))
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
