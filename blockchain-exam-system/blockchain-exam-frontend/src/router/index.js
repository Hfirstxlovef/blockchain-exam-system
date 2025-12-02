import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requireAuth: false },
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    meta: { requireAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' },
      },
      // 教师端路由
      {
        path: 'papers',
        name: 'PaperList',
        component: () => import('@/views/papers/List.vue'),
        meta: { title: '我的试卷', roles: ['teacher'] },
      },
      {
        path: 'papers/create',
        name: 'PaperCreate',
        component: () => import('@/views/papers/Create.vue'),
        meta: { title: '创建试卷', roles: ['teacher'] },
      },
      {
        path: 'papers/:id',
        name: 'PaperDetail',
        component: () => import('@/views/papers/Detail.vue'),
        meta: { title: '试卷详情', roles: ['teacher', 'dept_admin', 'college_admin'] },
      },
      // 管理员路由
      {
        path: 'papers/all',
        name: 'AllPapers',
        component: () => import('@/views/papers/All.vue'),
        meta: { title: '所有试卷', roles: ['dept_admin', 'college_admin'] },
      },
      {
        path: 'approval',
        name: 'ApprovalList',
        component: () => import('@/views/approval/List.vue'),
        meta: { title: '待审批试卷', roles: ['dept_admin', 'college_admin'] },
      },
      {
        path: 'approval/:id',
        name: 'ApprovalDetail',
        component: () => import('@/views/approval/Detail.vue'),
        meta: { title: '审批详情', roles: ['dept_admin', 'college_admin'] },
      },
      // 区块链相关路由（所有用户可访问）
      {
        path: 'blockchain',
        name: 'Blockchain',
        component: () => import('@/views/blockchain/Index.vue'),
        meta: { title: '区块链浏览器' },
      },
      {
        path: 'ledger',
        name: 'Ledger',
        component: () => import('@/views/blockchain/Ledger.vue'),
        meta: { title: '审计账本' },
      },
      {
        path: 'nodes',
        name: 'Nodes',
        component: () => import('@/views/nodes/Index.vue'),
        meta: { title: '节点状态' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const token = userStore.token

  // 设置页面标题
  document.title = to.meta.title
    ? `${to.meta.title} - 试卷审批系统`
    : '试卷审批系统'

  // 需要登录的页面
  if (to.meta.requireAuth !== false) {
    if (!token) {
      ElMessage.warning('请先登录')
      next('/login')
      return
    }

    // 权限检查
    if (to.meta.roles && to.meta.roles.length > 0) {
      const userRole = userStore.userInfo?.role
      if (!to.meta.roles.includes(userRole)) {
        ElMessage.error('权限不足')
        next('/')
        return
      }
    }
  }

  next()
})

export default router
