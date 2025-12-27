import { h } from 'vue'
import { createRouter, createWebHistory, RouterView } from 'vue-router'
import Layout from '@/layout/index.vue'

const ParentView = {
  name: 'ParentView',
  render: () => h(RouterView)
}

export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    hidden: true
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'Odometer', affix: true }
      }
    ]
  },
  {
    path: '/404',
    component: () => import('@/views/error-page/404.vue'),
    hidden: true
  }
]

export const asyncRoutes = [
  {
    path: '/system',
    component: Layout,
    redirect: '/system/user',
    name: 'System',
    meta: { title: '系统管理', icon: 'Setting', roles: ['admin'] },
    children: [
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'User', roles: ['admin'] }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'UserFilled', roles: ['admin'] }
      },
      {
        path: 'dept',
        name: 'Dept',
        component: () => import('@/views/system/dept/index.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding', roles: ['admin'] }
      }
    ]
  },
  {
    path: '/battery',
    component: Layout,
    redirect: '/battery/info',
    name: 'Battery',
    meta: { title: '电池管理', icon: 'Box', roles: ['admin', 'manufacturer'] },
    children: [
      {
        path: 'info',
        name: 'BatteryInfo',
        component: () => import('@/views/battery/info/index.vue'),
        meta: { title: '电池信息', icon: 'List', roles: ['admin', 'manufacturer'] }
      },
      {
        path: 'batch',
        name: 'BatteryBatch',
        component: () => import('@/views/battery/batch/index.vue'),
        meta: { title: '生产批次', icon: 'Collection', roles: ['admin', 'manufacturer'] }
      },
      {
        path: 'quality',
        name: 'BatteryQuality',
        component: ParentView,
        redirect: '/battery/quality/pending',
        meta: { title: '出厂质检', icon: 'Checked', roles: ['admin', 'manufacturer'] },
        children: [
          {
            path: 'pending',
            name: 'QualityPending',
            component: () => import('@/views/battery/quality/index.vue'),
            meta: { title: '待质检任务', icon: 'Clock' }
          },
          {
            path: 'audit',
            name: 'QualityAudit',
            component: () => import('@/views/battery/quality/audit.vue'),
            meta: { title: '质检审核', icon: 'Stamp' }
          },
          {
            path: 'list',
            name: 'QualityRecord',
            component: () => import('@/views/battery/quality/index.vue'),
            meta: { title: '质检记录', icon: 'List' }
          }
        ]
      },
      {
        path: 'maintenance',
        name: 'BatteryMaintenance',
        component: ParentView,
        redirect: '/battery/maintenance/list',
        meta: { title: '电池维修', icon: 'Tools', roles: ['admin', 'maintainer', 'maintenance'] },
        children: [
          {
            path: 'list',
            name: 'MaintenanceRecordList',
            component: () => import('@/views/battery/maintenance/index.vue'),
            meta: { title: '维修记录', icon: 'List' }
          },
          {
            path: 'audit',
            name: 'MaintenanceAudit',
            component: () => import('@/views/battery/maintenance/audit.vue'),
            meta: { title: '维修审核', icon: 'Stamp' }
          }
        ]
      },
      {
        path: 'sales',
        name: 'SalesRecord',
        component: ParentView,
        redirect: '/battery/sales/list',
        meta: { title: '电池销售', icon: 'ShoppingCart', roles: ['admin', 'sales', 'dealer'] },
        children: [
          {
            path: 'list',
            name: 'SalesRecordList',
            component: () => import('@/views/battery/sales/index.vue'),
            meta: { title: '销售记录', icon: 'List' }
          },
          {
            path: 'audit',
            name: 'SalesAudit',
            component: () => import('@/views/battery/sales/audit.vue'),
            meta: { title: '销售审核', icon: 'Stamp' }
          }
        ]
      }
    ]
  },
  {
    path: '/sales',
    component: Layout,
    redirect: '/sales/binding',
    name: 'Sales',
    meta: { title: '渠道销售', icon: 'Money', roles: ['admin', 'dealer'] },
    children: [
      {
        path: 'binding',
        name: 'VehicleBinding',
        component: () => import('@/views/sales/binding/index.vue'),
        meta: { title: '车电绑定', icon: 'Connection', roles: ['admin', 'dealer'] }
      },
      {
        path: 'transfer',
        name: 'TransferRecord',
        component: () => import('@/views/sales/transfer/index.vue'),
        meta: { title: '流转记录', icon: 'Sort', roles: ['admin', 'dealer'] }
      }
    ]
  },
  {
    path: '/maintenance',
    component: Layout,
    redirect: '/maintenance/monitor',
    name: 'MaintenanceManage',
    meta: { title: '售后服务', icon: 'FirstAidKit', roles: ['admin', 'maintenance'] },
    children: [
      {
        path: 'monitor',
        name: 'HealthMonitor',
        component: () => import('@/views/maintenance/monitor/index.vue'),
        meta: { title: '健康监测', icon: 'DataLine', roles: ['admin', 'maintenance'] }
      },
      {
        path: 'record',
        name: 'MaintenanceRecord',
        component: () => import('@/views/maintenance/record/index.vue'),
        meta: { title: '维修记录', icon: 'Document', roles: ['admin', 'maintenance'] }
      },
      {
        path: 'alarm',
        name: 'BatteryAlarm',
        component: () => import('@/views/maintenance/alarm/index.vue'),
        meta: { title: '故障报警', icon: 'Warning', roles: ['admin', 'maintenance'] }
      }
    ]
  },
  {
    path: '/recycling',
    component: Layout,
    redirect: '/recycling/appraisal',
    name: 'Recycling',
    meta: { title: '回收利用', icon: 'Refresh', roles: ['admin', 'recycler'] },
    children: [
      {
        path: 'appraisal',
        name: 'RecyclingAppraisal',
        component: () => import('@/views/recycling/appraisal/index.vue'),
        meta: { title: '回收评估', icon: 'Edit', roles: ['admin', 'recycler'] }
      }
    ]
  },
  {
    path: '/trace',
    component: Layout,
    children: [
      {
        path: 'index',
        name: 'Trace',
        component: () => import('@/views/trace/index.vue'),
        meta: { title: '溯源查询', icon: 'Search', roles: ['admin', 'manufacturer', 'dealer', 'owner', 'maintenance', 'recycler'] }
      }
    ]
  },
  // 404 page must be placed at the end !!!
  { path: '/:pathMatch(.*)*', redirect: '/404', hidden: true }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

export default router
