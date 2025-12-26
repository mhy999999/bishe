<template>
  <div class="app-container">
    <div class="filter-container">
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
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog title="新增销售记录" v-model="dialogFormVisible">
      <el-form ref="dataForm" :model="temp" label-width="100px">
        <el-form-item label="电池ID">
          <el-input v-model="temp.batteryId" />
        </el-form-item>
        <el-form-item label="买家姓名">
          <el-input v-model="temp.buyerName" />
        </el-form-item>
        <el-form-item label="售价">
          <el-input v-model="temp.salesPrice" type="number" />
        </el-form-item>
        <el-form-item label="销售员">
          <el-input v-model="temp.salesPerson" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">取消</el-button>
          <el-button type="primary" @click="createData">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getSalesList, saveSales } from '@/api/battery'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'

const list = ref([])
const total = ref(0)
const listLoading = ref(true)
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10
})

const statusMap = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' }
}

const dialogFormVisible = ref(false)
const temp = reactive({
  batteryId: '',
  buyerName: '',
  salesPrice: '',
  salesPerson: ''
})

const getList = () => {
  listLoading.value = true
  getSalesList(listQuery).then(response => {
    const pageData = response.data || response
    if (pageData) {
      list.value = pageData.records
      total.value = pageData.total
    }
    listLoading.value = false
  })
}

const handleCreate = () => {
  temp.batteryId = ''
  temp.buyerName = ''
  temp.salesPrice = ''
  temp.salesPerson = ''
  dialogFormVisible.value = true
}

const createData = () => {
  saveSales(temp).then(() => {
    dialogFormVisible.value = false
    ElMessage.success('创建成功，已提交审核')
    getList()
  })
}

onMounted(() => {
  getList()
})
</script>
