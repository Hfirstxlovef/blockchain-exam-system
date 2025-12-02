<template>
  <div class="dashboard-container">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card" shadow="hover">
      <div class="welcome-content">
        <div class="welcome-text">
          <h2>欢迎回来，{{ userStore.realName }}！</h2>
          <p>{{ welcomeMessage }}</p>
        </div>
        <div class="welcome-icon">
          <el-icon :size="80" color="#409eff"><UserFilled /></el-icon>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6" v-for="stat in statistics" :key="stat.key">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" :style="{ background: stat.color }">
              <el-icon :size="32">
                <component :is="stat.icon" />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-card class="quick-actions" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>快捷操作</span>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" v-for="action in quickActions" :key="action.path">
          <div class="action-item" @click="handleAction(action.path)">
            <el-icon :size="40" :color="action.color">
              <component :is="action.icon" />
            </el-icon>
            <div class="action-text">
              <div class="action-title">{{ action.title }}</div>
              <div class="action-desc">{{ action.desc }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 系统信息 -->
    <el-card class="system-info" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>系统信息</span>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="系统名称">试卷审批系统</el-descriptions-item>
        <el-descriptions-item label="版本号">v1.0.0</el-descriptions-item>
        <el-descriptions-item label="用户角色">{{ roleLabel }}</el-descriptions-item>
        <el-descriptions-item label="所属院系">{{ userStore.department || '无' }}</el-descriptions-item>
        <el-descriptions-item label="安全特性" :span="2">
          <el-tag size="small" class="security-tag">数据传输加密</el-tag>
          <el-tag size="small" class="security-tag">数字签名验证</el-tag>
          <el-tag size="small" class="security-tag">权限访问控制</el-tag>
          <el-tag size="small" class="security-tag">审计日志记录</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  UserFilled,
  Document,
  Plus,
  CircleCheck,
  Clock,
  Finished,
  CloseBold
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getStatistics } from '@/api/paper'

const router = useRouter()
const userStore = useUserStore()

// 统计数据
const statsData = ref({
  total: 0,
  draft: 0,
  pending: 0,
  deptApproved: 0,
  collegeApproved: 0,
  rejected: 0
})

// 欢迎消息
const welcomeMessage = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了，注意休息'
  if (hour < 9) return '早上好，开始新的一天'
  if (hour < 12) return '上午好，工作顺利'
  if (hour < 14) return '中午好，记得休息'
  if (hour < 18) return '下午好，继续加油'
  if (hour < 22) return '晚上好，辛苦了'
  return '夜深了，早点休息'
})

// 角色标签
const roleLabel = computed(() => {
  const roleMap = {
    'teacher': '教师',
    'dept_admin': '系管理员',
    'college_admin': '院管理员'
  }
  return roleMap[userStore.role] || '用户'
})

