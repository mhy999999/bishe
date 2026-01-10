<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.batteryId" class="filter-item" style="width: 180px;" placeholder="电池ID" />
      <el-input v-model="listQuery.recycleNo" class="filter-item" style="width: 180px;" placeholder="回收编号" />
      <el-select v-model="listQuery.status" class="filter-item" style="width: 140px;" placeholder="状态" clearable>
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
        <el-option label="已完成" :value="3" />
      </el-select>
      <el-button class="filter-item" type="primary" icon="Search" @click="handleFilter">查询</el-button>
      <el-button class="filter-item" icon="Refresh" @click="resetFilter">重置</el-button>
      <el-button v-if="canApply" class="filter-item" type="success" icon="Plus" @click="openApplyDialog">
        发起回收申请
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border fit highlight-current-row style="width: 100%;">
      <el-table-column label="ID" prop="appraisalId" align="center" width="80" />
      <el-table-column label="电池ID" prop="batteryId" align="center" min-width="160" />
      <el-table-column label="回收编号" prop="recycleNo" align="center" min-width="160" />
      <el-table-column label="状态" align="center" width="110">
        <template #default="{ row }">
          <el-tag :type="statusMap[row?.status]?.type || 'info'">
            {{ statusMap[row?.status]?.text || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请人" prop="applyUser" align="center" width="120" />
      <el-table-column label="申请时间" align="center" width="170">
        <template #default="{ row }">
          <span>{{ formatDateTime(row?.applyTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预估价" align="center" width="120">
        <template #default="{ row }">
          <span>{{ formatMoney(row?.preliminaryValue) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最终价" align="center" width="120">
        <template #default="{ row }">
          <span>{{ formatMoney(row?.finalValue) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openDetail(row)">详情</el-button>
          <el-button v-if="canAudit && row?.status === 0" type="warning" size="small" @click="openAuditDialog(row)">
            审核
          </el-button>
          <el-button v-if="canAudit && row?.status === 1" type="success" size="small" @click="openValuationDialog(row)">
            估值
          </el-button>
          <el-button v-if="row?.appraisalId" type="info" size="small" @click="openReceiptDialog(row)">
            单据
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize"
      @pagination="getList" />

    <el-dialog title="发起回收申请" v-model="applyDialogVisible" width="560px">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="100px">
        <el-form-item label="电池ID" prop="batteryId">
          <el-input v-model="applyForm.batteryId" placeholder="请输入电池ID" />
        </el-form-item>
        <el-form-item label="外观描述" prop="appearance">
          <el-input v-model="applyForm.appearance" type="textarea" :rows="2" placeholder="请输入外观描述" />
        </el-form-item>
        <el-form-item label="处理建议" prop="suggestion">
          <el-input v-model="applyForm.suggestion" type="textarea" :rows="2" placeholder="请输入处理建议" />
        </el-form-item>
        <el-form-item label="回收原因" prop="reason">
          <el-input v-model="applyForm.reason" type="textarea" :rows="5" placeholder="请输入回收原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="applyDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="applySubmitting" @click="submitApply">提交</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="回收审核" v-model="auditDialogVisible" width="460px">
      <el-form :model="auditForm" label-width="90px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.status">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditForm.auditOpinion" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="auditDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="auditSubmitting" :disabled="auditSubmitting"
            @click="submitAudit">提交</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="回收估值" v-model="valuationDialogVisible" width="760px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="评估ID">{{ activeRow?.appraisalId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="电池ID">{{ activeRow?.batteryId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[activeRow?.status]?.type || 'info'">
            {{ statusMap[activeRow?.status]?.text || '-' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="回收编号">{{ activeRow?.recycleNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预估价">{{ formatMoney(activeRow?.preliminaryValue) }}</el-descriptions-item>
        <el-descriptions-item label="最终价">{{ formatMoney(activeRow?.finalValue) }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top: 14px;">
        <el-card shadow="never" style="margin-bottom: 12px;">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between;">
              <span>外观照片（至少3张）</span>
              <el-upload :http-request="handlePhotoUpload" multiple :limit="10" :on-exceed="handlePhotoExceed"
                :show-file-list="false">
                <el-button type="primary" size="small">上传照片</el-button>
              </el-upload>
            </div>
          </template>

          <div v-if="photoPreviewUrls.length > 0" class="recycling-photo-grid">
            <el-image v-for="(u, idx) in photoPreviewUrls" :key="u + '_' + idx" class="recycling-photo-item" :src="u"
              :preview-src-list="photoPreviewUrls" :initial-index="idx" fit="cover" />
          </div>
          <el-empty v-else description="暂无照片" />
        </el-card>

        <el-card shadow="never" style="margin-bottom: 12px;">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between;">
              <span>性能检测报告（必传）</span>
              <el-upload :http-request="handleReportUpload" :limit="1" :on-exceed="handleReportExceed"
                :show-file-list="false">
                <el-button type="primary" size="small">上传报告</el-button>
              </el-upload>
            </div>
          </template>

          <div v-if="activeRow?.performanceReportUrl">
            <el-link type="primary" underline="never" @click="openFileInNewTab(activeRow.performanceReportUrl)">
              打开报告
            </el-link>
          </div>
          <el-empty v-else description="暂无报告" />
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between;">
              <span>估值与确认</span>
            </div>
          </template>

          <el-form :inline="true" :model="valuationForm" label-width="90px">
            <el-form-item label="预估价">
              <el-input v-model="valuationForm.preliminaryValue" style="width: 200px;" readonly>
                <template #append>元</template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button :loading="autoCalcLoading" @click="autoFillPreliminary">自动计算</el-button>
            </el-form-item>
          </el-form>

          <el-form :inline="true" :model="confirmForm" label-width="90px" style="margin-top: 6px;">
            <el-form-item label="最终回收价">
              <el-input v-model="confirmForm.finalValue" style="width: 200px;" placeholder="请输入金额">
                <template #append>元</template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="success" :loading="confirmSubmitting" @click="confirmPrice">确认价格并完成</el-button>
            </el-form-item>
          </el-form>

        </el-card>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="valuationDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="回收单据" v-model="receiptDialogVisible" width="720px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="回收编号">{{ receiptData?.recycleNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="回收时间">{{ formatDateTime(receiptData?.recycleTime) }}</el-descriptions-item>
        <el-descriptions-item label="预估价">{{ formatMoney(receiptData?.preliminaryValue) }}</el-descriptions-item>
        <el-descriptions-item label="最终价">{{ formatMoney(receiptData?.finalValue) }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ receiptData?.auditor || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ formatDateTime(receiptData?.auditTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2">
          <span style="white-space: pre-wrap;">{{ receiptData?.auditOpinion || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="上链哈希" :span="2">
          <template v-if="receiptData?.receiptHash">
            <el-tooltip :content="receiptData.receiptHash" placement="top" effect="light">
              <el-link type="primary" underline="never" @click="openTxHash(receiptData.receiptHash)">
                {{ getTxHashShort(receiptData.receiptHash) }}
              </el-link>
            </el-tooltip>
          </template>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>

      <div style="margin-top: 12px;">
        <el-card shadow="never">
          <template #header>
            <span>电池信息</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="电池ID">{{ receiptData?.batteryInfo?.batteryId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ receiptData?.batteryInfo?.status ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="容量">{{ receiptData?.batteryInfo?.capacity ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="电压">{{ receiptData?.batteryInfo?.voltage ?? '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </div>

      <div v-if="receiptData?.receiptHash" v-loading="receiptChainLoading" style="margin-top: 12px;">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between;">
              <span>区块链存证</span>
              <el-button size="small" text type="primary" @click="openTxHash(receiptData.receiptHash)">进入溯源</el-button>
            </div>
          </template>
          <el-descriptions v-if="receiptChainRow" :column="2" border size="small">
            <el-descriptions-item label="TxHash" :span="2">
              <el-tooltip :content="receiptChainRow.txHash" placement="top" effect="light">
                <el-link type="primary" underline="never" @click="openTxHash(receiptChainRow.txHash)">
                  {{ getTxHashShort(receiptChainRow.txHash) }}
                </el-link>
              </el-tooltip>
            </el-descriptions-item>
            <el-descriptions-item label="区块高度">{{ receiptChainRow.blockHeight != null ? ('#' +
              receiptChainRow.blockHeight)
              : '-' }}</el-descriptions-item>
            <el-descriptions-item label="链ID">{{ receiptChainRow.chainId ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="方法">{{ receiptChainRow.methodName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="receiptChainRow.status === 1 ? 'success' : 'danger'">
                {{ receiptChainRow.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="合约地址" :span="2">{{ receiptChainRow.contractAddr || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="上链时间" :span="2">{{ formatDateTime(receiptChainRow.createTime)
            }}</el-descriptions-item>
            <el-descriptions-item v-if="receiptChainRow.errorMessage" label="错误信息" :span="2">
              <span style="white-space: pre-wrap;">{{ receiptChainRow.errorMessage }}</span>
            </el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="暂无交易详情" />
        </el-card>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="receiptDialogVisible = false">关闭</el-button>
          <el-button v-if="receiptData?.receiptHash" type="primary" @click="openTxHash(receiptData.receiptHash)">
            查看上链交易
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="回收详情" size="640px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="评估ID">{{ activeRow?.appraisalId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="电池ID">{{ activeRow?.batteryId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[activeRow?.status]?.type || 'info'">
            {{ statusMap[activeRow?.status]?.text || '-' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="回收编号">{{ activeRow?.recycleNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ activeRow?.applyUser || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatDateTime(activeRow?.applyTime) }}</el-descriptions-item>
        <el-descriptions-item label="外观描述" :span="2">
          <span style="white-space: pre-wrap;">{{ activeRow?.appearance || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="处理建议" :span="2">
          <span style="white-space: pre-wrap;">{{ activeRow?.suggestion || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="回收原因" :span="2">
          <span style="white-space: pre-wrap;">{{ activeRow?.applyReason || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="预估价">{{ formatMoney(activeRow?.preliminaryValue) }}</el-descriptions-item>
        <el-descriptions-item label="最终价">{{ formatMoney(activeRow?.finalValue) }}</el-descriptions-item>
        <el-descriptions-item label="申请哈希" :span="2">
          <template v-if="activeRow?.dataHash">
            <el-tooltip :content="activeRow.dataHash" placement="top" effect="light">
              <el-link type="primary" underline="never" @click="openTxHash(activeRow.dataHash)">
                {{ getTxHashShort(activeRow.dataHash) }}
              </el-link>
            </el-tooltip>
          </template>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="估值依据" :span="2">
          <div class="valuation-basis">
            <div v-if="!detailValuationBasisText" class="valuation-basis__empty">-</div>
            <template v-else>
              <div class="valuation-basis__meta">
                <el-tag size="small" :type="detailValuationBasisInfo.kind === 'legacy' ? 'warning' : 'info'"
                  effect="plain">
                  {{ detailValuationBasisInfo.kind === 'legacy' ? '历史估值记录' : (detailValuationBasisInfo.kind ===
                    'manual_auto' ? '系统建议' : '人工估值') }}
                </el-tag>
                <el-button text size="small" @click="detailBasisExpanded = !detailBasisExpanded">
                  {{ detailBasisExpanded ? '收起详情' : '展开详情' }}
                </el-button>
              </div>

              <el-alert v-if="detailValuationBasisInfo.kind === 'legacy'" type="warning" show-icon :closable="false"
                title="该条估值依据为旧格式，已做兼容展示" />

              <div v-if="detailValuationBasisInfo.metrics.length" class="valuation-basis__features">
                <div class="valuation-basis__label">关键数值</div>
                <div class="valuation-basis__tags">
                  <el-tag v-for="m in detailValuationBasisInfo.metrics" :key="m.key" size="small" type="info"
                    effect="plain">
                    {{ m.label }}：{{ m.value }}
                  </el-tag>
                </div>
                <div v-if="detailValuationBasisInfo.formula" class="valuation-basis__formula">
                  公式：{{ detailValuationBasisInfo.formula }}
                </div>
              </div>

              <div v-if="detailBasisExpanded" class="valuation-basis__raw">
                <el-descriptions v-if="detailValuationBasisInfo.pairsPreview.length" :column="2" border size="small">
                  <el-descriptions-item v-for="p in detailValuationBasisInfo.pairsPreview" :key="p.key"
                    :label="p.label">
                    {{ p.value }}
                  </el-descriptions-item>
                </el-descriptions>
                <div v-if="detailValuationBasisInfo.morePairsCount" class="valuation-basis__more">
                  还有 {{ detailValuationBasisInfo.morePairsCount }} 项未展示
                </div>
                <div class="valuation-basis__raw-actions">
                  <el-button text size="small" @click="basisRawVisible.detail = !basisRawVisible.detail">
                    {{ basisRawVisible.detail ? '隐藏原始文本' : '查看原始文本' }}
                  </el-button>
                  <el-button text size="small" @click="copyText(detailValuationBasisText)">复制原文</el-button>
                </div>
                <div v-if="basisRawVisible.detail" class="valuation-basis__rawbox">
                  <pre class="valuation-basis__pre">{{ detailValuationBasisText }}</pre>
                </div>
              </div>
            </template>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="报告">
          <el-link v-if="activeRow?.performanceReportUrl" type="primary" underline="never"
            @click="openFileInNewTab(activeRow.performanceReportUrl)">
            打开
          </el-link>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="照片数量">{{ parseStringList(activeRow?.photoUrls).length }}</el-descriptions-item>
        <el-descriptions-item label="单据哈希" :span="2">
          <span>{{ activeRow?.receiptHash || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="detailTxHashToShow" v-loading="detailChainLoading" style="margin-top: 12px;">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between;">
              <span>区块链存证</span>
              <el-button size="small" text type="primary" @click="openTxHash(detailTxHashToShow)">进入溯源</el-button>
            </div>
          </template>
          <el-descriptions v-if="detailChainRow" :column="2" border size="small">
            <el-descriptions-item label="TxHash" :span="2">
              <el-tooltip :content="detailChainRow.txHash" placement="top" effect="light">
                <el-link type="primary" underline="never" @click="openTxHash(detailChainRow.txHash)">
                  {{ getTxHashShort(detailChainRow.txHash) }}
                </el-link>
              </el-tooltip>
            </el-descriptions-item>
            <el-descriptions-item label="区块高度">{{ detailChainRow.blockHeight != null ? ('#' +
              detailChainRow.blockHeight) :
              '-' }}</el-descriptions-item>
            <el-descriptions-item label="链ID">{{ detailChainRow.chainId ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="方法">{{ detailChainRow.methodName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="detailChainRow.status === 1 ? 'success' : 'danger'">
                {{ detailChainRow.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="合约地址" :span="2">{{ detailChainRow.contractAddr || '-' }}</el-descriptions-item>
            <el-descriptions-item label="上链时间" :span="2">{{ formatDateTime(detailChainRow.createTime)
            }}</el-descriptions-item>
            <el-descriptions-item v-if="detailChainRow.errorMessage" label="错误信息" :span="2">
              <span style="white-space: pre-wrap;">{{ detailChainRow.errorMessage }}</span>
            </el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="暂无交易详情" />
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Pagination from '@/components/Pagination/index.vue'
import { useUserStore } from '@/store/user'
import { applyRecycling, auditRecycling, calcRecyclingValuation, confirmRecyclingPrice, getChainList, getMaintenanceList, getRecyclingList, getRecyclingReceipt, getTransferList, uploadRecyclingPhoto, uploadRecyclingReport } from '@/api/trace'
import { getBatteryList } from '@/api/battery'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const roles = computed(() => userStore.roles || [])
const canApply = computed(() => roles.value.includes('admin') || roles.value.includes('maintainer') || roles.value.includes('maintenance'))
const canAudit = computed(() => roles.value.includes('admin') || roles.value.includes('recycler'))

const statusMap = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' },
  3: { text: '已完成', type: 'info' }
}

const list = ref([])
const total = ref(0)
const listLoading = ref(false)
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  batteryId: '',
  recycleNo: '',
  status: undefined
})

const getList = () => {
  listLoading.value = true
  const params = {
    pageNum: listQuery.pageNum,
    pageSize: listQuery.pageSize,
    batteryId: String(listQuery.batteryId || '').trim() || undefined,
    recycleNo: String(listQuery.recycleNo || '').trim() || undefined,
    status: listQuery.status
  }
  getRecyclingList(params).then((response) => {
    const pageData = response?.data || response
    list.value = pageData?.records || []
    total.value = pageData?.total || 0
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

const resetFilter = () => {
  listQuery.batteryId = ''
  listQuery.recycleNo = ''
  listQuery.status = undefined
  listQuery.pageNum = 1
  getList()
}

const applyDialogVisible = ref(false)
const applySubmitting = ref(false)
const applyFormRef = ref()
const applyForm = reactive({
  batteryId: '',
  reason: '',
  appearance: '',
  suggestion: ''
})

const applyRules = {
  batteryId: [{ required: true, message: '电池ID必填', trigger: 'blur' }],
  appearance: [{ required: true, message: '外观描述必填', trigger: 'blur' }],
  suggestion: [{ required: true, message: '处理建议必填', trigger: 'blur' }],
  reason: [{ required: true, message: '回收原因必填', trigger: 'blur' }]
}

const openApplyDialog = () => {
  applyForm.batteryId = String(route.query?.batteryId || '').trim()
  applyForm.reason = ''
  applyForm.appearance = ''
  applyForm.suggestion = ''
  applyDialogVisible.value = true
}

const submitApply = () => {
  if (!applyFormRef.value) return
  applyFormRef.value.validate((valid) => {
    if (!valid) return
    applySubmitting.value = true
    const payload = {
      batteryId: String(applyForm.batteryId || '').trim(),
      reason: String(applyForm.reason || '').trim(),
      appearance: String(applyForm.appearance || '').trim(),
      suggestion: String(applyForm.suggestion || '').trim()
    }
    applyRecycling(payload).then(() => {
      applyDialogVisible.value = false
      ElMessage.success('回收申请已提交')
      getList()
    }).finally(() => {
      applySubmitting.value = false
    })
  })
}

const auditDialogVisible = ref(false)
const auditSubmitting = ref(false)
const auditForm = reactive({
  appraisalId: undefined,
  status: 1,
  auditOpinion: ''
})

const openAuditDialog = (row) => {
  auditForm.appraisalId = row?.appraisalId
  auditForm.status = 1
  auditForm.auditOpinion = ''
  auditDialogVisible.value = true
}

const submitAudit = () => {
  if (!auditForm.appraisalId) return
  if (auditSubmitting.value) return
  auditSubmitting.value = true
  auditRecycling({
    appraisalId: auditForm.appraisalId,
    status: auditForm.status,
    auditOpinion: String(auditForm.auditOpinion || '').trim() || undefined
  }).then(() => {
    auditDialogVisible.value = false
    ElMessage.success('审核完成')
    getList()
  }).finally(() => {
    auditSubmitting.value = false
  })
}

const detailVisible = ref(false)
const activeRow = ref(null)
const openDetail = (row) => {
  activeRow.value = row || null
  detailVisible.value = true
}

const valuationBasisExpanded = ref(false)
const receiptBasisExpanded = ref(false)
const detailBasisExpanded = ref(false)
const basisRawVisible = reactive({
  active: false,
  receipt: false,
  detail: false
})

const valuationDialogVisible = ref(false)
const receiptDialogVisible = ref(false)
const receiptData = ref(null)
const valuationSubmitting = ref(false)
const autoCalcLoading = ref(false)
const valuationForm = reactive({
  preliminaryValue: '',
  valuationBasis: ''
})
const confirmSubmitting = ref(false)
const confirmForm = reactive({
  finalValue: ''
})

const receiptChainLoading = ref(false)
const receiptChainRow = ref(null)
const detailChainLoading = ref(false)
const detailChainRow = ref(null)

const getTxHashShort = (val) => {
  const t = String(val || '').trim()
  if (!t) return '-'
  if (t.length <= 18) return t
  return `${t.slice(0, 10)}...${t.slice(-6)}`
}

const loadChainTx = (txHash, targetRow, targetLoading) => {
  const hash = String(txHash || '').trim()
  if (!hash) {
    targetRow.value = null
    return
  }
  targetLoading.value = true
  getChainList({ pageNum: 1, pageSize: 1, txHash: hash }).then((res) => {
    const page = res?.data || res
    const row = Array.isArray(page?.records) ? page.records[0] : null
    targetRow.value = row || null
  }).catch(() => {
    targetRow.value = null
  }).finally(() => {
    targetLoading.value = false
  })
}

const parseStringList = (raw) => {
  if (!raw) return []
  if (Array.isArray(raw)) return raw.filter(Boolean).map(String)
  if (typeof raw !== 'string') return []
  const text = raw.trim()
  if (!text) return []
  try {
    const parsed = JSON.parse(text)
    if (Array.isArray(parsed)) return parsed.filter(Boolean).map(String)
  } catch (e) {
  }
  if (text.includes(',')) return text.split(',').map(s => s.trim()).filter(Boolean)
  return [text]
}

const apiBase = computed(() => (import.meta.env.VITE_APP_BASE_API || '/api').replace(/\/$/, ''))
const resolveFileUrl = (rawUrl) => {
  const url = String(rawUrl || '').trim()
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/api/')) return url
  if (url.startsWith('/files/')) return `${apiBase.value}${url}`
  if (url.startsWith('files/')) return `${apiBase.value}/${url}`
  if (url.startsWith('/')) return url
  return `${apiBase.value}/${url}`
}

const photoPreviewUrls = computed(() => parseStringList(activeRow.value?.photoUrls).map(resolveFileUrl).filter(Boolean))

const openValuationDialog = (row) => {
  activeRow.value = row || null
  confirmForm.finalValue = row?.finalValue != null ? String(row.finalValue) : ''
  valuationForm.preliminaryValue = row?.preliminaryValue != null ? String(row.preliminaryValue) : ''
  valuationForm.valuationBasis = String(row?.valuationBasis || '').trim()
  valuationDialogVisible.value = true
}

watch(valuationDialogVisible, (val) => {
  if (!val) {
    confirmForm.finalValue = ''
    valuationForm.preliminaryValue = ''
    valuationForm.valuationBasis = ''
    valuationBasisExpanded.value = false
    basisRawVisible.active = false
  }
})

watch(receiptDialogVisible, (val) => {
  if (!val) {
    receiptBasisExpanded.value = false
    basisRawVisible.receipt = false
  }
})

watch(detailVisible, (val) => {
  if (!val) {
    detailBasisExpanded.value = false
    basisRawVisible.detail = false
  }
})

function normalizeValuationBasis(raw) {
  const text = String(raw || '').trim()
  return text
}

function parseBasisPairs(text) {
  const input = String(text || '')
  const out = []
  const re = /(^|[;,])\s*([a-zA-Z_][a-zA-Z0-9_\.]*)\s*=\s*([^,;]+)\s*/g
  let m
  while ((m = re.exec(input)) !== null) {
    const key = String(m[2] || '').trim()
    const value = String(m[3] || '').trim()
    if (!key || !value) continue
    out.push({ key, value })
  }
  return out
}

function normalizeBasisKey(rawKey) {
  const k = String(rawKey || '').trim()
  if (!k) return ''
  if (k.startsWith('chain.')) return k
  if (k.startsWith('chain') && k.length > 5) {
    const rest = k.slice(5)
    const normalized = rest.charAt(0).toLowerCase() + rest.slice(1)
    return `chain.${normalized}`
  }
  return k
}

function labelBasisKey(key) {
  const k0 = String(key || '').trim()
  const k = normalizeBasisKey(k0)
  const map = {
    capacity: '容量(kWh)',
    voltage: '电压(V)',
    capacityVoltage: '容量×电压',
    maintenanceCount: '维修次数',
    transferCount: '流转次数',
    batteryStatus: '状态码',
    unitPricePerKwh: '单价(元/kWh)',
    statusFactor: '状态系数',
    maintenanceFactor: '维修系数',
    transferFactor: '流转系数',
    suggestedPreliminary: '预估价(建议)',
    'chain.transferCount': '流转次数(链)',
    'chain.maintenanceCount': '维修次数(链)',
    'chain.qualityInspectionCount': '质检次数(链)',
    'chain.salesCount': '销售次数(链)',
    'chain.lastTransferDays': '距上次流转(天)',
    'chain.lastMaintenanceDays': '距上次维修(天)',
    'chain.lastQualityInspectionDays': '距上次质检(天)',
    'chain.latestOcv': '最新开路电压(OCV)',
    'chain.latestAcr': '最新内阻(ACR)',
    'chain.latestInsulationRes': '最新绝缘电阻',
    'chain.latestAirTightnessOk': '最新气密性(合格)'
  }
  if (map[k]) return map[k]
  return k0 || k
}

function extractKeyValue(text, key) {
  if (!text || !key) return ''
  const re = new RegExp(`(?:^|[;,])\\s*${key}\\s*=\\s*([^,;]+)`, 'i')
  const m = text.match(re)
  if (!m || !m[1]) return ''
  return String(m[1]).trim()
}

function extractFormula(text) {
  if (!text) return ''
  const parts = String(text)
    .split(';')
    .map(s => s.trim())
    .filter(Boolean)
  if (parts.length < 2) return ''
  const candidate = parts[1]
  if (!candidate) return ''
  if (candidate.includes('=') || candidate.toLowerCase().startsWith('features')) return ''
  return candidate
}

function buildValuationBasisInfo(raw) {
  const text = normalizeValuationBasis(raw)
  const head = (String(text).split(';')[0] || '').trim().toLowerCase()
  const legacy = head.startsWith('ai_') || /\bfeatures\s*=\s*\[/i.test(text) || /\bsamples\s*=\s*\d+/i.test(text)
  const kind = legacy ? 'legacy' : (head === 'manual_auto' ? 'manual_auto' : (head ? head : 'manual'))
  const formula = extractFormula(text)
  const capacity = extractKeyValue(text, 'capacity')
  const voltage = extractKeyValue(text, 'voltage')
  const capacityVoltage = extractKeyValue(text, 'capacityVoltage')
  const maintenanceCount = extractKeyValue(text, 'maintenanceCount')
  const batteryStatus = extractKeyValue(text, 'batteryStatus')
  const metrics = [
    { key: 'capacity', label: '容量(kWh)', value: capacity },
    { key: 'voltage', label: '电压(V)', value: voltage },
    { key: 'capacityVoltage', label: '容量×电压', value: capacityVoltage },
    { key: 'maintenanceCount', label: '维修次数', value: maintenanceCount },
    { key: 'batteryStatus', label: '状态码', value: batteryStatus }
  ].filter(m => String(m.value || '').trim())
  const ignoredKeys = new Set(['features', 'samples'])
  const pairsAll = parseBasisPairs(text)
    .map(p => ({ key: normalizeBasisKey(p.key), rawKey: p.key, value: p.value }))
    .filter(p => !ignoredKeys.has(String(p.rawKey || '').trim()))

  const dedup = new Map()
  for (const p of pairsAll) {
    const k = String(p.key || '').trim()
    if (!k || dedup.has(k)) continue
    dedup.set(k, p)
  }
  const pairs = Array.from(dedup.values())
    .map(p => ({ key: p.key || p.rawKey, label: labelBasisKey(p.key || p.rawKey), value: p.value }))
  const pairsPreviewLimit = 12
  const pairsPreview = pairs.slice(0, pairsPreviewLimit)
  const morePairsCount = Math.max(0, pairs.length - pairsPreview.length)
  return {
    kind,
    metrics,
    formula,
    pairsPreview,
    morePairsCount
  }
}

const copyText = async (text) => {
  const t = String(text || '')
  if (!t.trim()) {
    ElMessage.warning('没有可复制内容')
    return
  }
  try {
    await navigator.clipboard.writeText(t)
    ElMessage.success('已复制')
  } catch (e) {
    ElMessage.warning('复制失败，请手动选中复制')
  }
}

const autoFillPreliminary = async () => {
  const batteryId = String(activeRow.value?.batteryId || '').trim()
  if (!activeRow.value?.appraisalId || !batteryId) return
  if (autoCalcLoading.value) return

  autoCalcLoading.value = true
  try {
    const res = await calcRecyclingValuation({
      appraisalId: activeRow.value.appraisalId
    })
    const row = res?.data || res
    if (row) {
      activeRow.value = row
      valuationForm.preliminaryValue = row?.preliminaryValue != null ? String(row.preliminaryValue) : ''
    }
    ElMessage.success('已自动计算预估价')
    getList()
  } catch (e) {
    ElMessage.warning(e?.message || '自动计算失败')
  } finally {
    autoCalcLoading.value = false
  }
}

const activeValuationBasisText = computed(() => normalizeValuationBasis(activeRow.value?.valuationBasis))
const receiptValuationBasisText = computed(() => normalizeValuationBasis(receiptData.value?.valuationBasis))
const detailValuationBasisText = computed(() => normalizeValuationBasis(activeRow.value?.valuationBasis))

const activeValuationBasisInfo = computed(() => buildValuationBasisInfo(activeRow.value?.valuationBasis))
const receiptValuationBasisInfo = computed(() => buildValuationBasisInfo(receiptData.value?.valuationBasis))
const detailValuationBasisInfo = computed(() => buildValuationBasisInfo(activeRow.value?.valuationBasis))

const detailTxHashToShow = computed(() => {
  if (!detailVisible.value) return ''
  const row = activeRow.value
  return String(row?.receiptHash || row?.dataHash || '').trim()
})

watch(
  () => receiptData.value?.receiptHash,
  (hash) => {
    loadChainTx(hash, receiptChainRow, receiptChainLoading)
  }
)

watch(
  detailTxHashToShow,
  (hash) => {
    loadChainTx(hash, detailChainRow, detailChainLoading)
  },
  { immediate: true }
)

const handlePhotoExceed = () => {
  ElMessage.warning('照片最多上传 10 张')
}

const handleReportExceed = () => {
  ElMessage.warning('检测报告最多上传 1 个')
}

const handlePhotoUpload = (options) => {
  if (!activeRow.value?.appraisalId) {
    options.onError && options.onError(new Error('缺少评估ID'))
    return
  }
  const formData = new FormData()
  formData.append('appraisalId', String(activeRow.value.appraisalId))
  formData.append('file', options.file)
  uploadRecyclingPhoto(formData).then((res) => {
    const url = res?.data || res
    const urls = parseStringList(activeRow.value.photoUrls)
    urls.push(String(url || ''))
    activeRow.value.photoUrls = JSON.stringify(urls.filter(Boolean))
    ElMessage.success('上传成功')
    options.onSuccess && options.onSuccess(url)
  }).catch((err) => {
    options.onError && options.onError(err)
  })
}

const handleReportUpload = (options) => {
  if (!activeRow.value?.appraisalId) {
    options.onError && options.onError(new Error('缺少评估ID'))
    return
  }
  const formData = new FormData()
  formData.append('appraisalId', String(activeRow.value.appraisalId))
  formData.append('file', options.file)
  uploadRecyclingReport(formData).then((res) => {
    const url = res?.data || res
    activeRow.value.performanceReportUrl = String(url || '')
    ElMessage.success('上传成功')
    options.onSuccess && options.onSuccess(url)
  }).catch((err) => {
    options.onError && options.onError(err)
  })
}

const confirmPrice = () => {
  if (!activeRow.value?.appraisalId) return
  if (activeRow.value?.preliminaryValue == null) {
    ElMessage.warning('请先保存预估价')
    return
  }
  const finalValue = Number(String(confirmForm.finalValue || '').trim())
  if (!Number.isFinite(finalValue) || finalValue <= 0) {
    ElMessage.warning('最终回收价必须大于0')
    return
  }
  confirmSubmitting.value = true
  confirmRecyclingPrice({ appraisalId: activeRow.value.appraisalId, finalValue }).then(() => {
    ElMessage.success('已确认价格并完成回收')
    valuationDialogVisible.value = false
    getList()
  }).finally(() => {
    confirmSubmitting.value = false
  })
}

const openReceiptDialog = (row) => {
  const id = row?.appraisalId
  if (!id) return
  receiptDialogVisible.value = true
  receiptData.value = null
  getRecyclingReceipt(id).then((data) => {
    receiptData.value = data?.data || data
  })
}

const openTxHash = (txHash) => {
  const hash = String(txHash || '').trim()
  if (!hash) return
  router.push({ name: 'Trace', query: { txHash: hash } }).catch(() => { })
}

const openFileInNewTab = (rawUrl) => {
  const url = resolveFileUrl(rawUrl)
  if (!url) return
  window.open(url, '_blank')
}

const formatDateTime = (val) => {
  if (!val) return '-'
  const text = String(val)
  return text.replace('T', ' ')
}

const formatMoney = (val) => {
  if (val == null || val === '') return '-'
  const num = Number(val)
  if (!Number.isFinite(num)) return String(val)
  return num.toFixed(2)
}

onMounted(() => {
  const qBatteryId = String(route.query?.batteryId || '').trim()
  if (qBatteryId) {
    listQuery.batteryId = qBatteryId
  }
  getList()
})

watch(
  () => route.query?.batteryId,
  (val) => {
    const id = String(val || '').trim()
    if (id) {
      listQuery.batteryId = id
      listQuery.pageNum = 1
      getList()
    }
  }
)
</script>

<style scoped>
.recycling-photo-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.recycling-photo-item {
  width: 150px;
  height: 100px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.valuation-basis {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.valuation-basis__empty {
  color: #909399;
}

.valuation-basis__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.valuation-basis__features {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.valuation-basis__label {
  font-size: 12px;
  color: #909399;
}

.valuation-basis__formula {
  font-size: 12px;
  color: #606266;
}

.valuation-basis__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.valuation-basis__more {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}

.valuation-basis__raw {
  border-radius: 4px;
  border: 1px dashed #ebeef5;
  background-color: #f8f9fb;
  padding: 6px 8px;
}

.valuation-basis__raw-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.valuation-basis__rawbox {
  margin-top: 6px;
  max-height: 140px;
  overflow: auto;
}

.valuation-basis__pre {
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
