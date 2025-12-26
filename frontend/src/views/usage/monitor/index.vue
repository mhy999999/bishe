<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.batteryId" placeholder="电池ID" style="width: 200px;" class="filter-item"
        @keyup.enter="handleFilter" />
      <el-button class="filter-item" type="primary" icon="Search" @click="handleFilter">
        搜索
      </el-button>
      <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="Monitor" @click="handleCreate">
        模拟数据上报
      </el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="健康状态 (SOH/SOC)" name="health">
        <el-table v-loading="listLoading" :data="healthList" border fit highlight-current-row style="width: 100%;">
          <el-table-column label="电池ID" prop="batteryId" align="center" width="180" />
          <el-table-column label="SOC(%)" prop="soc" align="center">
            <template #default="{ row }">
              <el-progress :percentage="Number(row.soc)" :color="customColors" />
            </template>
          </el-table-column>
          <el-table-column label="SOH(%)" prop="soh" align="center">
            <template #default="{ row }">
              <el-progress :percentage="Number(row.soh)" status="success" />
            </template>
          </el-table-column>
          <el-table-column label="最高温度(°C)" prop="maxTemp" align="center">
            <template #default="{ row }">
              <span :style="{ color: row.maxTemp > 60 ? 'red' : 'inherit' }">{{ row.maxTemp }}</span>
            </template>
          </el-table-column>
          <el-table-column label="最低温度(°C)" prop="minTemp" align="center" />
          <el-table-column label="上报时间" prop="reportTime" align="center" width="180">
            <template #default="{ row }">
              <span>{{ row.reportTime?.replace('T', ' ') }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="故障报警" name="alarm">
        <el-table v-loading="alarmLoading" :data="alarmList" border fit highlight-current-row style="width: 100%;">
          <el-table-column label="电池ID" prop="batteryId" align="center" width="180" />
          <el-table-column label="报警级别" prop="alarmLevel" align="center">
            <template #default="{ row }">
              <el-tag :type="row.alarmLevel === 'High' ? 'danger' : 'warning'">{{ row.alarmLevel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="报警内容" prop="alarmContent" align="center" />
          <el-table-column label="状态" prop="status" align="center">
            <template #default="{ row }">
              {{ row.status === 0 ? '未处理' : '已处理' }}
            </template>
          </el-table-column>
          <el-table-column label="报警时间" prop="createTime" align="center" width="180">
            <template #default="{ row }">
              <span>{{ row.createTime?.replace('T', ' ') }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog title="模拟BMS数据上报" v-model="dialogFormVisible">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="100px"
        style="width: 400px; margin-left:50px;">
        <el-form-item label="电池ID" prop="batteryId">
          <el-input v-model="temp.batteryId" />
        </el-form-item>
        <el-form-item label="SOC (%)" prop="soc">
          <el-input-number v-model="temp.soc" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="SOH (%)" prop="soh">
          <el-input-number v-model="temp.soh" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="循环次数" prop="cycleCount">
          <el-input-number v-model="temp.cycleCount" />
        </el-form-item>
        <el-form-item label="最高温度" prop="maxTemp">
          <el-input-number v-model="temp.maxTemp" :precision="1" />
        </el-form-item>
        <el-form-item label="最低温度" prop="minTemp">
          <el-input-number v-model="temp.minTemp" :precision="1" />
        </el-form-item>
      </el-form>
      <div style="margin-left: 50px; color: #909399; font-size: 12px; margin-bottom: 20px;">
        <el-icon>
          <InfoFilled />
        </el-icon> 若温度>60°C或SOC&lt;20%，将自动触发报警。
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">
            取消
          </el-button>
          <el-button type="primary" @click="createData">
            模拟上报
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { getHealthList, getAlarmList, saveHealth } from '@/api/battery'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'

const activeTab = ref('health')
const healthList = ref(null)
const alarmList = ref(null)
const listLoading = ref(false)
const alarmLoading = ref(false)

const listQuery = reactive({
  pageNum: 1,
  pageSize: 20,
  batteryId: undefined
})

const temp = reactive({
  batteryId: '',
  soc: 80,
  soh: 98,
  cycleCount: 50,
  maxTemp: 25.5,
  minTemp: 22.0
})

const dialogFormVisible = ref(false)

const rules = {
  batteryId: [{ required: true, message: '电池ID必填', trigger: 'blur' }]
}

const customColors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#5cb87a', percentage: 100 },
]

const getHealth = () => {
  listLoading.value = true
  getHealthList(listQuery).then(response => {
    if (response.data) {
      healthList.value = response.data.records
    } else {
      healthList.value = []
    }
    listLoading.value = false
  }).catch(err => {
    console.error(err)
    listLoading.value = false
  })
}

const getAlarm = () => {
  alarmLoading.value = true
  getAlarmList(listQuery).then(response => {
    if (response.data) {
      alarmList.value = response.data.records
    } else {
      alarmList.value = []
    }
    alarmLoading.value = false
  }).catch(err => {
    console.error(err)
    alarmLoading.value = false
  })
}

const handleFilter = () => {
  if (activeTab.value === 'health') {
    getHealth()
  } else {
    getAlarm()
  }
}

const handleCreate = () => {
  dialogFormVisible.value = true
}

const createData = () => {
  saveHealth(temp).then(() => {
    dialogFormVisible.value = false
    ElMessage({
      message: '数据上报成功',
      type: 'success',
      duration: 2000
    })
    getHealth()
    // 如果有报警可能需要刷新报警列表
    setTimeout(() => {
      getAlarm()
    }, 500)
  })
}

watch(activeTab, (val) => {
  if (val === 'health') {
    getHealth()
  } else {
    getAlarm()
  }
})

onMounted(() => {
  getHealth()
})
</script>
