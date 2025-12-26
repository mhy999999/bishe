<template>
  <el-menu active-text-color="#409EFF" background-color="#304156" class="el-menu-vertical-demo"
    :default-active="activeMenu" text-color="#bfcbd9" :unique-opened="false" :collapse-transition="false"
    mode="vertical" router>
    <sidebar-item v-for="route in permission_routes" :key="route.path" :item="route" :base-path="route.path" />
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { usePermissionStore } from '@/store/permission'
import SidebarItem from './SidebarItem.vue'
import { Odometer, Setting, User, Link, Box, UserFilled, Menu, OfficeBuilding, List, Collection, Checked, Money, Connection, Sort, FirstAidKit, DataLine, Document, Warning, Refresh, Edit, Search } from '@element-plus/icons-vue'

const route = useRoute()
const permissionStore = usePermissionStore()
const permission_routes = computed(() => permissionStore.routes)

const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta.activeMenu) {
    return meta.activeMenu
  }
  return path
})
</script>

<style lang="scss" scoped>
.el-menu-vertical-demo {
  border-right: none;
}
</style>
