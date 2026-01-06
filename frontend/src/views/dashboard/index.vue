<template>
  <div class="dashboard-page">
    <div class="dashboard-header">
      <div class="header-left">
        <div class="header-title">欢迎回来，{{ displayName }}</div>
        <div class="header-subtitle">
          电池溯源系统 · {{ dateText }}
          <el-tag v-if="rolesText" type="info" size="small" style="margin-left: 10px;">
            {{ rolesText }}
          </el-tag>
        </div>
      </div>
      <div class="header-right">
        <el-button :loading="refreshing" icon="Refresh" @click="refreshAll">刷新</el-button>
      </div>
    </div>

    <template v-if="homeType === 'admin'">
      <el-row :gutter="14" class="stat-row">
        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="stat-card" @click="goTo('BatteryInfo')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">电池总数</div>
                <div class="stat-value">{{ adminStat.batteryTotal }}</div>
                <div class="stat-hint">覆盖生产、质检、销售、回收全生命周期</div>
              </div>
              <div class="stat-icon stat-icon-blue">
                <el-icon size="22">
                  <Box />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="stat-card" @click="goTo('BatteryBatch')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">生产批次</div>
                <div class="stat-value">{{ adminStat.batchTotal }}</div>
                <div class="stat-hint">批次维度管理生产计划与产能</div>
              </div>
              <div class="stat-icon stat-icon-purple">
                <el-icon size="22">
                  <Collection />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="stat-card" @click="goToAuditCenter">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">待办审核</div>
                <div class="stat-value">{{ adminStat.pendingAuditTotal }}</div>
                <div class="stat-hint">质检 / 销售 / 回收等统一审核入口</div>
              </div>
              <div class="stat-icon stat-icon-orange">
                <el-icon size="22">
                  <Stamp />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="stat-card" @click="goTo('Trace')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">上链交易</div>
                <div class="stat-value">{{ adminStat.chainTotal }}</div>
                <div class="stat-hint">不可篡改的证据哈希与交易记录</div>
              </div>
              <div class="stat-icon stat-icon-green">
                <el-icon size="22">
                  <Link />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="14" class="main-row">
        <el-col :xs="24" :lg="16">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>近7日上链趋势</span>
                <span class="panel-extra">更新时间：{{ lastUpdatedText }}</span>
              </div>
            </template>
            <div v-if="trendLoading" class="panel-loading">
              <el-skeleton :rows="6" animated />
            </div>
            <div v-else-if="!trendHasData" class="panel-empty">
              <el-empty description="暂无可展示的趋势数据" />
            </div>
            <div v-else ref="trendChartRef" class="chart" />
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="panel-card" style="margin-bottom: 14px;">
            <template #header>
              <div class="panel-header">
                <span>电池类型分布</span>
                <span class="panel-extra">样本：{{ pieSampleText }}</span>
              </div>
            </template>
            <div v-if="pieLoading" class="panel-loading">
              <el-skeleton :rows="5" animated />
            </div>
            <div v-else-if="!pieHasData" class="panel-empty">
              <el-empty description="暂无可展示的类型数据" />
            </div>
            <div v-else ref="pieChartRef" class="chart chart-sm" />
          </el-card>

          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>快捷入口</span>
                <span class="panel-extra">管理员工作台</span>
              </div>
            </template>
            <div class="quick-actions">
              <div v-for="item in homeActions" :key="item.key" class="quick-item" @click="goTo(item.routeName)">
                <div class="quick-icon">
                  <el-icon size="18">
                    <component :is="item.icon" />
                  </el-icon>
                </div>
                <div class="quick-text">
                  <div class="quick-title">{{ item.title }}</div>
                  <div class="quick-desc">{{ item.desc }}</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="14" class="bottom-row">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>待办审核（最近）</span>
                <el-button size="small" text type="primary" @click="goToAuditCenter">查看全部</el-button>
              </div>
            </template>
            <el-table :data="pendingAuditRows" size="small" border style="width: 100%;" height="320">
              <el-table-column label="类型" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="auditTypeMap[row?.businessType]?.tagType || 'info'" size="small">
                    {{ auditTypeMap[row?.businessType]?.text || (row?.businessType || '-') }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="业务ID" prop="businessId" align="center" width="120" />
              <el-table-column label="申请人" prop="applyUser" align="center" width="120" />
              <el-table-column label="申请时间" align="center" min-width="160">
                <template #default="{ row }">
                  <span>{{ formatDateTime(row?.applyTime) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center" width="110">
                <template #default="{ row }">
                  <el-button size="small" type="primary" @click="openAudit(row)">处理</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!pendingAuditRows.length" style="padding: 12px 0;">
              <el-empty description="当前暂无待办审核" />
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>最新上链交易</span>
                <el-button size="small" text type="primary" @click="goTo('Trace')">进入溯源</el-button>
              </div>
            </template>
            <el-table :data="recentChainRows" size="small" border style="width: 100%;" height="320"
              @row-click="openChainTx">
              <el-table-column label="TxHash" align="center" min-width="210">
                <template #default="{ row }">
                  <span class="mono">{{ shortTxHash(row?.txHash) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="区块" align="center" width="90">
                <template #default="{ row }">
                  <el-tag type="info" size="small">#{{ row?.blockHeight ?? '-' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="方法" prop="methodName" align="center" width="120" />
              <el-table-column label="状态" align="center" width="90">
                <template #default="{ row }">
                  <el-tag :type="row?.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row?.status === 1 ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="上链时间" align="center" min-width="160">
                <template #default="{ row }">
                  <span>{{ formatDateTime(row?.createTime) }}</span>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!recentChainRows.length" style="padding: 12px 0;">
              <el-empty description="暂无交易数据" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <template v-else-if="homeType === 'manufacturer'">
      <el-row :gutter="14" class="stat-row">
        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="stat-card" @click="goTo('BatteryInfo')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">电池总数</div>
                <div class="stat-value">{{ manufacturerStat.batteryTotal }}</div>
                <div class="stat-hint">维护生产档案与批次追溯</div>
              </div>
              <div class="stat-icon stat-icon-blue">
                <el-icon size="22">
                  <Box />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="stat-card" @click="goTo('BatteryBatch')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">生产批次</div>
                <div class="stat-value">{{ manufacturerStat.batchTotal }}</div>
                <div class="stat-hint">批次工艺与产能统计</div>
              </div>
              <div class="stat-icon stat-icon-purple">
                <el-icon size="22">
                  <Collection />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="stat-card" @click="goTo('QualityPending')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">待质检任务</div>
                <div class="stat-value">{{ manufacturerStat.pendingQualityTotal }}</div>
                <div class="stat-hint">录入质检并上链</div>
              </div>
              <div class="stat-icon stat-icon-orange">
                <el-icon size="22">
                  <Edit />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="stat-card" @click="goTo('Trace')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">上链交易</div>
                <div class="stat-value">{{ manufacturerStat.chainTotal }}</div>
                <div class="stat-hint">查看最新交易与证据哈希</div>
              </div>
              <div class="stat-icon stat-icon-green">
                <el-icon size="22">
                  <Link />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="14" class="main-row">
        <el-col :xs="24" :lg="16">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>近7日上链趋势</span>
                <span class="panel-extra">更新时间：{{ lastUpdatedText }}</span>
              </div>
            </template>
            <div v-if="trendLoading" class="panel-loading">
              <el-skeleton :rows="6" animated />
            </div>
            <div v-else-if="!trendHasData" class="panel-empty">
              <el-empty description="暂无可展示的趋势数据" />
            </div>
            <div v-else ref="trendChartRef" class="chart" />
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="panel-card" style="margin-bottom: 14px;">
            <template #header>
              <div class="panel-header">
                <span>电池类型分布</span>
                <span class="panel-extra">样本：{{ pieSampleText }}</span>
              </div>
            </template>
            <div v-if="pieLoading" class="panel-loading">
              <el-skeleton :rows="5" animated />
            </div>
            <div v-else-if="!pieHasData" class="panel-empty">
              <el-empty description="暂无可展示的类型数据" />
            </div>
            <div v-else ref="pieChartRef" class="chart chart-sm" />
          </el-card>

          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>快捷入口</span>
                <span class="panel-extra">生产工作台</span>
              </div>
            </template>
            <div class="quick-actions">
              <div v-for="item in homeActions" :key="item.key" class="quick-item" @click="goTo(item.routeName)">
                <div class="quick-icon">
                  <el-icon size="18">
                    <component :is="item.icon" />
                  </el-icon>
                </div>
                <div class="quick-text">
                  <div class="quick-title">{{ item.title }}</div>
                  <div class="quick-desc">{{ item.desc }}</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="14" class="bottom-row">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>待审核质检记录</span>
                <el-button size="small" text type="primary" @click="goTo('QualityAudit')">查看全部</el-button>
              </div>
            </template>
            <el-table :data="pendingQualityRows" size="small" border style="width: 100%;" height="320">
              <el-table-column label="电池ID" prop="batteryId" align="center" min-width="180" />
              <el-table-column label="检测人" prop="inspector" align="center" width="120" />
              <el-table-column label="检测时间" align="center" min-width="160">
                <template #default="{ row }">
                  <span>{{ formatDateTime(row?.checkTime) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center" width="110">
                <template #default>
                  <el-button size="small" type="primary" @click="goTo('QualityAudit')">审核</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!pendingQualityRows.length" style="padding: 12px 0;">
              <el-empty description="当前暂无待审核质检" />
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>最新上链交易</span>
                <el-button size="small" text type="primary" @click="goTo('Trace')">进入溯源</el-button>
              </div>
            </template>
            <el-table :data="recentChainRows" size="small" border style="width: 100%;" height="320"
              @row-click="openChainTx">
              <el-table-column label="TxHash" align="center" min-width="210">
                <template #default="{ row }">
                  <span class="mono">{{ shortTxHash(row?.txHash) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="区块" align="center" width="90">
                <template #default="{ row }">
                  <el-tag type="info" size="small">#{{ row?.blockHeight ?? '-' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="方法" prop="methodName" align="center" width="120" />
              <el-table-column label="状态" align="center" width="90">
                <template #default="{ row }">
                  <el-tag :type="row?.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row?.status === 1 ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="上链时间" align="center" min-width="160">
                <template #default="{ row }">
                  <span>{{ formatDateTime(row?.createTime) }}</span>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!recentChainRows.length" style="padding: 12px 0;">
              <el-empty description="暂无交易数据" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <template v-else-if="homeType === 'sales'">
      <el-row :gutter="14" class="stat-row">
        <el-col :xs="24" :sm="12" :lg="8">
          <el-card shadow="never" class="stat-card" @click="goTo('SalesRecordList')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">销售记录</div>
                <div class="stat-value">{{ salesStat.salesTotal }}</div>
                <div class="stat-hint">创建、编辑与提交销售单据</div>
              </div>
              <div class="stat-icon stat-icon-blue">
                <el-icon size="22">
                  <Sort />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="8">
          <el-card shadow="never" class="stat-card" @click="goTo('SalesAudit')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">待审核销售</div>
                <div class="stat-value">{{ salesStat.pendingSalesTotal }}</div>
                <div class="stat-hint">处理待审核销售订单</div>
              </div>
              <div class="stat-icon stat-icon-orange">
                <el-icon size="22">
                  <Stamp />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="8">
          <el-card shadow="never" class="stat-card" @click="goTo('Trace')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">上链交易</div>
                <div class="stat-value">{{ salesStat.chainTotal }}</div>
                <div class="stat-hint">按交易哈希或电池查询</div>
              </div>
              <div class="stat-icon stat-icon-green">
                <el-icon size="22">
                  <Link />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="14" class="main-row">
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>快捷入口</span>
                <span class="panel-extra">销售工作台</span>
              </div>
            </template>
            <div class="quick-actions">
              <div v-for="item in homeActions" :key="item.key" class="quick-item" @click="goTo(item.routeName)">
                <div class="quick-icon">
                  <el-icon size="18">
                    <component :is="item.icon" />
                  </el-icon>
                </div>
                <div class="quick-text">
                  <div class="quick-title">{{ item.title }}</div>
                  <div class="quick-desc">{{ item.desc }}</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="16">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>待审核销售（最近）</span>
                <el-button size="small" text type="primary" @click="goTo('SalesAudit')">查看全部</el-button>
              </div>
            </template>
            <el-table :data="pendingSalesRows" size="small" border style="width: 100%;" height="320">
              <el-table-column label="销售ID" prop="salesId" align="center" width="90" />
              <el-table-column label="电池ID" prop="batteryId" align="center" min-width="180" />
              <el-table-column label="买家" prop="buyerName" align="center" width="120" />
              <el-table-column label="销售日期" align="center" min-width="160">
                <template #default="{ row }">
                  <span>{{ formatDateTime(row?.salesDate) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center" width="110">
                <template #default>
                  <el-button size="small" type="primary" @click="goTo('SalesAudit')">审核</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!pendingSalesRows.length" style="padding: 12px 0;">
              <el-empty description="当前暂无待审核销售" />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="14" class="bottom-row">
        <el-col :xs="24">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>最新上链交易</span>
                <el-button size="small" text type="primary" @click="goTo('Trace')">进入溯源</el-button>
              </div>
            </template>
            <el-table :data="recentChainRows" size="small" border style="width: 100%;" height="320"
              @row-click="openChainTx">
              <el-table-column label="TxHash" align="center" min-width="210">
                <template #default="{ row }">
                  <span class="mono">{{ shortTxHash(row?.txHash) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="区块" align="center" width="90">
                <template #default="{ row }">
                  <el-tag type="info" size="small">#{{ row?.blockHeight ?? '-' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="方法" prop="methodName" align="center" width="120" />
              <el-table-column label="状态" align="center" width="90">
                <template #default="{ row }">
                  <el-tag :type="row?.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row?.status === 1 ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="上链时间" align="center" min-width="160">
                <template #default="{ row }">
                  <span>{{ formatDateTime(row?.createTime) }}</span>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!recentChainRows.length" style="padding: 12px 0;">
              <el-empty description="暂无交易数据" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <template v-else-if="homeType === 'maintenance'">
      <el-row :gutter="14" class="stat-row">
        <el-col :xs="24" :sm="12" :lg="8">
          <el-card shadow="never" class="stat-card" @click="goTo('MaintenanceRecord')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">售后工单</div>
                <div class="stat-value">{{ maintenanceStat.maintenanceTotal }}</div>
                <div class="stat-hint">创建与跟进维修保养记录</div>
              </div>
              <div class="stat-icon stat-icon-blue">
                <el-icon size="22">
                  <Edit />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="8">
          <el-card shadow="never" class="stat-card" @click="goTo('MaintenanceRecord')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">进行中工单</div>
                <div class="stat-value">{{ maintenanceStat.ongoingTotal }}</div>
                <div class="stat-hint">优先处理进行中的售后任务</div>
              </div>
              <div class="stat-icon stat-icon-orange">
                <el-icon size="22">
                  <Checked />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="8">
          <el-card shadow="never" class="stat-card" @click="goTo('Trace')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">上链交易</div>
                <div class="stat-value">{{ maintenanceStat.chainTotal }}</div>
                <div class="stat-hint">查看维保与流转链上记录</div>
              </div>
              <div class="stat-icon stat-icon-green">
                <el-icon size="22">
                  <Link />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="14" class="main-row">
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>快捷入口</span>
                <span class="panel-extra">售后工作台</span>
              </div>
            </template>
            <div class="quick-actions">
              <div v-for="item in homeActions" :key="item.key" class="quick-item" @click="goTo(item.routeName)">
                <div class="quick-icon">
                  <el-icon size="18">
                    <component :is="item.icon" />
                  </el-icon>
                </div>
                <div class="quick-text">
                  <div class="quick-title">{{ item.title }}</div>
                  <div class="quick-desc">{{ item.desc }}</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="16">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>进行中工单（最近）</span>
                <el-button size="small" text type="primary" @click="goTo('MaintenanceRecord')">查看全部</el-button>
              </div>
            </template>
            <el-table :data="ongoingMaintenanceRows" size="small" border style="width: 100%;" height="320">
              <el-table-column label="记录ID" prop="recordId" align="center" width="90" />
              <el-table-column label="电池ID" prop="batteryId" align="center" min-width="180" />
              <el-table-column label="故障类型" prop="faultType" align="center" width="140" />
              <el-table-column label="维修人" prop="maintainer" align="center" width="120" />
              <el-table-column label="操作" align="center" width="110">
                <template #default>
                  <el-button size="small" type="primary" @click="goTo('MaintenanceRecord')">处理</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!ongoingMaintenanceRows.length" style="padding: 12px 0;">
              <el-empty description="当前暂无进行中工单" />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="14" class="bottom-row">
        <el-col :xs="24">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>最新上链交易</span>
                <el-button size="small" text type="primary" @click="goTo('Trace')">进入溯源</el-button>
              </div>
            </template>
            <el-table :data="recentChainRows" size="small" border style="width: 100%;" height="320"
              @row-click="openChainTx">
              <el-table-column label="TxHash" align="center" min-width="210">
                <template #default="{ row }">
                  <span class="mono">{{ shortTxHash(row?.txHash) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="区块" align="center" width="90">
                <template #default="{ row }">
                  <el-tag type="info" size="small">#{{ row?.blockHeight ?? '-' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="方法" prop="methodName" align="center" width="120" />
              <el-table-column label="状态" align="center" width="90">
                <template #default="{ row }">
                  <el-tag :type="row?.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row?.status === 1 ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="上链时间" align="center" min-width="160">
                <template #default="{ row }">
                  <span>{{ formatDateTime(row?.createTime) }}</span>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!recentChainRows.length" style="padding: 12px 0;">
              <el-empty description="暂无交易数据" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <template v-else-if="homeType === 'recycler'">
      <el-row :gutter="14" class="stat-row">
        <el-col :xs="24" :sm="12" :lg="8">
          <el-card shadow="never" class="stat-card" @click="goTo('RecyclingAppraisal')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">回收流程</div>
                <div class="stat-value">{{ recyclerStat.recyclingTotal }}</div>
                <div class="stat-hint">回收评估、审核与确认</div>
              </div>
              <div class="stat-icon stat-icon-blue">
                <el-icon size="22">
                  <Edit />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="8">
          <el-card shadow="never" class="stat-card" @click="goTo('RecyclingAppraisal')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">待审核回收</div>
                <div class="stat-value">{{ recyclerStat.pendingAuditTotal }}</div>
                <div class="stat-hint">处理待审核回收申请</div>
              </div>
              <div class="stat-icon stat-icon-orange">
                <el-icon size="22">
                  <Stamp />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="8">
          <el-card shadow="never" class="stat-card" @click="goTo('Trace')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">上链交易</div>
                <div class="stat-value">{{ recyclerStat.chainTotal }}</div>
                <div class="stat-hint">查看回收与估值链上记录</div>
              </div>
              <div class="stat-icon stat-icon-green">
                <el-icon size="22">
                  <Link />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="14" class="main-row">
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>快捷入口</span>
                <span class="panel-extra">回收工作台</span>
              </div>
            </template>
            <div class="quick-actions">
              <div v-for="item in homeActions" :key="item.key" class="quick-item" @click="goTo(item.routeName)">
                <div class="quick-icon">
                  <el-icon size="18">
                    <component :is="item.icon" />
                  </el-icon>
                </div>
                <div class="quick-text">
                  <div class="quick-title">{{ item.title }}</div>
                  <div class="quick-desc">{{ item.desc }}</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="16">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>待审核回收（最近）</span>
                <el-button size="small" text type="primary" @click="goTo('RecyclingAppraisal')">查看全部</el-button>
              </div>
            </template>
            <el-table :data="pendingRecyclingRows" size="small" border style="width: 100%;" height="320">
              <el-table-column label="评估ID" prop="appraisalId" align="center" width="110" />
              <el-table-column label="电池ID" prop="batteryId" align="center" min-width="180" />
              <el-table-column label="申请人" prop="applyUser" align="center" width="120" />
              <el-table-column label="申请时间" align="center" min-width="160">
                <template #default="{ row }">
                  <span>{{ formatDateTime(row?.applyTime) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center" width="110">
                <template #default>
                  <el-button size="small" type="primary" @click="goTo('RecyclingAppraisal')">审核</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!pendingRecyclingRows.length" style="padding: 12px 0;">
              <el-empty description="当前暂无待审核回收" />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="14" class="bottom-row">
        <el-col :xs="24">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>最新上链交易</span>
                <el-button size="small" text type="primary" @click="goTo('Trace')">进入溯源</el-button>
              </div>
            </template>
            <el-table :data="recentChainRows" size="small" border style="width: 100%;" height="320"
              @row-click="openChainTx">
              <el-table-column label="TxHash" align="center" min-width="210">
                <template #default="{ row }">
                  <span class="mono">{{ shortTxHash(row?.txHash) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="区块" align="center" width="90">
                <template #default="{ row }">
                  <el-tag type="info" size="small">#{{ row?.blockHeight ?? '-' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="方法" prop="methodName" align="center" width="120" />
              <el-table-column label="状态" align="center" width="90">
                <template #default="{ row }">
                  <el-tag :type="row?.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row?.status === 1 ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="上链时间" align="center" min-width="160">
                <template #default="{ row }">
                  <span>{{ formatDateTime(row?.createTime) }}</span>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!recentChainRows.length" style="padding: 12px 0;">
              <el-empty description="暂无交易数据" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <template v-else>
      <el-row :gutter="14" class="stat-row">
        <el-col :xs="24" :sm="12" :lg="12">
          <el-card shadow="never" class="stat-card" @click="goTo('Trace')">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">溯源查询</div>
                <div class="stat-value">{{ ownerStat.chainTotal }}</div>
                <div class="stat-hint">按电池ID或交易哈希查看生命周期</div>
              </div>
              <div class="stat-icon stat-icon-green">
                <el-icon size="22">
                  <Search />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="12">
          <el-card shadow="never" class="stat-card" @click="refreshAll">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-label">最新交易</div>
                <div class="stat-value">{{ recentChainRows.length }}</div>
                <div class="stat-hint">点击刷新拉取最新链上记录</div>
              </div>
              <div class="stat-icon stat-icon-blue">
                <el-icon size="22">
                  <Link />
                </el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="14" class="main-row">
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>快捷入口</span>
                <span class="panel-extra">用户工作台</span>
              </div>
            </template>
            <div class="quick-actions">
              <div v-for="item in homeActions" :key="item.key" class="quick-item" @click="goTo(item.routeName)">
                <div class="quick-icon">
                  <el-icon size="18">
                    <component :is="item.icon" />
                  </el-icon>
                </div>
                <div class="quick-text">
                  <div class="quick-title">{{ item.title }}</div>
                  <div class="quick-desc">{{ item.desc }}</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="16">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header">
                <span>最新上链交易</span>
                <el-button size="small" text type="primary" @click="goTo('Trace')">进入溯源</el-button>
              </div>
            </template>
            <el-table :data="recentChainRows" size="small" border style="width: 100%;" height="320"
              @row-click="openChainTx">
              <el-table-column label="TxHash" align="center" min-width="210">
                <template #default="{ row }">
                  <span class="mono">{{ shortTxHash(row?.txHash) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="区块" align="center" width="90">
                <template #default="{ row }">
                  <el-tag type="info" size="small">#{{ row?.blockHeight ?? '-' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="方法" prop="methodName" align="center" width="120" />
              <el-table-column label="状态" align="center" width="90">
                <template #default="{ row }">
                  <el-tag :type="row?.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row?.status === 1 ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="上链时间" align="center" min-width="160">
                <template #default="{ row }">
                  <span>{{ formatDateTime(row?.createTime) }}</span>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!recentChainRows.length" style="padding: 12px 0;">
              <el-empty description="暂无交易数据" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getAuditList, getBatchList, getBatteryList, getMaintenanceList, getQualityList, getSalesList } from '@/api/battery'
import { getChainList, getRecyclingList } from '@/api/trace'
import * as echarts from 'echarts'
import { Box, Collection, Link, Stamp, Checked, Search, Edit, Sort } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const refreshing = ref(false)
const lastUpdated = ref(null)

const roleKeys = computed(() => {
  const roles = userStore.roles || []
  return (Array.isArray(roles) ? roles : []).map(r => String(r))
})

const roleSet = computed(() => new Set(roleKeys.value))

const homeType = computed(() => {
  const set = roleSet.value
  if (set.has('admin')) return 'admin'
  if (set.has('manufacturer')) return 'manufacturer'
  if (set.has('sales') || set.has('dealer')) return 'sales'
  if (set.has('maintainer') || set.has('maintenance')) return 'maintenance'
  if (set.has('recycler')) return 'recycler'
  if (set.has('owner')) return 'owner'
  return 'owner'
})

const adminStat = reactive({
  batteryTotal: 0,
  batchTotal: 0,
  pendingAuditTotal: 0,
  chainTotal: 0
})

const manufacturerStat = reactive({
  batteryTotal: 0,
  batchTotal: 0,
  pendingQualityTotal: 0,
  chainTotal: 0
})

const salesStat = reactive({
  salesTotal: 0,
  pendingSalesTotal: 0,
  chainTotal: 0
})

const maintenanceStat = reactive({
  maintenanceTotal: 0,
  ongoingTotal: 0,
  chainTotal: 0
})

const recyclerStat = reactive({
  recyclingTotal: 0,
  pendingAuditTotal: 0,
  chainTotal: 0
})

const ownerStat = reactive({
  chainTotal: 0
})

const pendingAuditRows = ref([])
const pendingQualityRows = ref([])
const pendingSalesRows = ref([])
const ongoingMaintenanceRows = ref([])
const pendingRecyclingRows = ref([])
const recentChainRows = ref([])

const trendLoading = ref(false)
const pieLoading = ref(false)

const trendChartRef = ref(null)
const pieChartRef = ref(null)

let trendChart = null
let pieChart = null

const auditTypeMap = {
  QUALITY: { text: '质检', tagType: 'warning' },
  SALES: { text: '销售', tagType: 'primary' },
  RECYCLING: { text: '回收', tagType: 'success' }
}

const displayName = computed(() => {
  const n = String(userStore.name || '').trim()
  return n || '用户'
})

const rolesText = computed(() => {
  const roles = userStore.roles || []
  if (!Array.isArray(roles) || roles.length === 0) return ''
  return roles.map(r => String(r)).join(' / ')
})

const dateText = computed(() => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${y}-${m}-${day} 星期${week}`
})

const lastUpdatedText = computed(() => {
  if (!lastUpdated.value) return '-'
  const d = lastUpdated.value
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  const ss = String(d.getSeconds()).padStart(2, '0')
  return `${hh}:${mm}:${ss}`
})

const actionsByHome = {
  admin: [
    { key: 'battery', title: '电池信息', desc: '维护电池档案与状态', icon: Box, routeName: 'BatteryInfo' },
    { key: 'batch', title: '生产批次', desc: '创建与管理生产批次', icon: Collection, routeName: 'BatteryBatch' },
    { key: 'quality_audit', title: '质检审核', desc: '处理待审核质检记录', icon: Checked, routeName: 'QualityAudit' },
    { key: 'sales_audit', title: '销售审核', desc: '处理待审核销售订单', icon: Stamp, routeName: 'SalesAudit' },
    { key: 'recycling', title: '回收流程', desc: '回收评估与审核', icon: Edit, routeName: 'RecyclingAppraisal' },
    { key: 'trace', title: '溯源查询', desc: '按电池或交易查看生命周期', icon: Search, routeName: 'Trace' }
  ],
  manufacturer: [
    { key: 'battery', title: '电池信息', desc: '维护电池档案与状态', icon: Box, routeName: 'BatteryInfo' },
    { key: 'batch', title: '生产批次', desc: '创建与管理生产批次', icon: Collection, routeName: 'BatteryBatch' },
    { key: 'quality_pending', title: '待质检任务', desc: '录入质检并上链', icon: Edit, routeName: 'QualityPending' },
    { key: 'quality_audit', title: '质检审核', desc: '处理待审核质检记录', icon: Checked, routeName: 'QualityAudit' },
    { key: 'trace', title: '溯源查询', desc: '按电池或交易查看生命周期', icon: Search, routeName: 'Trace' }
  ],
  sales: [
    { key: 'sales_list', title: '销售记录', desc: '销售单据与材料管理', icon: Sort, routeName: 'SalesRecordList' },
    { key: 'sales_audit', title: '销售审核', desc: '处理待审核销售订单', icon: Stamp, routeName: 'SalesAudit' },
    { key: 'trace', title: '溯源查询', desc: '按电池或交易查看生命周期', icon: Search, routeName: 'Trace' }
  ],
  maintenance: [
    { key: 'maintenance', title: '售后工单', desc: '创建与跟进维修保养记录', icon: Edit, routeName: 'MaintenanceRecord' },
    { key: 'trace', title: '溯源查询', desc: '按电池或交易查看生命周期', icon: Search, routeName: 'Trace' }
  ],
  recycler: [
    { key: 'recycling', title: '回收流程', desc: '回收评估与审核', icon: Edit, routeName: 'RecyclingAppraisal' },
    { key: 'trace', title: '溯源查询', desc: '按电池或交易查看生命周期', icon: Search, routeName: 'Trace' }
  ],
  owner: [
    { key: 'trace', title: '溯源查询', desc: '按电池或交易查看生命周期', icon: Search, routeName: 'Trace' }
  ]
}

const homeActions = computed(() => actionsByHome[homeType.value] || actionsByHome.owner)

const trendSeries = ref({ labels: [], values: [] })
const pieSeries = ref({ items: [], sample: 0 })

const trendHasData = computed(() => {
  const labels = trendSeries.value?.labels || []
  const values = trendSeries.value?.values || []
  if (!labels.length || !values.length) return false
  return values.some(v => Number(v) > 0)
})
const pieHasData = computed(() => (pieSeries.value?.items?.length || 0) > 0)
const pieSampleText = computed(() => pieSeries.value?.sample ? `${pieSeries.value.sample}条` : '-')

function extractPageData(res) {
  return res?.data || res || null
}

function extractTotal(res) {
  const page = extractPageData(res)
  const t = page?.total
  const n = Number(t)
  return Number.isFinite(n) ? n : 0
}

function extractRecords(res) {
  const page = extractPageData(res)
  return Array.isArray(page?.records) ? page.records : []
}

function formatDateTime(raw) {
  if (!raw) return '-'
  if (raw instanceof Date) {
    const y = raw.getFullYear()
    const m = String(raw.getMonth() + 1).padStart(2, '0')
    const d = String(raw.getDate()).padStart(2, '0')
    const hh = String(raw.getHours()).padStart(2, '0')
    const mm = String(raw.getMinutes()).padStart(2, '0')
    return `${y}-${m}-${d} ${hh}:${mm}`
  }
  const text = String(raw)
  return text.includes('T') ? text.replace('T', ' ').slice(0, 16) : text
}

function shortTxHash(txHash) {
  const t = String(txHash || '').trim()
  if (!t) return '-'
  if (t.length <= 18) return t
  return `${t.slice(0, 10)}...${t.slice(-6)}`
}

function goTo(routeName, query) {
  if (!routeName) return
  router.push({ name: routeName, query: query || undefined })
}

function goToAuditCenter() {
  const roles = userStore.roles || []
  const roleSet = new Set((Array.isArray(roles) ? roles : []).map(r => String(r)))
  if (roleSet.has('recycler')) return router.push({ name: 'RecyclingAppraisal' })
  if (roleSet.has('sales') || roleSet.has('dealer')) return router.push({ name: 'SalesAudit' })
  if (roleSet.has('manufacturer')) return router.push({ name: 'QualityAudit' })
  router.push({ name: 'Dashboard' })
}

function openAudit(row) {
  const t = String(row?.businessType || '').toUpperCase()
  if (t === 'SALES') return router.push({ name: 'SalesAudit' })
  if (t === 'QUALITY') return router.push({ name: 'QualityAudit' })
  if (t === 'RECYCLING') return router.push({ name: 'RecyclingAppraisal' })
  goToAuditCenter()
}

function openChainTx(row) {
  if (!row?.txHash) return
  router.push({ name: 'Trace', query: { txHash: row.txHash } })
}

function ensureChart(elRef, current) {
  const el = elRef.value
  if (!el) return current
  if (current && !current.isDisposed()) {
    const dom = typeof current.getDom === 'function' ? current.getDom() : null
    if (dom === el) return current
    current.dispose()
  }
  const existing = echarts.getInstanceByDom(el)
  return existing || echarts.init(el)
}

function buildTrendOption(labels, values) {
  return {
    grid: { left: 12, right: 12, top: 18, bottom: 18, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#606266' }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#ebeef5' } },
      axisLabel: { color: '#606266' }
    },
    series: [
      {
        type: 'line',
        data: values,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 3, color: '#409EFF' },
        itemStyle: { color: '#409EFF' },
        areaStyle: { color: 'rgba(64,158,255,0.18)' }
      }
    ]
  }
}

function buildPieOption(items) {
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, left: 'center' },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 13, fontWeight: 600 } },
        labelLine: { show: false },
        data: items
      }
    ]
  }
}

function renderTrendChart() {
  if (!trendHasData.value) return
  trendChart = ensureChart(trendChartRef, trendChart)
  if (!trendChart) return
  const { labels, values } = trendSeries.value
  trendChart.setOption(buildTrendOption(labels, values), true)
  requestAnimationFrame(() => {
    if (trendChart && !trendChart.isDisposed()) trendChart.resize()
  })
}

function renderPieChart() {
  if (!pieHasData.value) return
  pieChart = ensureChart(pieChartRef, pieChart)
  if (!pieChart) return
  pieChart.setOption(buildPieOption(pieSeries.value.items), true)
  requestAnimationFrame(() => {
    if (pieChart && !pieChart.isDisposed()) pieChart.resize()
  })
}

function parseDateOnly(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function buildLast7DaysLabels() {
  const today = new Date()
  const list = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    list.push(parseDateOnly(d).slice(5))
  }
  return list
}

function formatTypeLabel(typeCode) {
  const t = String(typeCode || '').toUpperCase()
  if (t === 'NCM') return '三元锂 (NCM)'
  if (t === 'LFP') return '磷酸铁锂 (LFP)'
  return t || '未知'
}

async function fetchTotals() {
  const type = homeType.value
  if (type === 'admin') {
    const [batteryRes, batchRes, auditRes, chainRes] = await Promise.all([
      getBatteryList({ pageNum: 1, pageSize: 1 }),
      getBatchList({ pageNum: 1, pageSize: 1 }),
      getAuditList({ pageNum: 1, pageSize: 1, status: 0 }),
      getChainList({ pageNum: 1, pageSize: 1 })
    ])
    adminStat.batteryTotal = extractTotal(batteryRes)
    adminStat.batchTotal = extractTotal(batchRes)
    adminStat.pendingAuditTotal = extractTotal(auditRes)
    adminStat.chainTotal = extractTotal(chainRes)
    return
  }

  if (type === 'manufacturer') {
    const [batteryRes, batchRes, qualityRes, chainRes] = await Promise.all([
      getBatteryList({ pageNum: 1, pageSize: 1 }),
      getBatchList({ pageNum: 1, pageSize: 1 }),
      getQualityList({ pageNum: 1, pageSize: 1, status: 0 }),
      getChainList({ pageNum: 1, pageSize: 1 })
    ])
    manufacturerStat.batteryTotal = extractTotal(batteryRes)
    manufacturerStat.batchTotal = extractTotal(batchRes)
    manufacturerStat.pendingQualityTotal = extractTotal(qualityRes)
    manufacturerStat.chainTotal = extractTotal(chainRes)
    return
  }

  if (type === 'sales') {
    const [salesRes, pendingRes, chainRes] = await Promise.all([
      getSalesList({ pageNum: 1, pageSize: 1 }),
      getSalesList({ pageNum: 1, pageSize: 1, status: 0 }),
      getChainList({ pageNum: 1, pageSize: 1 })
    ])
    salesStat.salesTotal = extractTotal(salesRes)
    salesStat.pendingSalesTotal = extractTotal(pendingRes)
    salesStat.chainTotal = extractTotal(chainRes)
    return
  }

  if (type === 'maintenance') {
    const [allRes, ongoingRes, chainRes] = await Promise.all([
      getMaintenanceList({ pageNum: 1, pageSize: 1 }),
      getMaintenanceList({ pageNum: 1, pageSize: 1, status: 1 }),
      getChainList({ pageNum: 1, pageSize: 1 })
    ])
    maintenanceStat.maintenanceTotal = extractTotal(allRes)
    maintenanceStat.ongoingTotal = extractTotal(ongoingRes)
    maintenanceStat.chainTotal = extractTotal(chainRes)
    return
  }

  if (type === 'recycler') {
    const [allRes, pendingRes, chainRes] = await Promise.all([
      getRecyclingList({ pageNum: 1, pageSize: 1 }),
      getRecyclingList({ pageNum: 1, pageSize: 1, status: 0 }),
      getChainList({ pageNum: 1, pageSize: 1 })
    ])
    recyclerStat.recyclingTotal = extractTotal(allRes)
    recyclerStat.pendingAuditTotal = extractTotal(pendingRes)
    recyclerStat.chainTotal = extractTotal(chainRes)
    return
  }

  const chainRes = await getChainList({ pageNum: 1, pageSize: 1 })
  ownerStat.chainTotal = extractTotal(chainRes)
}

async function fetchPendingWork() {
  const type = homeType.value
  if (type === 'admin') {
    const res = await getAuditList({ pageNum: 1, pageSize: 8, status: 0 })
    pendingAuditRows.value = extractRecords(res)
    pendingQualityRows.value = []
    pendingSalesRows.value = []
    ongoingMaintenanceRows.value = []
    pendingRecyclingRows.value = []
    return
  }

  if (type === 'manufacturer') {
    const res = await getQualityList({ pageNum: 1, pageSize: 8, status: 0 })
    pendingQualityRows.value = extractRecords(res)
    pendingAuditRows.value = []
    pendingSalesRows.value = []
    ongoingMaintenanceRows.value = []
    pendingRecyclingRows.value = []
    return
  }

  if (type === 'sales') {
    const res = await getSalesList({ pageNum: 1, pageSize: 8, status: 0 })
    pendingSalesRows.value = extractRecords(res)
    pendingAuditRows.value = []
    pendingQualityRows.value = []
    ongoingMaintenanceRows.value = []
    pendingRecyclingRows.value = []
    return
  }

  if (type === 'maintenance') {
    const res = await getMaintenanceList({ pageNum: 1, pageSize: 8, status: 1 })
    ongoingMaintenanceRows.value = extractRecords(res)
    pendingAuditRows.value = []
    pendingQualityRows.value = []
    pendingSalesRows.value = []
    pendingRecyclingRows.value = []
    return
  }

  if (type === 'recycler') {
    const res = await getRecyclingList({ pageNum: 1, pageSize: 8, status: 0 })
    pendingRecyclingRows.value = extractRecords(res)
    pendingAuditRows.value = []
    pendingQualityRows.value = []
    pendingSalesRows.value = []
    ongoingMaintenanceRows.value = []
    return
  }

  pendingAuditRows.value = []
  pendingQualityRows.value = []
  pendingSalesRows.value = []
  ongoingMaintenanceRows.value = []
  pendingRecyclingRows.value = []
}

async function fetchRecentChain() {
  const res = await getChainList({ pageNum: 1, pageSize: 8 })
  recentChainRows.value = extractRecords(res)
}

async function fetchTrend() {
  trendLoading.value = true
  try {
    const labels = buildLast7DaysLabels()
    const dayKeyMap = new Map(labels.map(l => [l, 0]))

    const res = await getChainList({ pageNum: 1, pageSize: 200 })
    const rows = extractRecords(res)
    const today = new Date()
    const start = new Date(today)
    start.setDate(today.getDate() - 6)
    start.setHours(0, 0, 0, 0)

    rows.forEach(r => {
      const rawTime = r?.createTime
      const dt = rawTime ? new Date(rawTime) : null
      if (!dt || Number.isNaN(dt.getTime())) return
      if (dt.getTime() < start.getTime()) return
      const key = parseDateOnly(dt).slice(5)
      if (!dayKeyMap.has(key)) return
      dayKeyMap.set(key, (dayKeyMap.get(key) || 0) + 1)
    })

    trendSeries.value = {
      labels,
      values: labels.map(l => dayKeyMap.get(l) || 0)
    }
  } finally {
    trendLoading.value = false
  }
}

async function fetchPie() {
  pieLoading.value = true
  try {
    const res = await getBatteryList({ pageNum: 1, pageSize: 200 })
    const rows = extractRecords(res)
    const typeCount = new Map()
    rows.forEach(r => {
      const key = String(r?.typeCode || '').toUpperCase() || 'UNKNOWN'
      typeCount.set(key, (typeCount.get(key) || 0) + 1)
    })
    const items = Array.from(typeCount.entries())
      .sort((a, b) => b[1] - a[1])
      .map(([k, v]) => ({ name: formatTypeLabel(k), value: v }))

    pieSeries.value = { items, sample: rows.length }
  } finally {
    pieLoading.value = false
  }
}

async function refreshAll() {
  if (refreshing.value) return
  refreshing.value = true
  try {
    const tasks = [fetchTotals(), fetchPendingWork(), fetchRecentChain()]
    if (homeType.value === 'admin' || homeType.value === 'manufacturer') {
      tasks.push(fetchTrend(), fetchPie())
    }
    await Promise.all(tasks)
    lastUpdated.value = new Date()
    await nextTick()
    renderTrendChart()
    renderPieChart()
  } catch (e) {
    await nextTick()
    renderTrendChart()
    renderPieChart()
  } finally {
    refreshing.value = false
  }
}

function handleResize() {
  if (trendChart && !trendChart.isDisposed()) trendChart.resize()
  if (pieChart && !pieChart.isDisposed()) pieChart.resize()
}

watch(trendSeries, async () => {
  await nextTick()
  renderTrendChart()
}, { deep: true })

watch(pieSeries, async () => {
  await nextTick()
  renderPieChart()
}, { deep: true })

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await refreshAll()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (trendChart && !trendChart.isDisposed()) trendChart.dispose()
  if (pieChart && !pieChart.isDisposed()) pieChart.dispose()
  trendChart = null
  pieChart = null
})
</script>

<style scoped>
.dashboard-page {
  min-height: calc(100vh - 110px);
}

.dashboard-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.header-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.header-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.stat-row {
  margin-bottom: 14px;
}

.stat-card {
  cursor: pointer;
  border-radius: 10px;
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.stat-left {
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #606266;
}

.stat-value {
  margin-top: 6px;
  font-size: 26px;
  font-weight: 800;
  color: #303133;
  letter-spacing: 0.2px;
}

.stat-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 280px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-blue {
  background: rgba(64, 158, 255, 0.12);
  color: #409eff;
}

.stat-icon-purple {
  background: rgba(144, 147, 153, 0.14);
  color: #606266;
}

.stat-icon-orange {
  background: rgba(230, 162, 60, 0.14);
  color: #e6a23c;
}

.stat-icon-green {
  background: rgba(103, 194, 58, 0.14);
  color: #67c23a;
}

.panel-card {
  border-radius: 10px;
}

.panel-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.panel-extra {
  color: #909399;
  font-size: 12px;
}

.panel-loading {
  padding: 6px 0;
}

.panel-empty {
  padding: 10px 0;
}

.chart {
  width: 100%;
  height: 320px;
}

.chart-sm {
  height: 240px;
}

.quick-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.15s ease;
}

.quick-item:hover {
  border-color: rgba(64, 158, 255, 0.55);
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.quick-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: rgba(64, 158, 255, 0.12);
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.quick-text {
  min-width: 0;
}

.quick-title {
  font-size: 13px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.quick-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

@media (max-width: 768px) {
  .stat-hint {
    max-width: 180px;
  }

  .chart {
    height: 260px;
  }

  .chart-sm {
    height: 220px;
  }
}
</style>
