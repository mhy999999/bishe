import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/system/user/login',
    method: 'post',
    data
  })
}

export function register(data) {
  return request({
    url: '/system/user/register',
    method: 'post',
    data
  })
}

export function getInfo() {
  return request({
    url: '/system/user/info',
    method: 'get'
  })
}

export function logout() {
  return request({
    url: '/system/user/logout',
    method: 'post'
  })
}
