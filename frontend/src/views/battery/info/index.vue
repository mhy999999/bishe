<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.batteryId" placeholder="电池ID" style="width: 200px;" class="filter-item"
        @keyup.enter="handleFilter" />
      <el-button class="filter-item" type="primary" icon="Search" @click="handleFilter">
        搜索
      </el-button>
      <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="Edit" @click="handleCreate">
        新增电池
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="电池ID" prop="batteryId" align="center" width="180">
        <template #default="{ row }">
          <span>{{ row.batteryId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="生产批次" prop="batchId" align="center" width="100">
        <template #default="{ row }">
          <span>{{ row.batchId || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" prop="typeCode" align="center" />
      <el-table-column label="容量(kWh)" prop="capacity" align="center" />
      <el-table-column label="额定电压(V)" prop="voltage" align="center" />
      <el-table-column label="生产商" prop="manufacturer" align="center" />
      <el-table-column label="生产日期" align="center" width="180">
        <template #default="{ row }">
          <span>{{ row.produceDate }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type || 'info'">
            {{ statusMap[row.status]?.text || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="230" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleUpdate(row)">
            编辑
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog :title="textMap[dialogStatus]" v-model="dialogFormVisible">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="100px"
        style="width: 400px; margin-left:50px;">
        <el-form-item label="电池ID" prop="batteryId">
          <el-input v-model="temp.batteryId" :disabled="dialogStatus === 'update'" />
        </el-form-item>
        <el-form-item label="生产批次" prop="batchId">
          <el-select v-model="temp.batchId" placeholder="选择批次 (可选)" clearable filterable>
            <el-option v-for="item in batchOptions" :key="item.batchId"
              :label="item.batchNo + ' (ID:' + item.batchId + ')'" :value="item.batchId" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="typeCode">
          <el-select v-model="temp.typeCode" placeholder="请选择">
            <el-option label="三元锂" value="NCM" />
            <el-option label="磷酸铁锂" value="LFP" />
          </el-select>
        </el-form-item>
        <el-form-item label="容量" prop="capacity">
          <el-input v-model="temp.capacity" />
        </el-form-item>
        <el-form-item label="额定电压" prop="voltage">
          <el-input v-model="temp.voltage" />
        </el-form-item>
        <el-form-item label="生产商" prop="manufacturer">
          <el-input v-model="temp.manufacturer" />
        </el-form-item>
        <el-form-item label="生产日期" prop="produceDate">
          <el-date-picker v-model="temp.produceDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">
            取消
          </el-button>
          <el-button type="primary" @click="dialogStatus === 'create' ? createData() : updateData()">
            确认
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getBatteryList, saveBattery, updateBattery, deleteBattery, getBatchList } from '@/api/battery'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { BATTERY_STATUS_MAP } from '@/constants/batteryStatus'

const list = ref([])
const total = ref(0)
const listLoading = ref(true)
const batchOptions = ref([])

const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  batteryId: undefined
})

const statusMap = BATTERY_STATUS_MAP

const temp = reactive({
  batteryId: '',
  batchId: undefined,
  typeCode: '',
  capacity: '',
  voltage: '',
  manufacturer: '',
  produceDate: '',
  status: 6
})

const dialogFormVisible = ref(false)
const dialogStatus = ref('')
const textMap = {
  update: '编辑',
  create: '新增'
}

const rules = {
  batteryId: [{ required: true, message: '电池ID必填', trigger: 'blur' }],
  typeCode: [{ required: true, message: '类型必填', trigger: 'change' }]
}

const getList = () => {
  listLoading.value = true
  // 同时获取批次列表，供下拉框使用
  getBatchList({ pageNum: 1, pageSize: 100 }).then(res => {
    const data = res.data || res
    batchOptions.value = data.records || []
  })

  getBatteryList(listQuery).then(response => {
    // 兼容不同的响应结构
    const pageData = response.data || response
    if (pageData && pageData.records) {
      list.value = pageData.records
      total.value = pageData.total
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
  temp.batchId = undefined
  temp.typeCode = 'NCM'
  temp.capacity = ''
  temp.voltage = ''
  temp.manufacturer = ''
  temp.produceDate = new Date().toISOString().split('T')[0]
  temp.status = 6 // 默认待生产
}

const handleCreate = () => {
  resetTemp()
  dialogStatus.value = 'create'
  dialogFormVisible.value = true
}

const createData = () => {
  saveBattery(temp).then(() => {
    list.value.unshift(temp)
    dialogFormVisible.value = false
    ElMessage({
      message: '创建成功',
      type: 'success',
      duration: 2000
    })
    getList()
  })
}

const handleUpdate = (row) => {
  Object.assign(temp, row)
  dialogStatus.value = 'update'
  dialogFormVisible.value = true
}

const updateData = () => {
  updateBattery(temp).then(() => {
    const index = list.value.findIndex(v => v.batteryId === temp.batteryId)
    list.value.splice(index, 1, temp)
    dialogFormVisible.value = false
    ElMessage({
      message: '更新成功',
      type: 'success',
      duration: 2000
    })
    getList()
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteBattery(row.batteryId).then(() => {
      ElMessage({
        type: 'success',
        message: '删除成功!'
      })
      getList()
    })
  })
}

onMounted(() => {
  getList()
})
</script>
