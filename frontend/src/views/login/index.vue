<template>
  <div class="login-container">
    <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form" autocomplete="on"
      label-position="left">
      <div class="title-container">
        <h3 class="title">电池溯源系统{{ isLogin ? '登录' : '注册' }}</h3>
      </div>

      <el-form-item prop="username">
        <el-input v-model="loginForm.username" placeholder="用户名" name="username" type="text" tabindex="1"
          autocomplete="on">
          <template #prefix>
            <el-icon>
              <User />
            </el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="password">
        <el-input v-model="loginForm.password" type="password" placeholder="密码" name="password" tabindex="2"
          autocomplete="on" show-password @keyup.enter="handleLogin">
          <template #prefix>
            <el-icon>
              <Lock />
            </el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item v-if="!isLogin" prop="checkPassword">
        <el-input v-model="loginForm.checkPassword" type="password" placeholder="确认密码" name="checkPassword" tabindex="3"
          autocomplete="on" show-password @keyup.enter="handleRegister">
          <template #prefix>
            <el-icon>
              <Lock />
            </el-icon>
          </template>
        </el-input>
      </el-form-item>

      <div class="tips">
        <span @click="toggleMode" style="cursor: pointer; color: #409EFF; font-size: 14px;">
          {{ isLogin ? '没有账号？点击注册' : '已有账号？点击登录' }}
        </span>
      </div>

      <el-button :loading="loading" type="primary" style="width:100%;margin-bottom:30px;margin-top:20px;"
        @click.prevent="isLogin ? handleLogin() : handleRegister()">
        {{ isLogin ? '登录' : '注册' }}
      </el-button>

    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const isLogin = ref(true)

const loginForm = reactive({
  username: '',
  password: '',
  checkPassword: ''
})

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== loginForm.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const loginRules = {
  username: [{ required: true, trigger: 'blur', message: '请输入用户名' }],
  password: [{ required: true, trigger: 'blur', message: '请输入密码' }],
  checkPassword: [{ validator: validatePass2, trigger: 'blur' }]
}

const loading = ref(false)
const loginFormRef = ref(null)

const toggleMode = () => {
  isLogin.value = !isLogin.value
  loginFormRef.value.resetFields()
}

const handleLogin = () => {
  loginFormRef.value.validate(valid => {
    if (valid) {
      loading.value = true
      userStore.login(loginForm).then(() => {
        router.push({ path: '/' })
        loading.value = false
      }).catch(() => {
        loading.value = false
      })
    } else {
      console.log('error submit!!')
      return false
    }
  })
}

const handleRegister = () => {
  loginFormRef.value.validate(valid => {
    if (valid) {
      loading.value = true
      userStore.register(loginForm).then(() => {
        ElMessage.success('注册成功，请登录')
        isLogin.value = true
        loading.value = false
      }).catch(() => {
        loading.value = false
      })
    } else {
      console.log('error submit!!')
      return false
    }
  })
}
</script>

<style lang="scss">
$bg: #2d3a4b;
$dark_gray: #889aa4;
$light_gray: #eee;

.login-container {
  min-height: 100vh;
  width: 100%;
  background-color: $bg;
  overflow: hidden;

  .login-form {
    position: relative;
    width: 520px;
    max-width: 100%;
    padding: 160px 35px 0;
    margin: 0 auto;
    overflow: hidden;
  }

  .tips {
    font-size: 14px;
    color: #fff;
    margin-bottom: 10px;
    text-align: right;
  }

  .title-container {
    position: relative;

    .title {
      font-size: 26px;
      color: $light_gray;
      margin: 0px auto 40px auto;
      text-align: center;
      font-weight: bold;
    }
  }

  .el-form-item {
    border: 1px solid rgba(255, 255, 255, 0.1);
    background: rgba(0, 0, 0, 0.1);
    border-radius: 5px;
    color: #454545;
  }

  .el-input {
    height: 47px;

    .el-input__wrapper {
      background: transparent;
      box-shadow: none;

      &.is-focus {
        box-shadow: none;
      }
    }

    input {
      background: transparent;
      border: 0px;
      -webkit-appearance: none;
      border-radius: 0px;
      padding: 12px 5px 12px 15px;
      color: $light_gray;
      height: 47px;

      &:-webkit-autofill {
        box-shadow: 0 0 0px 1000px $bg inset !important;
        -webkit-text-fill-color: #fff !important;
      }
    }
  }
}
</style>
