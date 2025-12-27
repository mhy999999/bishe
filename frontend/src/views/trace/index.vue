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
      <el-table-column label="区块高度" prop="blockHeight" align="center" width="100">
        <template #default="{ row }">
          <el-tag type="info">#{{ row.blockHeight }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="调用方法" prop="methodName" align="center" width="150" />
      <el-table-column label="上链参数 (Params)" prop="params" align="center">
        <template #default="{ row }">
          <el-popover placement="top-start" title="详细参数" :width="400" trigger="hover">
            <template #reference>
              <span class="code-snippet">{{ row.params?.substring(0, 50) }}...</span>
            </template>
            <pre>{{ formatJson(row.params) }}</pre>
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

.code-snippet {
  font-family: monospace;
  color: #606266;
}
</style>
