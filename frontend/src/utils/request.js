import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

let isReLoginConfirmVisible = false

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '/api', // url = base url + request url
  timeout: 5000 // request timeout
})

function tryParseJson(data) {
  if (!data) return null
  if (typeof data === 'object') return data
  if (typeof data === 'string') {
    try {
      return JSON.parse(data)
    } catch (e) {
      return null
    }
  }
  return null
}

function getFriendlyHttpMessage(status) {
  const map = {
    400: '请求参数有误，请检查后重试',
    401: '登录状态已过期，请重新登录',
    403: '暂无权限访问该功能',
    404: '接口不存在，请检查服务是否已启动',
    408: '请求超时，请稍后再试',
    429: '请求过于频繁，请稍后再试',
    500: '服务异常，请稍后再试',
    502: '网关异常，请稍后再试',
    503: '服务不可用，请稍后再试',
    504: '服务响应超时，请稍后再试'
  }
  return map[status] || '请求失败，请稍后再试'
}

// request interceptor
service.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = 'Bearer ' + userStore.token
    }
    return config
  },
  error => {
    console.log(error) // for debug
    return Promise.reject(error)
  }
)

// response interceptor
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 如果是二进制数据则直接返回
    if (response.request.responseType ===  'blob' || response.request.responseType ===  'arraybuffer') {
      return res
    }

    const isPlainObject = res !== null && typeof res === 'object' && !Array.isArray(res)
    if (!isPlainObject || typeof res.code !== 'number') {
      const baseURL = response?.config?.baseURL || ''
      const url = response?.config?.url || ''
      const endpoint = `${baseURL}${url}`.trim()
      const message = endpoint ? `接口返回异常：${endpoint}` : '接口返回异常'
      ElMessage({
        message,
        type: 'error',
        duration: 5 * 1000
      })
      return Promise.reject(new Error(message))
    }

    if (res.code !== 200) {
      const baseURL = response?.config?.baseURL || ''
      const url = response?.config?.url || ''
      const endpoint = `${baseURL}${url}`.trim()
      const rawMessage = res.message || '操作失败'
      const displayMessage = endpoint ? `${rawMessage}：${endpoint}` : rawMessage
      ElMessage({
        message: displayMessage,
        type: 'error',
        duration: 5 * 1000
      })

      // 401: Token expired or invalid
      // Note: Backend might return 500 for token invalid currently, but ideally 401. 
      // Current JwtInterceptor in backend usually sets 401 response status or returns error json.
      // Let's assume standard handling.
      if (res.code === 401) {
        if (!isReLoginConfirmVisible) {
          isReLoginConfirmVisible = true
          ElMessageBox.confirm('登录状态已失效，请重新登录后继续操作', '系统提示', {
            confirmButtonText: '重新登录',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            const userStore = useUserStore()
            userStore.resetToken().then(() => {
              location.reload()
            })
          }).finally(() => {
            isReLoginConfirmVisible = false
          })
        }
      }
      return Promise.reject(new Error(displayMessage))
    } else {
      return res.data
    }
  },
  error => {
    console.log('err' + error) // for debug
    const response = error && error.response
    if (response) {
      const parsed = tryParseJson(response.data) || {}
      const backendMessage = parsed.message || parsed.msg
      const status = response.status
      const message = backendMessage || getFriendlyHttpMessage(status)

      if (status === 401 || parsed.code === 401) {
        if (!isReLoginConfirmVisible) {
          isReLoginConfirmVisible = true
          ElMessageBox.confirm('登录状态已失效，请重新登录后继续操作', '系统提示', {
            confirmButtonText: '重新登录',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            const userStore = useUserStore()
            userStore.resetToken().then(() => {
              router.push('/login')
              location.reload()
            })
          }).finally(() => {
            isReLoginConfirmVisible = false
          })
        }
        return Promise.reject(new Error(message))
      }

      ElMessage({
        message,
        type: 'error',
        duration: 5 * 1000
      })
      return Promise.reject(new Error(message))
    }

    let message = '网络异常，请检查网络后重试'
    if (error && error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后再试'
    }
    ElMessage({
      message,
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(new Error(message))
  }
)

export default service
