import request from '@/utils/request'

export function getUserList(params) {
  return request({
    url: '/system/user/list',
    method: 'get',
    params
  })
}

export function getUserOptions() {
  return request({
    url: '/system/user/options',
    method: 'get'
  })
}

export function createUser(data) {
  return request({
    url: '/system/user',
    method: 'post',
    data
  })
}

export function updateUser(data) {
  return request({
    url: '/system/user',
    method: 'put',
    data
  })
}

export function resetUserPassword(id, password) {
  return request({
    url: '/system/user/' + id + '/password',
    method: 'put',
    data: { password }
  })
}

export function getUserPassword(id) {
  return request({
    url: '/system/user/' + id + '/password',
    method: 'get'
  })
}

export function deleteUser(id) {
  return request({
    url: '/system/user/' + id,
    method: 'delete'
  })
}

export function getUserRoleIds(id) {
  return request({
    url: '/system/user/' + id + '/roleIds',
    method: 'get'
  })
}

export function updateUserRoleIds(id, roleIds) {
  return request({
    url: '/system/user/' + id + '/roleIds',
    method: 'put',
    data: roleIds
  })
}

export function getRoleList(params) {
  return request({
    url: '/system/role/list',
    method: 'get',
    params
  })
}

export function getRoleAll() {
  return request({
    url: '/system/role/all',
    method: 'get'
  })
}

export function createRole(data) {
  return request({
    url: '/system/role',
    method: 'post',
    data
  })
}

export function updateRole(data) {
  return request({
    url: '/system/role',
    method: 'put',
    data
  })
}

export function deleteRole(id) {
  return request({
    url: '/system/role/' + id,
    method: 'delete'
  })
}

export function getRoleMenuIds(id) {
  return request({
    url: '/system/role/' + id + '/menuIds',
    method: 'get'
  })
}

export function updateRoleMenuIds(id, menuIds) {
  return request({
    url: '/system/role/' + id + '/menuIds',
    method: 'put',
    data: menuIds
  })
}

export function getMenuTree() {
  return request({
    url: '/system/menu/tree',
    method: 'get'
  })
}

export function getDeptList() {
  return request({
    url: '/system/dept/list',
    method: 'get'
  })
}

export function getDeptTree() {
  return request({
    url: '/system/dept/tree',
    method: 'get'
  })
}

export function createDept(data) {
  return request({
    url: '/system/dept',
    method: 'post',
    data
  })
}

export function updateDept(data) {
  return request({
    url: '/system/dept',
    method: 'put',
    data
  })
}

export function deleteDept(id) {
  return request({
    url: '/system/dept/' + id,
    method: 'delete'
  })
}
