<template>
  <div class="layout-container">
    <!-- Apple风格侧边栏 -->
    <aside :class="['layout-aside', { 'is-collapse': isCollapse }]">
      <!-- Logo区域 -->
      <div class="logo-section">
        <div class="logo-icon">
          <el-icon :size="20"><Link /></el-icon>
        </div>
        <transition name="fade">
          <span v-show="!isCollapse" class="logo-text">区块链试卷</span>
        </transition>
      </div>

      <!-- 导航菜单 -->
      <nav class="nav-menu">
        <!-- 教师菜单 -->
        <template v-if="userStore.isTeacher">
          <router-link to="/" class="nav-item" :class="{ active: isActive('/') }">
            <el-icon><HomeFilled /></el-icon>
            <span v-show="!isCollapse">首页</span>
          </router-link>
          <router-link to="/papers" class="nav-item" :class="{ active: isActive('/papers') }">
            <el-icon><Document /></el-icon>
            <span v-show="!isCollapse">我的试卷</span>
          </router-link>
          <router-link to="/papers/create" class="nav-item" :class="{ active: isActive('/papers/create') }">
            <el-icon><Plus /></el-icon>
            <span v-show="!isCollapse">创建试卷</span>
          </router-link>
        </template>

        <!-- 管理员菜单 -->
        <template v-if="userStore.isAdmin">
          <router-link to="/" class="nav-item" :class="{ active: isActive('/') }">
            <el-icon><HomeFilled /></el-icon>
            <span v-show="!isCollapse">首页</span>
          </router-link>
          <router-link to="/approval" class="nav-item" :class="{ active: isActive('/approval') }">
            <el-icon><CircleCheck /></el-icon>
            <span v-show="!isCollapse">待审批</span>
          </router-link>
          <router-link to="/papers/all" class="nav-item" :class="{ active: isActive('/papers/all') }">
            <el-icon><Document /></el-icon>
            <span v-show="!isCollapse">所有试卷</span>
          </router-link>
        </template>

        <!-- 分隔线 -->
        <div class="nav-divider"></div>

        <!-- 区块链菜单（所有用户可见） -->
        <router-link to="/blockchain" class="nav-item" :class="{ active: isActive('/blockchain') }">
          <el-icon><Connection /></el-icon>
          <span v-show="!isCollapse">区块链</span>
        </router-link>
        <router-link to="/ledger" class="nav-item" :class="{ active: isActive('/ledger') }">
          <el-icon><Notebook /></el-icon>
          <span v-show="!isCollapse">审计账本</span>
        </router-link>
        <router-link to="/nodes" class="nav-item" :class="{ active: isActive('/nodes') }">
          <el-icon><Monitor /></el-icon>
          <span v-show="!isCollapse">节点状态</span>
        </router-link>
      </nav>

      <!-- 折叠按钮 -->
      <div class="collapse-btn" @click="toggleCollapse">
        <el-icon>
          <DArrowLeft v-if="!isCollapse" />
          <DArrowRight v-else />
        </el-icon>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="layout-main">
      <!-- Apple风格顶部导航 -->
      <header class="layout-header">
        <div class="header-left">
          <h1 class="page-title">{{ pageTitle }}</h1>
        </div>

        <div class="header-right">
          <!-- 角色徽章 -->
          <div class="role-badge" :class="roleBadgeClass">
            {{ roleLabel }}
          </div>

          <!-- 用户下拉菜单 -->
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-avatar-btn">
              <div class="avatar">
                {{ userStore.realName?.charAt(0) || 'U' }}
              </div>
              <span class="user-name">{{ userStore.realName || userStore.username }}</span>
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <div class="dropdown-user-info">
                  <div class="dropdown-avatar">
                    {{ userStore.realName?.charAt(0) || 'U' }}
                  </div>
                  <div class="dropdown-info">
                    <p class="dropdown-name">{{ userStore.realName }}</p>
                    <p class="dropdown-dept">{{ userStore.department }}</p>
                  </div>
                </div>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区域 -->
      <div class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Document,
  HomeFilled,
  Plus,
  CircleCheck,
  Connection,
  Monitor,
  Link,
  SwitchButton,
  ArrowDown,
  DArrowLeft,
  DArrowRight,
  Notebook
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { logout } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 侧边栏折叠状态
const isCollapse = ref(false)

// 页面标题映射
const pageTitleMap = {
  '/': '仪表盘',
  '/papers': '我的试卷',
  '/papers/create': '创建试卷',
  '/papers/all': '所有试卷',
  '/approval': '待审批试卷',
  '/blockchain': '区块链浏览器',
  '/ledger': '审计账本',
  '/nodes': '节点状态'
}

// 页面标题
const pageTitle = computed(() => {
  const path = route.path
  return route.meta?.title || pageTitleMap[path] || '试卷审批系统'
})

