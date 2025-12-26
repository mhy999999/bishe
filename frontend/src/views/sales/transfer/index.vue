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
          <el-tooltip :content="row.txHash" placement="top" effect="light">
            <el-link type="primary" :underline="false">{{ row.txHash ? row.txHash.substring(0, 10) + '...' : '未上链'
            }}</el-link>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog title="电池权属转移" v-model="dialogFormVisible">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="100px"
        style="width: 400px; margin-left:50px;">
        <el-form-item label="电池ID" prop="batteryId">
          <el-input v-model="temp.batteryId" />
        </el-form-item>
        <el-form-item label="原拥有者ID" prop="fromOwner">
          <el-input v-model="temp.fromOwner" />
        </el-form-item>
        <el-form-item label="新拥有者ID" prop="toOwner">
          <el-input v-model="temp.toOwner" />
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
import { getTransferList, saveTransfer } from '@/api/trace'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'

const list = ref(null)
const total = ref(0)
const listLoading = ref(true)
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  batteryId: undefined
})

const temp = reactive({
  batteryId: '',
  fromOwner: '',
  toOwner: '',
  actionType: ''
})

const dialogFormVisible = ref(false)

const rules = {
  batteryId: [{ required: true, message: '电池ID必填', trigger: 'blur' }],
  toOwner: [{ required: true, message: '新拥有者必填', trigger: 'blur' }]
}

const getList = () => {
  listLoading.value = true
  getTransferList(listQuery).then(response => {
    if (response.data) {
      list.value = response.data.records
      total.value = response.data.total
    } else {
      list.value = []
      total.value = 0
    }
    listLoading.value = false
  }).catch(err => {
    console.error(err)
    listLoading.value = false
  })
}

const handleFilter = () => {
  listQuery.pageNum = 1
  getList()
}

const resetTemp = () => {
  temp.batteryId = ''
  temp.fromOwner = ''
  temp.toOwner = ''
  temp.actionType = 'SALE_TO_DEALER'
}

const handleCreate = () => {
  resetTemp()
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
})
</script>
