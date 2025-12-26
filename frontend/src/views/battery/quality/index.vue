<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.batteryId" placeholder="电池ID" style="width: 200px;" class="filter-item"
        @keyup.enter="handleFilter" />
      <el-button class="filter-item" type="primary" icon="Search" @click="handleFilter">
        搜索
      </el-button>
      <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="Edit" @click="handleCreate">
        录入质检
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="电池ID" prop="batteryId" align="center" width="180" />
      <el-table-column label="检测结果" prop="checkResult" align="center">
        <template #default="{ row }">
          <el-tag :type="row.checkResult === 'PASS' ? 'success' : 'danger'">{{ row.checkResult }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="电压(V)" prop="voltage" align="center" />
      <el-table-column label="内阻(mΩ)" prop="internalResistance" align="center" />
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
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button v-if="row.status === 0 || row.status === undefined" type="primary" size="small"
            @click="handleAudit(row)">
            审核
          </el-button>
          <span v-else>{{ row.auditOpinion || (row.status === 1 ? '审核通过' : '审核驳回') }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog title="质检审核" v-model="dialogAuditVisible" width="400px">
      <el-form :model="auditTemp" label-width="80px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditTemp.status">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditTemp.auditOpinion" type="textarea" placeholder="请输入审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogAuditVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAudit">提交</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="录入质检记录" v-model="dialogFormVisible">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="100px"
        style="width: 400px; margin-left:50px;">
        <el-form-item label="电池ID" prop="batteryId">
          <el-input v-model="temp.batteryId" />
        </el-form-item>
        <el-form-item label="检测结果" prop="checkResult">
          <el-select v-model="temp.checkResult" placeholder="请选择">
            <el-option label="通过" value="PASS" />
            <el-option label="不通过" value="FAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="电压(V)" prop="voltage">
          <el-input v-model="temp.voltage" />
        </el-form-item>
        <el-form-item label="内阻(mΩ)" prop="internalResistance">
          <el-input v-model="temp.internalResistance" />
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
import { ref, reactive, onMounted } from 'vue'
import { getQualityList, saveQuality, auditQuality } from '@/api/battery'
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

const statusMap = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' }
}

const dialogAuditVisible = ref(false)
const auditTemp = reactive({
  id: undefined,
  status: 1,
  auditOpinion: ''
})

const handleAudit = (row) => {
  auditTemp.id = row.id
  auditTemp.status = 1
  auditTemp.auditOpinion = ''
  dialogAuditVisible.value = true
}

const submitAudit = () => {
  auditQuality(auditTemp).then(() => {
    dialogAuditVisible.value = false
    ElMessage.success('审核完成')
    getList()
  })
}

const temp = reactive({
  batteryId: '',
  checkResult: 'PASS',
  voltage: '',
  internalResistance: '',
  inspector: '',
  checkTime: ''
})

const dialogFormVisible = ref(false)

const rules = {
  batteryId: [{ required: true, message: '电池ID必填', trigger: 'blur' }],
  checkResult: [{ required: true, message: '结果必填', trigger: 'change' }]
}

const getList = () => {
  listLoading.value = true
  getQualityList(listQuery).then(response => {
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
  temp.checkResult = 'PASS'
  temp.voltage = ''
  temp.internalResistance = ''
  temp.inspector = ''
  temp.checkTime = new Date().toISOString()
}

const handleCreate = () => {
  resetTemp()
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
    getList()
  })
}

onMounted(() => {
  getList()
})
</script>
