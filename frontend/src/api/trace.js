import request from '@/utils/request'

// 流转记录
export function getTransferList(params) {
  return request({
    url: '/trace/transfer/list',
    method: 'get',
    params
  })
}

export function saveTransfer(data) {
  return request({
    url: '/trace/transfer',
    method: 'post',
    data
  })
}

// 上链交易
export function getChainList(params) {
  return request({
    url: '/trace/chain/list',
    method: 'get',
    params
  })
}

// 回收评估
export function getRecyclingList(params) {
  return request({
    url: '/trace/recycling/list',
    method: 'get',
    params
  })
}

export function saveRecycling(data) {
  return request({
    url: '/trace/recycling',
    method: 'post',
    data
  })
}

// 维修记录
export function getMaintenanceList(params) {
  return request({
    url: '/trace/maintenance/list',
    method: 'get',
    params
  })
}

export function saveMaintenance(data) {
  return request({
    url: '/trace/maintenance',
    method: 'post',
    data
  })
}

// 销售记录
export function getSalesList(params) {
  return request({
    url: '/trace/sales/list',
    method: 'get',
    params
  })
}

// 车辆信息 (绑定)
export function getVehicleList(params) {
  return request({
    url: '/trace/vehicle/list',
    method: 'get',
    params
  })
}

export function saveVehicle(data) {
  return request({
    url: '/trace/vehicle',
    method: 'post',
    data
  })
}
