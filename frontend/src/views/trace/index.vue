<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="按电池ID溯源" name="battery">
        <div class="filter-container">
          <el-autocomplete v-model="batteryQuery.batteryIdInput" placeholder="输入/选择电池ID" style="width: 320px;"
            class="filter-item" :fetch-suggestions="querySearchBatteryId" clearable :trigger-on-focus="true"
            :highlight-first-item="true" :debounce="200" @select="handleBatterySelect"
            @keyup.enter="handleBatteryFilter" />
          <el-button class="filter-item" type="primary" icon="Search" @click="handleBatteryFilter">
            查询溯源
          </el-button>
        </div>

        <template v-if="showAllBatteries">
          <el-table :data="batteryOptions" border fit highlight-current-row style="width: 100%;">
            <el-table-column label="电池ID" prop="batteryId" align="center" width="220" />
            <el-table-column label="生产商" prop="manufacturer" align="center" width="160" />
            <el-table-column label="类型" prop="typeCode" align="center" width="120" />
            <el-table-column label="容量(kWh)" prop="capacity" align="center" width="120" />
            <el-table-column label="电压(V)" prop="voltage" align="center" width="120" />
            <el-table-column label="状态" prop="status" align="center" width="120">
              <template #default="{ row }">
                <el-tag :type="batteryStatusMap[row?.status]?.type || 'info'">
                  {{ batteryStatusMap[row?.status]?.text || (row?.status ?? '-') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="生产日期" prop="produceDate" align="center" width="140">
              <template #default="{ row }">
                <span>{{ row.produceDate || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="120">
              <template #default="{ row }">
                <el-button type="primary" link @click.stop="openBatteryTrace(row.batteryId)">查看溯源</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <template v-else>
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
                    <el-link type="primary" underline="never" @click="openTxHash(row.txHash)">
                      {{ row.txHash.substring(0, 10) + '...' }}
                    </el-link>
                  </el-tooltip>
                </template>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>

          <el-card v-if="traceEvents.length" shadow="never" style="margin-top: 12px;">
            <template #header>
              <div style="display:flex; align-items:center; justify-content:space-between;">
                <span>生命周期图</span>
                <span style="color:#909399; font-size:12px;">可拖拽/缩放，点击节点查看TxHash</span>
              </div>
            </template>
            <div ref="batteryChartRef" class="trace-chart" />
          </el-card>
        </template>
      </el-tab-pane>

      <el-tab-pane label="按交易查询" name="tx">
        <div class="filter-container">
          <el-input v-model="chainQuery.txHash" placeholder="交易哈希 (TxHash)" style="width: 300px;" class="filter-item"
            clearable @keyup.enter="handleTxFilter" />
          <el-button class="filter-item" type="primary" icon="Search" @click="handleTxFilter">
            搜索交易
          </el-button>
        </div>

        <el-table v-loading="chainLoading" :data="chainList" border fit highlight-current-row style="width: 100%;">
          <el-table-column label="交易哈希" prop="txHash" align="center" width="220">
            <template #default="{ row }">
              <el-tooltip :content="row.txHash" placement="top">
                <el-link type="primary" underline="never" @click="openTxHash(row.txHash)">
                  {{ row.txHash?.substring(0, 18) }}...
                </el-link>
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

        <el-card v-if="chainQuery.txHash || txTraceLoading || txChartEvents.length" shadow="never"
          style="margin-top: 12px;">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between;">
              <span>关联电池生命周期图</span>
              <span style="color:#909399; font-size:12px;">电池ID：{{ txBatteryId || '-' }}</span>
            </div>
          </template>
          <div v-if="!txTraceLoading && !txChartEvents.length" style="padding: 18px 0;">
            <el-empty description="暂无可展示的图表数据" />
          </div>
          <div v-else v-loading="txTraceLoading" ref="txChartRef" class="trace-chart" />
        </el-card>

        <pagination v-show="chainTotal > 0" :total="chainTotal" v-model:page="chainQuery.pageNum"
          v-model:limit="chainQuery.pageSize" @pagination="getChainTxList" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, nextTick, onBeforeUnmount, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBatteryList } from '@/api/battery'
import { getChainList, getMaintenanceList, getRecyclingList, getSalesList, getTransferList, getVehicleList } from '@/api/trace'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { BATTERY_STATUS_MAP } from '@/constants/batteryStatus'

const route = useRoute()
const router = useRouter()

const activeTab = ref('tx')

const chainList = ref([])
const chainTotal = ref(0)
const chainLoading = ref(false)
const chainQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  txHash: ''
})

const batteryOptions = ref([])
const batteryQuery = reactive({
  batteryIdInput: ''
})
const traceEvents = ref([])
const traceLoading = ref(false)

const batteryIdTrimmed = computed(() => String(batteryQuery.batteryIdInput || '').trim())
const showAllBatteries = computed(() => activeTab.value === 'battery' && !batteryIdTrimmed.value)
const batteryStatusMap = BATTERY_STATUS_MAP

const txBatteryId = ref('')
const txTraceEvents = ref([])
const txTraceLoading = ref(false)

const batteryChartRef = ref(null)
const txChartRef = ref(null)
let batteryChart = null
let txChart = null

const resolveBatteryId = () => {
  const fromInput = String(batteryQuery.batteryIdInput || '').trim()
  return fromInput || undefined
}

const querySearchBatteryId = (queryString, cb) => {
  const q = String(queryString || '').trim().toLowerCase()
  const unique = Array.from(new Set((batteryOptions.value || []).map(r => String(r?.batteryId || '').trim()).filter(Boolean)))
  const matched = q ? unique.filter(v => v.toLowerCase().includes(q)) : unique
  cb(matched.slice(0, 20).map(v => ({ value: v })))
}

const handleBatterySelect = () => {
  handleBatteryFilter()
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
    if (chainQuery.txHash) {
      syncTxTraceFromChainList()
    } else {
      txBatteryId.value = ''
      txTraceEvents.value = []
    }
    chainLoading.value = false
  }).catch(() => {
    chainList.value = []
    chainTotal.value = 0
    txBatteryId.value = ''
    txTraceEvents.value = []
    chainLoading.value = false
  })
}

