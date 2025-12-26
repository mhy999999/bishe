<template>
  <div class="navbar">
    <div class="right-menu">
      <el-dropdown class="avatar-container" trigger="click">
        <div class="avatar-wrapper">
          <span class="user-name">{{ name }}</span>
          <el-icon class="el-icon--right"><arrow-down /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu class="user-dropdown">
            <router-link to="/">
              <el-dropdown-item>首页</el-dropdown-item>
            </router-link>
            <el-dropdown-item divided @click="logout">
              <span style="display:block;">退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/store/user'
import { ArrowDown } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const name = computed(() => userStore.name || 'Admin')

const logout = async () => {
  await userStore.logout()
  router.push(`/login?redirect=${router.currentRoute.value.fullPath}`)
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding-right: 20px;

  .right-menu {
    cursor: pointer;
    .avatar-wrapper {
      display: flex;
      align-items: center;
      .user-name {
        margin-right: 5px;
      }
    }
  }
}
</style>
