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
        <el-form-item label="外观描述">
          <el-input v-model="applyForm.appearance" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
        <el-form-item label="处理建议">
          <el-input v-model="applyForm.suggestion" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
        <el-form-item label="回收原因" prop="reason">
          <el-input v-model="applyForm.reason" type="textarea" :rows="5" placeholder="不少于50字" />
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

          <el-form :inline="true" :model="confirmForm" label-width="90px">
            <el-form-item label="最终回收价">
              <el-input v-model="confirmForm.finalValue" style="width: 200px;" placeholder="请输入金额">
                <template #append>元</template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="success" :loading="confirmSubmitting" @click="confirmPrice">确认价格并完成</el-button>
            </el-form-item>
          </el-form>

          <div style="margin-top: 8px;">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="估值依据">
                <div class="valuation-basis">
                  <div v-if="!activeValuationBasisText" class="valuation-basis__empty">-</div>
                  <template v-else>
                    <div class="valuation-basis__meta">
                      <el-tag v-if="activeValuationBasisInfo.model" size="small" type="info" effect="plain">
                        模型：{{ activeValuationBasisInfo.model }}
                      </el-tag>
                      <el-tag v-if="activeValuationBasisInfo.samples != null" size="small" type="success"
                        effect="plain">
                        样本：{{ activeValuationBasisInfo.samples }}
                      </el-tag>
                      <el-tag v-if="activeValuationBasisInfo.featuresCount" size="small" type="warning" effect="plain">
                        特征：{{ activeValuationBasisInfo.featuresCount }}项
                      </el-tag>
                      <el-button text size="small" @click="valuationBasisExpanded = !valuationBasisExpanded">
                        {{ valuationBasisExpanded ? '收起详情' : '展开详情' }}
                      </el-button>
                    </div>

                    <div v-if="activeValuationBasisInfo.previewFeatures.length" class="valuation-basis__features">
                      <div class="valuation-basis__label">使用特征</div>
                      <div class="valuation-basis__tags">
                        <el-tag v-for="f in activeValuationBasisInfo.previewFeatures" :key="f" size="small"
                          effect="plain">
                          {{ humanizeFeatureName(f) }}
                        </el-tag>
                        <span v-if="activeValuationBasisInfo.moreFeaturesCount" class="valuation-basis__more">
                          +{{ activeValuationBasisInfo.moreFeaturesCount }}
                        </span>
                      </div>
                    </div>

                    <div v-if="activeValuationBasisInfo.metrics.length" class="valuation-basis__features">
                      <div class="valuation-basis__label">关键数值</div>
                      <div class="valuation-basis__tags">
                        <el-tag v-for="m in activeValuationBasisInfo.metrics" :key="m.key" size="small" type="info"
                          effect="plain">
                          {{ m.label }}：{{ m.value }}
                        </el-tag>
                      </div>
                      <div v-if="activeValuationBasisInfo.formula" class="valuation-basis__formula">
                        公式：{{ activeValuationBasisInfo.formula }}
                      </div>
                    </div>

                    <div v-if="valuationBasisExpanded" class="valuation-basis__raw">
                      <pre class="valuation-basis__pre">{{ activeValuationBasisText }}</pre>
                    </div>
                  </template>
                </div>
              </el-descriptions-item>
            </el-descriptions>
          </div>
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
        <el-descriptions-item label="估值依据" :span="2">
          <div class="valuation-basis">
            <div v-if="!receiptValuationBasisText" class="valuation-basis__empty">-</div>
            <template v-else>
              <div class="valuation-basis__meta">
                <el-tag v-if="receiptValuationBasisInfo.model" size="small" type="info" effect="plain">
                  模型：{{ receiptValuationBasisInfo.model }}
                </el-tag>
                <el-tag v-if="receiptValuationBasisInfo.samples != null" size="small" type="success" effect="plain">
                  样本：{{ receiptValuationBasisInfo.samples }}
                </el-tag>
                <el-tag v-if="receiptValuationBasisInfo.featuresCount" size="small" type="warning" effect="plain">
                  特征：{{ receiptValuationBasisInfo.featuresCount }}项
                </el-tag>
                <el-button text size="small" @click="receiptBasisExpanded = !receiptBasisExpanded">
                  {{ receiptBasisExpanded ? '收起详情' : '展开详情' }}
                </el-button>
              </div>

              <div v-if="receiptValuationBasisInfo.previewFeatures.length" class="valuation-basis__features">
                <div class="valuation-basis__label">使用特征</div>
                <div class="valuation-basis__tags">
                  <el-tag v-for="f in receiptValuationBasisInfo.previewFeatures" :key="f" size="small" effect="plain">
                    {{ humanizeFeatureName(f) }}
                  </el-tag>
                  <span v-if="receiptValuationBasisInfo.moreFeaturesCount" class="valuation-basis__more">
                    +{{ receiptValuationBasisInfo.moreFeaturesCount }}
                  </span>
                </div>
              </div>

              <div v-if="receiptValuationBasisInfo.metrics.length" class="valuation-basis__features">
                <div class="valuation-basis__label">关键数值</div>
                <div class="valuation-basis__tags">
                  <el-tag v-for="m in receiptValuationBasisInfo.metrics" :key="m.key" size="small" type="info"
                    effect="plain">
                    {{ m.label }}：{{ m.value }}
                  </el-tag>
                </div>
                <div v-if="receiptValuationBasisInfo.formula" class="valuation-basis__formula">
                  公式：{{ receiptValuationBasisInfo.formula }}
                </div>
              </div>

              <div v-if="receiptBasisExpanded" class="valuation-basis__raw">
                <pre class="valuation-basis__pre">{{ receiptValuationBasisText }}</pre>
              </div>
            </template>
          </div>
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
                <el-tag v-if="detailValuationBasisInfo.model" size="small" type="info" effect="plain">
                  模型：{{ detailValuationBasisInfo.model }}
                </el-tag>
                <el-tag v-if="detailValuationBasisInfo.samples != null" size="small" type="success" effect="plain">
                  样本：{{ detailValuationBasisInfo.samples }}
                </el-tag>
                <el-tag v-if="detailValuationBasisInfo.featuresCount" size="small" type="warning" effect="plain">
                  特征：{{ detailValuationBasisInfo.featuresCount }}项
                </el-tag>
                <el-button text size="small" @click="detailBasisExpanded = !detailBasisExpanded">
                  {{ detailBasisExpanded ? '收起详情' : '展开详情' }}
                </el-button>
              </div>

              <div v-if="detailValuationBasisInfo.previewFeatures.length" class="valuation-basis__features">
                <div class="valuation-basis__label">使用特征</div>
                <div class="valuation-basis__tags">
                  <el-tag v-for="f in detailValuationBasisInfo.previewFeatures" :key="f" size="small" effect="plain">
                    {{ humanizeFeatureName(f) }}
                  </el-tag>
                  <span v-if="detailValuationBasisInfo.moreFeaturesCount" class="valuation-basis__more">
                    +{{ detailValuationBasisInfo.moreFeaturesCount }}
                  </span>
                </div>
              </div>

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
                <pre class="valuation-basis__pre">{{ detailValuationBasisText }}</pre>
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
import { applyRecycling, auditRecycling, confirmRecyclingPrice, getChainList, getRecyclingList, getRecyclingReceipt, uploadRecyclingPhoto, uploadRecyclingReport } from '@/api/trace'

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
  reason: [
    { required: true, message: '回收原因必填', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        const text = String(value || '').trim()
        if (text.length < 50) {
          callback(new Error('回收原因说明不少于50字'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
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
      appearance: String(applyForm.appearance || '').trim() || undefined,
      suggestion: String(applyForm.suggestion || '').trim() || undefined
    }
    applyRecycling(payload).then(() => {
      applyDialogVisible.value = false
      ElMessage.success('回收申请已提交')
      getList()
    }).catch((err) => {
      console.error(err)
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
  }).catch((err) => {
    console.error(err)
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

const valuationDialogVisible = ref(false)
const receiptDialogVisible = ref(false)
const receiptData = ref(null)
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
  valuationDialogVisible.value = true
}

watch(valuationDialogVisible, (val) => {
  if (!val) {
    confirmForm.finalValue = ''
    valuationBasisExpanded.value = false
  }
})

watch(receiptDialogVisible, (val) => {
  if (!val) {
    receiptBasisExpanded.value = false
  }
})

watch(detailVisible, (val) => {
  if (!val) {
    detailBasisExpanded.value = false
  }
})

function normalizeValuationBasis(raw) {
  const text = String(raw || '').trim()
  return text
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

function extractFeatures(text) {
  if (!text) return []
  const m = text.match(/features\s*=\s*\[([\s\S]*?)\]/i) || text.match(/features\s*:\s*\[([\s\S]*?)\]/i)
  if (!m || !m[1]) return []
  return String(m[1])
    .split(',')
    .map(s => s.trim())
    .filter(Boolean)
}

function extractSamples(text) {
  if (!text) return null
  const m = text.match(/samples\s*=\s*([0-9]+)/i) || text.match(/samples\s*:\s*([0-9]+)/i)
  if (!m || !m[1]) return null
  const n = Number(m[1])
  return Number.isFinite(n) ? n : null
}

function extractModel(text) {
  if (!text) return ''
  const head = text.split(';')[0] || ''
  const h = head.trim()
  if (!h) return ''
  if (h.includes('=') || h.includes(':')) return ''
  return h
}

function buildValuationBasisInfo(raw) {
  const text = normalizeValuationBasis(raw)
  const features = extractFeatures(text)
  const samples = extractSamples(text)
  const model = extractModel(text)
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
  const previewLimit = 16
  const previewFeatures = features.slice(0, previewLimit)
  const moreFeaturesCount = Math.max(0, features.length - previewFeatures.length)
  return {
    model,
    samples,
    featuresCount: features.length,
    previewFeatures,
    moreFeaturesCount,
    metrics,
    formula
  }
}

function humanizeFeatureName(raw) {
  const t = String(raw || '').trim()
  if (!t) return '-'
  const dot = t.replaceAll('.', ' · ')
  const snake = dot.replaceAll('_', ' ')
  const camel = snake.replace(/([a-z])([A-Z])/g, '$1 $2')
  return camel
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
    console.error(err)
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
    console.error(err)
    options.onError && options.onError(err)
  })
}

const confirmPrice = () => {
  if (!activeRow.value?.appraisalId) return
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
  }).catch((err) => {
    console.error(err)
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
  }).catch((err) => {
    console.error(err)
  })
}

const openTxHash = (txHash) => {
  router.push({ name: 'Trace', query: { txHash: String(txHash || '') } })
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
  max-height: 120px;
  border-radius: 4px;
  border: 1px dashed #ebeef5;
  background-color: #f8f9fb;
  padding: 6px 8px;
}

.valuation-basis__pre {
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
