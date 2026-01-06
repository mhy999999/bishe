<template>
  <div v-if="!item.hidden">
    <template
      v-if="hasOneShowingChild(item.children, item) && (!onlyOneChild.children || onlyOneChild.noShowingChildren) && !item.alwaysShow">
      <el-menu-item :index="getMenuItemIndex(item, onlyOneChild)" :class="{ 'submenu-title-noDropdown': !isNest }">
        <el-icon v-if="onlyOneChild.meta && onlyOneChild.meta.icon">
          <component :is="onlyOneChild.meta.icon" />
        </el-icon>
        <template #title>
          <span>{{ onlyOneChild.meta?.title }}</span>
        </template>
      </el-menu-item>
    </template>

    <el-sub-menu v-else :index="resolvePath(item.path)" popper-append-to-body>
      <template #title>
        <el-icon v-if="item.meta && item.meta.icon">
          <component :is="item.meta.icon" />
        </el-icon>
        <span>{{ item.meta?.title }}</span>
      </template>
      <sidebar-item v-for="child in item.children" :key="child.path" :is-nest="true" :item="child"
        :base-path="resolvePath(item.path)" />
    </el-sub-menu>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  isNest: {
    type: Boolean,
    default: false
  },
  basePath: {
    type: String,
    default: ''
  }
})

const onlyOneChild = ref(null)

const hasOneShowingChild = (children = [], parent) => {
  const showingChildren = children.filter(item => {
    if (item.hidden) {
      return false
    } else {
      // Temp set(will be used if only has one showing child)
      onlyOneChild.value = item
      return true
    }
  })

  // When there is only one child router, the child router is displayed by default
  if (showingChildren.length === 1) {
    return true
  }

  // Show parent if there are no child router to display
  if (showingChildren.length === 0) {
    onlyOneChild.value = { ...parent, noShowingChildren: true }
    return true
  }

  return false
}

const resolvePath = (routePath) => {
  if (isExternal(routePath)) {
    return routePath
  }
  if (isExternal(props.basePath)) {
    return props.basePath
  }
  if (!routePath) {
    return props.basePath
  }
  if (routePath.startsWith('/')) {
    return routePath
  }
  if (props.basePath === '/') {
    return props.basePath + routePath
  }
  return props.basePath + '/' + routePath
}

const resolveFullPath = (parentPath, childPath) => {
  const base = resolvePath(parentPath)
  const child = String(childPath || '')
  if (!child) return base
  if (isExternal(child)) return child
  if (child.startsWith('/')) return child
  if (isExternal(base)) return base
  if (!base || base === '/') return '/' + child
  return base.replace(/\/$/, '') + '/' + child
}

const getMenuItemIndex = (parentItem, resolvedChild) => {
  if (resolvedChild?.noShowingChildren) {
    return resolvePath(parentItem?.path)
  }
  return resolveFullPath(parentItem?.path, resolvedChild?.path)
}

function isExternal(path) {
  return /^(https?:|mailto:|tel:)/.test(path)
}
</script>