// 统计卡片数据
const statistics = computed(() => {
  if (userStore.isTeacher) {
    return [
      { key: 'total', label: '全部试卷', value: statsData.value.total, icon: Document, color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
      { key: 'draft', label: '草稿', value: statsData.value.draft, icon: Clock, color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
      { key: 'pending', label: '审批中', value: statsData.value.pending + statsData.value.deptApproved, icon: Clock, color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
      { key: 'approved', label: '已通过', value: statsData.value.collegeApproved, icon: Finished, color: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' }
    ]
  } else {
    return [
      { key: 'pending', label: '待审批', value: statsData.value.pending, icon: Clock, color: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)' },
      { key: 'approved', label: '已审批', value: statsData.value.deptApproved + statsData.value.collegeApproved, icon: CircleCheck, color: 'linear-gradient(135deg, #30cfd0 0%, #330867 100%)' },
      { key: 'rejected', label: '已驳回', value: statsData.value.rejected, icon: CloseBold, color: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)' },
      { key: 'total', label: '全部试卷', value: statsData.value.total, icon: Document, color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }
    ]
  }
})

// 快捷操作
const quickActions = computed(() => {
  if (userStore.isTeacher) {
    return [
      { title: '创建试卷', desc: '开始创建新的试卷', path: '/papers/create', icon: Plus, color: '#67c23a' },
      { title: '我的试卷', desc: '查看所有试卷', path: '/papers', icon: Document, color: '#409eff' }
    ]
  } else {
    return [
      { title: '待审批试卷', desc: '查看待审批的试卷', path: '/approval', icon: CircleCheck, color: '#e6a23c' },
      { title: '所有试卷', desc: '查看所有试卷', path: '/papers/all', icon: Document, color: '#409eff' }
    ]
  }
})

/**
 * 加载统计数据
 */
const loadStatistics = async () => {
  try {
    const response = await getStatistics()
    if (response.code === 200 && response.data) {
      statsData.value = response.data
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

/**
 * 处理快捷操作
 */
const handleAction = (path) => {
  router.push(path)
}

// 组件挂载时加载数据
onMounted(() => {
  // 所有角色都加载统计数据
  loadStatistics()
})
</script>

<style scoped>
/* ========== Apple 高雅白 UI 设计系统 ========== */

/* ========== Dashboard 容器 + CSS变量定义 ========== */
.dashboard-container {
  /* 背景色系统 */
  --apple-white: #fbfbfd;
  --apple-gray-1: #f5f5f7;
  --apple-gray-2: #e8e8ed;
  --apple-gray-3: #d2d2d7;
  --apple-border: rgba(0, 0, 0, 0.08);

  /* 文字色系统 */
  --apple-text-primary: #1d1d1f;
  --apple-text-secondary: #6e6e73;
  --apple-text-tertiary: #86868b;

  /* 品牌色 */
  --apple-blue: #0071e3;
  --apple-blue-hover: #0077ed;
  --apple-blue-light: rgba(0, 113, 227, 0.1);

  /* 间距系统 (8px基准) */
  --spacing-xs: 8px;
  --spacing-sm: 16px;
  --spacing-md: 24px;
  --spacing-lg: 32px;
  --spacing-xl: 48px;

  /* 阴影系统 */
  --shadow-card:
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 8px 32px rgba(0, 0, 0, 0.03);
  --shadow-card-hover:
    0 8px 24px rgba(0, 0, 0, 0.08),
    0 16px 48px rgba(0, 0, 0, 0.06);

  /* 圆角系统 */
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;

  /* 字体系统 */
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text",
                "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  --font-weight-regular: 400;
  --font-weight-medium: 500;
  --font-weight-semibold: 600;

  /* 动画系统 */
  --ease-out: cubic-bezier(0.25, 0.46, 0.45, 0.94);
  --duration-base: 0.3s;

  /* 布局样式 */
  padding: var(--spacing-lg);
  min-height: 100vh;
  background: linear-gradient(135deg, #fafafa 0%, #f0f0f2 100%);
  font-family: var(--font-apple);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* ========== 欢迎卡片 ========== */
.dashboard-container :deep(.welcome-card) {
  margin-bottom: var(--spacing-lg);
  border-radius: var(--radius-xl) !important;
  border: 0.5px solid var(--apple-border) !important;
  box-shadow: var(--shadow-card) !important;
  transition: all var(--duration-base) var(--ease-out);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
}

.dashboard-container :deep(.welcome-card:hover) {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card-hover) !important;
}

.dashboard-container :deep(.welcome-card .el-card__body) {
  padding: var(--spacing-lg);
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-text h2 {
  margin: 0 0 var(--spacing-sm) 0;
  font-size: 32px;
  font-weight: var(--font-weight-semibold);
  letter-spacing: -0.5px;
  color: var(--apple-text-primary);
  line-height: 1.2;
}

.welcome-text p {
  margin: 0;
  font-size: 16px;
  font-weight: var(--font-weight-regular);
  color: var(--apple-text-secondary);
  line-height: 1.4;
}

.welcome-icon {
  opacity: 0.15;
  transition: opacity var(--duration-base) var(--ease-out);
}

.dashboard-container :deep(.welcome-card:hover) .welcome-icon {
  opacity: 0.25;
}

/* ========== 统计卡片行 ========== */
.stats-row {
  margin-bottom: var(--spacing-lg);
}

/* ========== 统计卡片 ========== */
.dashboard-container :deep(.stat-card) {
  margin-bottom: var(--spacing-md);
  border-radius: var(--radius-xl) !important;
  border: 0.5px solid var(--apple-border) !important;
  box-shadow: var(--shadow-card) !important;
  cursor: pointer;
  transition: all var(--duration-base) var(--ease-out);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
}

.dashboard-container :deep(.stat-card:hover) {
  transform: translateY(-6px) scale(1.02);
  box-shadow: var(--shadow-card-hover) !important;
}

.dashboard-container :deep(.stat-card .el-card__body) {
  padding: var(--spacing-md);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.stat-icon {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
  color: white;
  box-shadow: 0 4px 16px rgba(0, 113, 227, 0.2);
  transition: all var(--duration-base) var(--ease-out);
}

.dashboard-container :deep(.stat-card:hover) .stat-icon {
  transform: scale(1.1);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 36px;
  font-weight: var(--font-weight-semibold);
  color: var(--apple-text-primary);
  margin-bottom: var(--spacing-xs);
  line-height: 1;
  letter-spacing: -1px;
}

.stat-label {
  font-size: 15px;
  font-weight: var(--font-weight-medium);
  color: var(--apple-text-secondary);
}

/* ========== 快捷操作卡片 ========== */
.dashboard-container :deep(.quick-actions) {
  margin-bottom: var(--spacing-lg);
  border-radius: var(--radius-xl) !important;
  border: 0.5px solid var(--apple-border) !important;
  box-shadow: var(--shadow-card) !important;
  transition: all var(--duration-base) var(--ease-out);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
}

.dashboard-container :deep(.quick-actions:hover) {
  box-shadow: var(--shadow-card-hover) !important;
}

.dashboard-container :deep(.quick-actions .el-card__header) {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--apple-gray-2);
}

.dashboard-container :deep(.quick-actions .el-card__body) {
  padding: var(--spacing-lg);
}

.card-header {
  font-weight: var(--font-weight-semibold);
  font-size: 18px;
  color: var(--apple-text-primary);
  letter-spacing: -0.3px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  border: 1.5px solid var(--apple-gray-2);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-base) var(--ease-out);
  margin-bottom: var(--spacing-md);
  background: var(--apple-gray-1);
}

.action-item:hover {
  border-color: var(--apple-blue);
  background: var(--apple-blue-light);
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 113, 227, 0.15);
}

.action-text {
  flex: 1;
}

.action-title {
  font-size: 17px;
  font-weight: var(--font-weight-semibold);
  color: var(--apple-text-primary);
  margin-bottom: var(--spacing-xs);
  letter-spacing: -0.2px;
}

.action-desc {
  font-size: 14px;
  font-weight: var(--font-weight-regular);
  color: var(--apple-text-secondary);
}

/* ========== 系统信息卡片 ========== */
.dashboard-container :deep(.system-info) {
  margin-bottom: var(--spacing-lg);
  border-radius: var(--radius-xl) !important;
  border: 0.5px solid var(--apple-border) !important;
  box-shadow: var(--shadow-card) !important;
  transition: all var(--duration-base) var(--ease-out);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
}

.dashboard-container :deep(.system-info:hover) {
  box-shadow: var(--shadow-card-hover) !important;
}

.dashboard-container :deep(.system-info .el-card__header) {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--apple-gray-2);
}

.dashboard-container :deep(.system-info .el-card__body) {
  padding: var(--spacing-lg);
}

.dashboard-container :deep(.el-descriptions__body) {
  background-color: transparent;
}

.dashboard-container :deep(.el-descriptions__label) {
  font-weight: var(--font-weight-medium);
  color: var(--apple-text-secondary);
  background-color: var(--apple-gray-1);
}

.dashboard-container :deep(.el-descriptions__content) {
  color: var(--apple-text-primary);
  font-weight: var(--font-weight-regular);
}

.security-tag {
  margin-right: var(--spacing-xs);
  margin-bottom: var(--spacing-xs);
  border-radius: 6px;
  font-weight: var(--font-weight-medium);
  background-color: var(--apple-blue-light);
  border-color: transparent;
  color: var(--apple-blue);
}

/* ========== 响应式设计 ========== */
@media (max-width: 768px) {
  .dashboard-container {
    padding: var(--spacing-md);
  }

  .welcome-content {
    flex-direction: column;
    text-align: center;
  }

  .welcome-text h2 {
    font-size: 28px;
  }

  .welcome-icon {
    margin-top: var(--spacing-md);
  }

  .stat-value {
    font-size: 32px;
  }

  .action-title {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .dashboard-container {
    padding: var(--spacing-sm);
  }

  .welcome-text h2 {
    font-size: 24px;
  }

  .stat-value {
    font-size: 28px;
  }

  .stat-icon {
    width: 64px;
    height: 64px;
  }
}
</style>
