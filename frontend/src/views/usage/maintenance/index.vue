<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.batteryId" placeholder="电池ID" style="width: 200px;" class="filter-item"
        @keyup.enter="handleFilter" />
      <el-button class="filter-item" type="primary" icon="Search" @click="handleFilter">
        搜索
      </el-button>
      <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="Tools" @click="handleCreate">
        录入维修
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="电池ID" prop="batteryId" align="center" width="180" />
      <el-table-column label="维修类型" prop="maintenanceType" align="center" />
      <el-table-column label="维修描述" prop="description" align="center" />
      <el-table-column label="维修成本" prop="cost" align="center" />
      <el-table-column label="维修时间" prop="createTime" align="center" width="180">
        <template #default="{ row }">
          <span>{{ row.createTime?.replace('T', ' ') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="维修凭证(TxHash)" align="center" width="200">
        <template #default="{ row }">
          <el-tooltip :content="row.txHash" placement="top" effect="light">
            <el-link type="primary" underline="never">{{ row.txHash ? row.txHash.substring(0, 10) + '...' : '未上链'
              }}</el-link>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog title="录入维修记录" v-model="dialogFormVisible">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="100px"
        style="width: 400px; margin-left:50px;">
        <el-form-item label="电池ID" prop="batteryId">
          <el-input v-model="temp.batteryId" />
        </el-form-item>
        <el-form-item label="维修类型" prop="maintenanceType">
          <el-select v-model="temp.maintenanceType" placeholder="请选择">
            <el-option label="定期保养" value="REGULAR" />
            <el-option label="故障维修" value="REPAIR" />
            <el-option label="配件更换" value="REPLACE" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="temp.description" />
        </el-form-item>
        <el-form-item label="成本(元)" prop="cost">
          <el-input v-model="temp.cost" />
        </el-form-item>
      </el-form>
      <div style="margin-left: 50px; color: #909399; font-size: 12px; margin-bottom: 20px;">
        <el-icon>
          <InfoFilled />
        </el-icon> 关键维修数据将自动上链存证。
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">
            取消
          </el-button>
          <el-button type="primary" @click="createData">
            确认
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMaintenanceList, saveMaintenance } from '@/api/trace'
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
  maintenanceType: 'REGULAR',
  description: '',
  cost: ''
})

const dialogFormVisible = ref(false)

const rules = {
  batteryId: [{ required: true, message: '电池ID必填', trigger: 'blur' }]
}

const getList = () => {
  listLoading.value = true
  getMaintenanceList(listQuery).then(response => {
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
  temp.maintenanceType = 'REGULAR'
  temp.description = ''
  temp.cost = ''
}

const handleCreate = () => {
  resetTemp()
  dialogFormVisible.value = true
}

const createData = () => {
  saveMaintenance(temp).then(() => {
    dialogFormVisible.value = false
    ElMessage({
      message: '维修记录已上链',
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
