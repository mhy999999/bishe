import request from '@/utils/request'

// 电池信息
export function getBatteryList(params) {
  return request({
    url: '/battery/info/list',
    method: 'get',
    params
  })
}

export function saveBattery(data) {
  return request({
    url: '/battery/info',
    method: 'post',
    data
  })
}

export function updateBattery(data) {
  return request({
    url: '/battery/info',
    method: 'put',
    data
  })
}

export function deleteBattery(id) {
  return request({
    url: '/battery/info/' + id,
    method: 'delete'
  })
}

// 质检记录
export function getQualityList(params) {
  return request({
    url: '/battery/quality/list',
    method: 'get',
    params
  })
}

export function saveQuality(data) {
  return request({
    url: '/battery/quality',
    method: 'post',
    data
  })
}

export function auditQuality(data) {
  return request({
    url: '/battery/quality/audit',
    method: 'post',
    data
  })
}

// 健康监测
export function getHealthList(params) {
  return request({
    url: '/battery/health/list',
    method: 'get',
    params
  })
}

export function saveHealth(data) {
  return request({
    url: '/battery/health',
    method: 'post',
    data
  })
}

// 生产批次
export function getBatchList(params) {
  return request({
    url: '/battery/batch/list',
    method: 'get',
    params
  })
}

export function endBatch(batchId) {
  return request({
    url: '/battery/batch/end/' + batchId,
    method: 'post'
  })
}

export function saveBatch(data) {
  return request({
    url: '/battery/batch',
    method: 'post',
    data
  })
}

// 故障报警
export function getAlarmList(params) {
  return request({
    url: '/battery/alarm/list',
    method: 'get',
    params
  })
}

// 维修保养
export function getMaintenanceList(params) {
  return request({
    url: '/battery/maintenance/list',
    method: 'get',
    params
  })
}

export function saveMaintenance(data) {
  return request({
    url: '/battery/maintenance',
    method: 'post',
    data
  })
}

export function updateMaintenance(data) {
  return request({
    url: '/battery/maintenance',
    method: 'put',
    data
  })
}

export function auditMaintenance(data) {
  return request({
    url: '/battery/maintenance/audit',
    method: 'post',
    data
  })
}

// 销售管理
export function getSalesList(params) {
  return request({
    url: '/battery/sales/list',
    method: 'get',
    params
  })
}

export function saveSales(data) {
  return request({
    url: '/battery/sales',
    method: 'post',
    data
  })
}

export function updateSales(data) {
  return request({
    url: '/battery/sales',
    method: 'put',
    data
  })
}

export function completeMaintenance(data) {
  return request({
    url: '/battery/maintenance/complete',
    method: 'post',
    data
  })
}

export function uploadMaintenanceMaterial(kind, data) {
  return request({
    url: '/battery/maintenance/material/upload',
    method: 'post',
    params: { kind },
    data
  })
}

export function auditSales(data) {
  return request({
    url: '/battery/sales/audit',
    method: 'post',
    data
  })
}

export function cancelSales(salesId) {
  return request({
    url: '/battery/sales/cancel/' + salesId,
    method: 'post'
  })
}

export function uploadSalesMaterial(data) {
  return request({
    url: '/battery/sales/material/upload',
    method: 'post',
    data
  })
}

// 审核中心
export function getAuditList(params) {
  return request({
    url: '/audit/list',
    method: 'get',
    params
  })
}

export function processAudit(data) {
  return request({
    url: '/audit/process',
    method: 'post',
    data
  })
}