// 判断当前路由是否激活
const isActive = (path) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

// 角色标签
const roleLabel = computed(() => {
  const roleMap = {
    'teacher': '教师',
    'dept_admin': '系主任',
    'college_admin': '院长'
  }
  return roleMap[userStore.role] || '用户'
})

// 角色徽章样式
const roleBadgeClass = computed(() => {
  const classMap = {
    'teacher': 'badge-green',
    'dept_admin': 'badge-orange',
    'college_admin': 'badge-purple'
  }
  return classMap[userStore.role] || 'badge-gray'
})

/**
 * 切换侧边栏折叠
 */
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

/**
 * 处理下拉菜单命令
 */
const handleCommand = async (command) => {
  if (command === 'logout') {
    await handleLogout()
  }
}

/**
 * 退出登录
 */
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await logout().catch(() => {})
    userStore.clearAuth()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch (error) {
    // 用户取消
  }
}
</script>

<style scoped>
/* ========== Apple 高雅白 布局样式 ========== */

.layout-container {
  display: flex;
  height: 100vh;
  background-color: var(--apple-gray-1, #f5f5f7);
}

/* ========== 侧边栏 ========== */
.layout-aside {
  display: flex;
  flex-direction: column;
  width: 240px;
  background: #ffffff;
  border-right: 0.5px solid var(--apple-border, rgba(0, 0, 0, 0.08));
  transition: width 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.layout-aside.is-collapse {
  width: 72px;
}

/* Logo区域 */
.logo-section {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 20px;
  gap: 12px;
  border-bottom: 0.5px solid var(--apple-border, rgba(0, 0, 0, 0.08));
}

.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #0071e3 0%, #005bb5 100%);
  border-radius: 8px;
  color: white;
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-text-primary, #1d1d1f);
  white-space: nowrap;
}

/* 导航菜单 */
.nav-menu {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  height: 44px;
  padding: 0 12px;
  margin-bottom: 4px;
  border-radius: 10px;
  color: var(--apple-text-secondary, #6e6e73);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  gap: 12px;
  transition: all 0.2s ease;
}

.nav-item:hover {
  background-color: var(--apple-gray-1, #f5f5f7);
  color: var(--apple-text-primary, #1d1d1f);
}

.nav-item.active {
  background-color: rgba(0, 113, 227, 0.1);
  color: #0071e3;
}

.nav-item .el-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.nav-divider {
  height: 1px;
  background-color: var(--apple-gray-2, #e8e8ed);
  margin: 16px 0;
}

/* 折叠按钮 */
.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  border-top: 0.5px solid var(--apple-border, rgba(0, 0, 0, 0.08));
  color: var(--apple-text-tertiary, #86868b);
  cursor: pointer;
  transition: all 0.2s ease;
}

.collapse-btn:hover {
  background-color: var(--apple-gray-1, #f5f5f7);
  color: var(--apple-text-primary, #1d1d1f);
}

/* ========== 主内容区 ========== */
.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 顶部导航 */
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 32px;
  background: #ffffff;
  border-bottom: 0.5px solid var(--apple-border, rgba(0, 0, 0, 0.08));
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--apple-text-primary, #1d1d1f);
  letter-spacing: -0.3px;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 角色徽章 */
.role-badge {
  padding: 4px 12px;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 500;
}

.badge-green {
  background-color: rgba(52, 199, 89, 0.12);
  color: #34c759;
}

.badge-orange {
  background-color: rgba(255, 149, 0, 0.12);
  color: #ff9500;
}

.badge-purple {
  background-color: rgba(175, 82, 222, 0.12);
  color: #af52de;
}

.badge-gray {
  background-color: var(--apple-gray-2, #e8e8ed);
  color: var(--apple-text-secondary, #6e6e73);
}

/* 用户头像按钮 */
.user-avatar-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 100px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-avatar-btn:hover {
  background-color: var(--apple-gray-1, #f5f5f7);
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0071e3 0%, #005bb5 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--apple-text-primary, #1d1d1f);
}

.dropdown-icon {
  font-size: 12px;
  color: var(--apple-text-tertiary, #86868b);
}

/* 下拉菜单用户信息 */
.dropdown-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--apple-gray-2, #e8e8ed);
}

.dropdown-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0071e3 0%, #005bb5 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
}

.dropdown-info {
  flex: 1;
}

.dropdown-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--apple-text-primary, #1d1d1f);
  margin: 0 0 2px 0;
}

.dropdown-dept {
  font-size: 12px;
  color: var(--apple-text-tertiary, #86868b);
  margin: 0;
}

/* 内容区域 */
.layout-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
  background-color: var(--apple-gray-1, #f5f5f7);
}

/* ========== 页面切换动画 ========== */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Logo文字淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