const handleTxFilter = () => {
  chainQuery.pageNum = 1
  const trimmed = String(chainQuery.txHash || '').trim()
  chainQuery.txHash = trimmed
  const query = { ...route.query }
  if (trimmed) query.txHash = trimmed
  else delete query.txHash
  delete query.batteryId
  router.replace({ name: route.name, query })

  if (!trimmed) {
    txBatteryId.value = ''
    txTraceEvents.value = []
  }
  getChainTxList()
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

const getEventColor = (typeText) => {
  const map = {
    '流转': '#409eff',
    '销售': '#67c23a',
    '维修': '#e6a23c',
    '回收': '#f56c6c'
  }
  return map[typeText] || '#909399'
}

const getEventSymbol = (typeText) => {
  const map = {
    '流转': 'roundRect',
    '销售': 'circle',
    '维修': 'diamond',
    '回收': 'triangle'
  }
  return map[typeText] || 'circle'
}

const getTimeShort = (timeText) => {
  const text = String(timeText || '').trim()
  if (!text) return ''
  const parts = text.split(' ')
  return parts.length >= 2 ? parts[1] : text
}

const buildLifecycleChartOption = (events) => {
  const list = (events || []).slice().sort(sortByTimeAsc)
  const gapX = 220
  const gapY = 90
  const groupMap = new Map()

  list.forEach((e, idx) => {
    const key = e?.time ? Math.floor(e.time.getTime() / 1000) * 1000 : idx
    const arr = groupMap.get(key) || []
    arr.push(e)
    groupMap.set(key, arr)
  })

  const groups = Array.from(groupMap.entries()).sort((a, b) => a[0] - b[0])
  const nodes = []
  const orderedIds = []
  let nodeSeq = 0

  groups.forEach(([, arr], groupIndex) => {
    const x = groupIndex * gapX
    const n = arr.length
    arr.forEach((e, j) => {
      const title = e?.typeText || ''
      const timeText = e?.timeText || ''
      const timeShort = getTimeShort(timeText)
      const y = (j - (n - 1) / 2) * gapY
      const id = String(nodeSeq++)
      orderedIds.push(id)
      nodes.push({
        id,
        name: title,
        x,
        y,
        value: {
          ...e,
          title,
          time: timeText
        },
        symbol: getEventSymbol(title),
        symbolSize: 52,
        itemStyle: {
          color: getEventColor(title),
          borderColor: '#ffffff',
          borderWidth: 2
        },
        label: {
          show: true,
          position: 'bottom',
          distance: 10,
          formatter: timeShort ? `{t|${title}}\n{m|${timeShort}}` : `{t|${title}}`,
          backgroundColor: '#ffffff',
          borderColor: '#ebeef5',
          borderWidth: 1,
          borderRadius: 6,
          padding: [6, 8],
          rich: {
            t: {
              color: '#303133',
              fontSize: 12,
              fontWeight: 600,
              lineHeight: 16,
              align: 'center'
            },
            m: {
              color: '#909399',
              fontSize: 11,
              lineHeight: 14,
              align: 'center'
            }
          }
        }
      })
    })
  })

  const links = orderedIds.slice(1).map((id, idx) => ({
    source: orderedIds[idx],
    target: id
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: (p) => {
        const v = p?.data?.value || {}
        const lines = []
        if (v.title) lines.push(`<div style="font-weight:600; margin-bottom:6px;">${v.title}</div>`)
        if (v.timeText) lines.push(`<div>时间：${v.timeText}</div>`)
        if (v.content) lines.push(`<div style="margin-top:6px; white-space:pre-wrap;">${v.content}</div>`)
        if (v.txHash) lines.push(`<div style="margin-top:6px; color:#909399;">TxHash：${v.txHash}</div>`)
        return lines.join('') || '-'
      }
    },
    grid: {
      left: 10,
      right: 10,
      top: 10,
      bottom: 10,
      containLabel: true
    },
    series: [
      {
        type: 'graph',
        layout: 'none',
        data: nodes,
        links,
        roam: true,
        scaleLimit: {
          min: 0.5,
          max: 3
        },
        edgeSymbol: ['none', 'arrow'],
        edgeSymbolSize: 8,
        lineStyle: {
          color: '#dcdfe6',
          width: 2,
          curveness: 0.18
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: {
            width: 3
          }
        }
      }
    ]
  }
}

const ensureChart = (elRef, current) => {
  const el = elRef.value
  if (!el) return current
  if (current && !current.isDisposed()) {
    const dom = typeof current.getDom === 'function' ? current.getDom() : null
    if (dom === el) return current
    current.dispose()
  }
  const existing = echarts.getInstanceByDom(el)
  return existing || echarts.init(el)
}

const renderBatteryChart = () => {
  batteryChart = ensureChart(batteryChartRef, batteryChart)
  if (!batteryChart) return
  handleChartClick(batteryChart)
  const ev = traceEvents.value || []
  if (!ev.length) {
    batteryChart.clear()
    return
  }
  batteryChart.setOption(buildLifecycleChartOption(ev), true)
  requestAnimationFrame(() => {
    if (batteryChart && !batteryChart.isDisposed()) batteryChart.resize()
  })
}

const renderTxChart = () => {
  txChart = ensureChart(txChartRef, txChart)
  if (!txChart) return
  handleChartClick(txChart)
  const ev = txChartEvents.value || []
  if (!ev.length) {
    txChart.clear()
    return
  }
  txChart.setOption(buildLifecycleChartOption(ev), true)
  requestAnimationFrame(() => {
    if (txChart && !txChart.isDisposed()) txChart.resize()
  })
}

const buildTxOnlyEvents = (rows) => {
  const list = (rows || []).map(r => {
    const time = parseToDate(r?.createTime)
    const method = r?.methodName ? String(r.methodName) : '交易'
    const statusText = r?.status === 1 ? '成功' : (r?.status === 0 ? '失败' : String(r?.status ?? '-'))
    const content = `方法：${method}，状态：${statusText}，参数：${getParamsSummary(r)}`
    return {
      time,
      timeText: time ? formatDateTime(time) : String(r?.createTime || ''),
      typeText: '交易',
      typeTag: 'info',
      content,
      txHash: r?.txHash || ''
    }
  })
  list.sort(sortByTimeAsc)
  return list
}

const txChartEvents = computed(() => {
  if (txTraceEvents.value?.length) return txTraceEvents.value
  if (chainQuery.txHash && chainList.value?.length) return buildTxOnlyEvents(chainList.value)
  return []
})

const buildTraceEvents = ({ transfers, sales, maintenance, recycling, vehicleBindings, bindTxHashByVin }) => {
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

    ; (vehicleBindings || []).forEach(r => {
      const time = parseToDate(r.bindTime)
      const vin = String(r.vin || '').trim()
      const vinKey = vin ? vin.toUpperCase() : ''
      const txHash = (bindTxHashByVin && vinKey) ? (bindTxHashByVin.get(vinKey) || '') : ''
      const ownerId = r.ownerId != null ? String(r.ownerId) : '-'
      const content = `VIN：${vin || '-'}，品牌：${r.brand || '-'}，型号：${r.model || '-'}，车主ID：${ownerId}`
      events.push({
        time,
        timeText: time ? formatDateTime(time) : '',
        typeText: '车电绑定',
        typeTag: 'info',
        content,
        txHash
      })
    })

  events.sort(sortByTimeAsc)
  return events
}

const fetchTraceEventsByBatteryId = (batteryId) => {
  const id = String(batteryId || '').trim()
  if (!id) {
    return Promise.resolve()
  }
  return Promise.allSettled([
    getTransferList({ pageNum: 1, pageSize: 1000, batteryId: id }),
    getSalesList({ pageNum: 1, pageSize: 1000, batteryId: id }),
    getMaintenanceList({ pageNum: 1, pageSize: 1000, batteryId: id }),
    getRecyclingList({ pageNum: 1, pageSize: 1000, batteryId: id }),
    getVehicleList({ pageNum: 1, pageSize: 1000, batteryId: id }),
    getChainList({ pageNum: 1, pageSize: 1000, methodName: 'bindVehicle', batteryId: id })
  ]).then(([tRes, sRes, mRes, rRes, vRes, cRes]) => {
    const tOk = tRes?.status === 'fulfilled' ? tRes.value : null
    const sOk = sRes?.status === 'fulfilled' ? sRes.value : null
    const mOk = mRes?.status === 'fulfilled' ? mRes.value : null
    const rOk = rRes?.status === 'fulfilled' ? rRes.value : null
    const vOk = vRes?.status === 'fulfilled' ? vRes.value : null
    const cOk = cRes?.status === 'fulfilled' ? cRes.value : null

    const tPage = tOk?.data || tOk
    const sPage = sOk?.data || sOk
    const mPage = mOk?.data || mOk
    const rPage = rOk?.data || rOk
    const vPage = vOk?.data || vOk
    const cPage = cOk?.data || cOk

    const bindTxHashByVin = new Map()
      ; (cPage?.records || []).forEach(row => {
        if (!row) return
        const root = parseJson(row?.params)
        const vin = String(root?.vin || '').trim()
        if (!vin) return
        const key = vin.toUpperCase()
        if (!bindTxHashByVin.has(key)) {
          bindTxHashByVin.set(key, String(row.txHash || ''))
        }
      })

    return buildTraceEvents({
      transfers: tPage?.records || [],
      sales: sPage?.records || [],
      maintenance: mPage?.records || [],
      recycling: rPage?.records || [],
      vehicleBindings: vPage?.records || [],
      bindTxHashByVin
    })
  }).catch(() => [])
}

const fetchTraceByBatteryId = (batteryId) => {
  const id = String(batteryId || '').trim()
  if (!id) {
    traceEvents.value = []
    return Promise.resolve()
  }
  traceLoading.value = true
  return fetchTraceEventsByBatteryId(id).then(events => {
    traceEvents.value = events || []
  }).finally(() => {
    traceLoading.value = false
  })
}

const extractBatteryIdFromChainRow = (row) => {
  const root = parseJson(row?.params)
  const stack = [{ value: root, depth: 0 }]
  const visited = new WeakSet()

  const normalize = (v) => {
    if (v == null) return undefined
    const s = String(v).trim()
    return s ? s : undefined
  }

  const acceptId = (key, v) => {
    const s = normalize(v)
    if (!s) return undefined
    if (key === 'batteryId' || key === 'battery_id') return s
    if (key === 'id' && /^BAT/i.test(s)) return s
    return undefined
  }

  while (stack.length) {
    const { value, depth } = stack.shift()
    if (!value || typeof value !== 'object') continue
    if (depth > 6) continue
    if (visited.has(value)) continue
    visited.add(value)

    if (!Array.isArray(value)) {
      const direct = acceptId('batteryId', value.batteryId) || acceptId('battery_id', value.battery_id) || acceptId('id', value.id)
      if (direct) return direct
    }

    if (Array.isArray(value)) {
      value.forEach(v => stack.push({ value: v, depth: depth + 1 }))
    } else {
      Object.keys(value).forEach(k => stack.push({ value: value[k], depth: depth + 1 }))
    }
  }

  return undefined
}

const handleBatteryFilter = () => {
  const id = batteryIdTrimmed.value || undefined
  activeTab.value = 'battery'

  if (id) {
    fetchTraceByBatteryId(id)
  } else {
    traceEvents.value = []
  }

  const query = { ...route.query }
  if (id) query.batteryId = id
  else delete query.batteryId
  delete query.txHash

  const currentBatteryId = route.query?.batteryId
  const needReplace = String(currentBatteryId || '') !== String(query.batteryId || '') || route.query?.txHash != null
  if (needReplace) {
    router.replace({ name: route.name, query })
  }
}

const openBatteryTrace = (batteryId) => {
  const id = String(batteryId || '').trim()
  if (!id) return
  batteryQuery.batteryIdInput = id
  handleBatteryFilter()
}

const openTxHash = (txHash) => {
  const hash = String(txHash || '').trim()
  if (!hash) return
  activeTab.value = 'tx'
  router.push({ name: route.name, query: { txHash: hash } })
}

const syncTxTraceFromChainList = () => {
  if (!chainQuery.txHash) {
    txBatteryId.value = ''
    txTraceEvents.value = []
    return Promise.resolve()
  }
  const row = (chainList.value || []).find(r => String(r?.txHash || '') === String(chainQuery.txHash || '')) || chainList.value?.[0]
  const batteryId = extractBatteryIdFromChainRow(row)
  txBatteryId.value = batteryId || ''
  if (!batteryId) {
    txTraceEvents.value = []
    return Promise.resolve()
  }
  txTraceLoading.value = true
  return fetchTraceEventsByBatteryId(batteryId).then(events => {
    txTraceEvents.value = events || []
  }).finally(() => {
    txTraceLoading.value = false
  })
}

onMounted(async () => {
  if (route.query?.batteryId) {
    activeTab.value = 'battery'
    batteryQuery.batteryIdInput = String(route.query.batteryId || '')
  } else if (route.query?.txHash) {
    activeTab.value = 'tx'
    chainQuery.txHash = String(route.query.txHash || '')
  }
  await loadBatteryOptions()
  if (activeTab.value === 'tx') {
    getChainTxList()
  } else {
    const id = resolveBatteryId()
    if (id) {
      fetchTraceByBatteryId(id)
    }
  }
})

watch(
  () => [activeTab.value, batteryIdTrimmed.value],
  ([tab, id]) => {
    if (tab !== 'battery') return
    if (!id) {
      traceEvents.value = []
    }
  }
)

watch(
  () => traceEvents.value,
  async () => {
    await nextTick()
    if (!traceEvents.value?.length) return
    renderBatteryChart()
  },
  { deep: true }
)

watch(
  () => txChartEvents.value,
  async () => {
    await nextTick()
    renderTxChart()
  },
  { deep: true }
)

watch(
  () => activeTab.value,
  async () => {
    await nextTick()
    if (batteryChart && !batteryChart.isDisposed()) batteryChart.resize()
    if (txChart && !txChart.isDisposed()) txChart.resize()
  }
)

const handleChartClick = (chart) => {
  if (!chart) return
  chart.off('click')
  chart.on('click', (p) => {
    if (p?.dataType !== 'node') return
    const txHash = p?.data?.value?.txHash
    if (txHash) openTxHash(txHash)
  })
}

watch(
  () => [batteryChartRef.value, txChartRef.value],
  async ([batteryEl, txEl]) => {
    await nextTick()
    if (!batteryEl) {
      if (batteryChart && !batteryChart.isDisposed()) batteryChart.dispose()
      batteryChart = null
    } else {
      batteryChart = ensureChart(batteryChartRef, batteryChart)
      handleChartClick(batteryChart)
      if (traceEvents.value?.length) renderBatteryChart()
    }
    if (!txEl) {
      if (txChart && !txChart.isDisposed()) txChart.dispose()
      txChart = null
    } else {
      txChart = ensureChart(txChartRef, txChart)
      handleChartClick(txChart)
      if (txChartEvents.value?.length) renderTxChart()
    }
  }
)

onBeforeUnmount(() => {
  if (batteryChart && !batteryChart.isDisposed()) batteryChart.dispose()
  if (txChart && !txChart.isDisposed()) txChart.dispose()
  batteryChart = null
  txChart = null
})

watch(
  () => activeTab.value,
  (tab) => {
    if (tab !== 'tx') return
    if (!chainList.value.length && !chainQuery.txHash) {
      getChainTxList()
    }
  }
)

watch(
  () => route.query?.txHash,
  (txHash) => {
    if (activeTab.value !== 'tx') return
    chainQuery.txHash = txHash ? String(txHash) : undefined
    chainQuery.pageNum = 1
    getChainTxList()
  }
)

watch(
  () => route.query?.batteryId,
  (batteryId) => {
    if (!batteryId) {
      batteryQuery.batteryIdInput = ''
      traceEvents.value = []
      return
    }
    activeTab.value = 'battery'
    batteryQuery.batteryIdInput = String(batteryId || '')
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

.trace-chart {
  width: 100%;
  height: 360px;
}
</style>
