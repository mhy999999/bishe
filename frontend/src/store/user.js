import { defineStore } from 'pinia'
import { login, logout, getInfo, register } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    name: '',
    avatar: '',
    roles: [],
    permissions: []
  }),
  actions: {
    // user login
    login(userInfo) {
      const { username, password } = userInfo
      return new Promise((resolve, reject) => {
        login({ username: username.trim(), password: password }).then(data => {
          this.token = data.token
          setToken(data.token)
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // user register
    register(userInfo) {
      return new Promise((resolve, reject) => {
        register(userInfo).then(data => {
          resolve(data)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // get user info
    getInfo() {
      return new Promise((resolve, reject) => {
        getInfo().then(data => {
          if (!data) {
            reject('Verification failed, please Login again.')
            return
          }

          const { roles, name, avatar, permissions } = data

          if (!roles || roles.length <= 0) {
            reject('getInfo: roles must be a non-null array!')
            return
          }

          this.roles = roles
          this.permissions = permissions
          this.name = name
          this.avatar = avatar
          resolve(data)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // user logout
    logout() {
      return new Promise((resolve, reject) => {
        logout().then(() => {
          this.token = ''
          this.roles = []
          this.permissions = []
          removeToken()
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // remove token
    resetToken() {
      return new Promise(resolve => {
        this.token = ''
        this.roles = []
        this.permissions = []
        removeToken()
        resolve()
      })
    }
  }
})
