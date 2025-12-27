<template>
  <div class="app-container">
    <div class="filter-container">
      <el-tag type="warning" class="filter-item" style="margin-right: 10px;">待审核列表</el-tag>
      <el-button class="filter-item" type="primary" icon="Refresh" @click="handleFilter">
        刷新
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="电池ID" prop="batteryId" align="center" width="180" />
      <el-table-column label="开路电压(V)" prop="ocv" align="center" />
      <el-table-column label="交流内阻(mΩ)" prop="acr" align="center" />
      <el-table-column label="绝缘电阻(MΩ)" prop="insulationRes" align="center" />
      <el-table-column label="气密性" prop="airTightness" align="center" width="100" />
      <el-table-column label="检测人" prop="inspector" align="center" />
      <el-table-column label="检测时间" prop="checkTime" align="center" width="180">
        <template #default="{ row }">
          <span>{{ row.checkTime?.replace('T', ' ') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleAudit(row)">
            审核
          </el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getQualityList, auditQuality } from '@/api/battery'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage } from 'element-plus'

const list = ref([])
const total = ref(0)
const listLoading = ref(true)
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  status: 0 // 只显示待审核
})

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

const getList = () => {
  listLoading.value = true
  getQualityList(listQuery).then(res => {
    const pageData = res?.data || res
    list.value = pageData?.records || []
    total.value = pageData?.total || 0
    listLoading.value = false
  }).catch(err => {
    console.error(err)
    list.value = []
    total.value = 0
    listLoading.value = false
  })
}

const handleFilter = () => {
  listQuery.pageNum = 1
  getList()
}

onMounted(() => {
  getList()
})
</script>
