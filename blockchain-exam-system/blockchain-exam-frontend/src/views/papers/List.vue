<template>
  <div class="paper-list-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的试卷</span>
          <el-button type="primary" :icon="Plus" @click="router.push('/papers/create')">
            创建试卷
          </el-button>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable @change="loadPapers">
            <el-option label="草稿" value="draft" />
            <el-option label="待审批" value="pending" />
            <el-option label="系已审批" value="dept_approved" />
            <el-option label="院已审批" value="college_approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadPapers">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="paperList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column prop="examType" label="考试类型" width="120" />
        <el-table-column prop="semester" label="学期" width="120" />
        <el-table-column prop="department" label="院系" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" class="view-btn" @click="handleView(row.id)">查看</el-button>
            <el-button
              v-if="row.status === 'draft'"
              link
              type="primary"
              @click="handleEdit(row.id)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              link
              type="success"
              @click="handleSubmit(row.id)"
            >
              提交审批
            </el-button>
            <el-button
              v-if="row.status === 'draft' || row.status === 'rejected'"
              link
              type="danger"
              @click="handleDelete(row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { getMyPapers, submitPaper, deletePaper } from '@/api/paper'

const router = useRouter()

// 查询参数
const queryParams = reactive({
  status: ''
})

// 数据
const paperList = ref([])
const loading = ref(false)

/**
 * 加载试卷列表
 */
const loadPapers = async () => {
  try {
    loading.value = true
    const response = await getMyPapers(queryParams.status)
    if (response.code === 200) {
      paperList.value = response.data || []
    }
  } catch (error) {
    console.error('加载试卷列表失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 重置查询
 */
const handleReset = () => {
  queryParams.status = ''
  loadPapers()
}

/**
 * 查看详情
 */
const handleView = (id) => {
  router.push(`/papers/${id}`)
}

/**
 * 编辑试卷
 */
const handleEdit = (id) => {
  router.push(`/papers/create?id=${id}`)
}

/**
 * 提交审批
 */
const handleSubmit = async (id) => {
  try {
    await ElMessageBox.confirm('确定要提交审批吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await submitPaper(id)
    if (response.code === 200) {
      ElMessage.success('提交成功')
      loadPapers()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交审批失败:', error)
    }
  }
}

/**
 * 删除试卷
 */
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该试卷吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await deletePaper(id)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadPapers()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除试卷失败:', error)
    }
  }
}

/**
 * 获取状态类型
 */
const getStatusType = (status) => {
  const typeMap = {
    draft: 'info',
    pending: 'warning',
    dept_approved: 'primary',
    college_approved: 'success',
    rejected: 'danger'
  }
  return typeMap[status] || 'info'
}

/**
 * 获取状态标签
 */
const getStatusLabel = (status) => {
  const labelMap = {
    draft: '草稿',
    pending: '待审批',
    dept_approved: '系已审批',
    college_approved: '院已审批',
    rejected: '已驳回'
  }
  return labelMap[status] || status
}

// 组件挂载时加载数据
onMounted(() => {
  loadPapers()
})
</script>

<style scoped>
/* ========== Apple 高雅白 UI 设计系统 ========== */

/* ========== 容器 + CSS变量定义 ========== */
.paper-list-container {
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
  --apple-green: #30d158;
  --apple-green-light: rgba(48, 209, 88, 0.1);
  --apple-red: #ff3b30;
  --apple-red-light: rgba(255, 59, 48, 0.1);
  --apple-orange: #ff9500;
  --apple-orange-light: rgba(255, 149, 0, 0.1);

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

/* ========== 主卡片 ========== */
.paper-list-container :deep(.el-card) {
  border-radius: var(--radius-xl) !important;
  border: 0.5px solid var(--apple-border) !important;
  box-shadow: var(--shadow-card) !important;
  transition: all var(--duration-base) var(--ease-out);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
}

.paper-list-container :deep(.el-card:hover) {
  box-shadow: var(--shadow-card-hover) !important;
}

.paper-list-container :deep(.el-card__header) {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--apple-gray-2);
  background: transparent;
}

.paper-list-container :deep(.el-card__body) {
  padding: var(--spacing-lg);
}

/* ========== 卡片头部 ========== */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  font-size: 20px;
  font-weight: var(--font-weight-semibold);
  color: var(--apple-text-primary);
  letter-spacing: -0.3px;
}

/* 创建试卷按钮 */
.card-header :deep(.el-button--primary) {
  background: var(--apple-blue) !important;
  border: none !important;
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
  letter-spacing: 0.3px;
  padding: 10px 20px;
  transition: all var(--duration-base) var(--ease-out);
  box-shadow: 0 2px 8px rgba(0, 113, 227, 0.15);
}

.card-header :deep(.el-button--primary:hover) {
  background: var(--apple-blue-hover) !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 113, 227, 0.25);
}

.card-header :deep(.el-button--primary:active) {
  transform: translateY(0);
}

/* ========== 筛选表单 ========== */
.filter-form {
  margin-bottom: var(--spacing-md);
}

.filter-form :deep(.el-form-item__label) {
  font-weight: var(--font-weight-medium);
  color: var(--apple-text-secondary);
}

.filter-form :deep(.el-select) {
  width: 200px;
}

.filter-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  background-color: var(--apple-gray-1);
  border: 1px solid transparent;
  box-shadow: none;
  transition: all var(--duration-base) var(--ease-out);
}

.filter-form :deep(.el-input__wrapper:hover) {
  background-color: var(--apple-gray-2);
  border-color: var(--apple-gray-3);
}

.filter-form :deep(.el-input__wrapper.is-focus) {
  background-color: #ffffff;
  border-color: var(--apple-blue);
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.1);
}

.filter-form :deep(.el-button) {
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
  padding: 10px 20px;
  transition: all var(--duration-base) var(--ease-out);
}

.filter-form :deep(.el-button--primary) {
  background: var(--apple-blue) !important;
  border: none !important;
  box-shadow: 0 2px 8px rgba(0, 113, 227, 0.15);
}

.filter-form :deep(.el-button--primary:hover) {
  background: var(--apple-blue-hover) !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 113, 227, 0.25);
}

.filter-form :deep(.el-button:not(.el-button--primary)) {
  background: var(--apple-gray-1);
  border: 1px solid var(--apple-gray-2);
  color: var(--apple-text-primary);
}

.filter-form :deep(.el-button:not(.el-button--primary):hover) {
  background: var(--apple-gray-2);
  border-color: var(--apple-gray-3);
  transform: translateY(-2px);
}

/* ========== 表格样式 ========== */
.paper-list-container :deep(.el-table) {
  border-radius: var(--radius-lg);
  overflow: hidden;
  background-color: transparent;
  font-family: var(--font-apple);
}

.paper-list-container :deep(.el-table__header-wrapper) {
  background-color: var(--apple-gray-1);
}

.paper-list-container :deep(.el-table th.el-table__cell) {
  background-color: var(--apple-gray-1);
  color: var(--apple-text-secondary);
  font-weight: var(--font-weight-semibold);
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid var(--apple-gray-2);
}

.paper-list-container :deep(.el-table td.el-table__cell) {
  color: var(--apple-text-primary);
  font-weight: var(--font-weight-regular);
  border-bottom: 1px solid var(--apple-gray-2);
}

.paper-list-container :deep(.el-table__row) {
  transition: all var(--duration-base) var(--ease-out);
}

.paper-list-container :deep(.el-table__row:hover) {
  background-color: var(--apple-gray-1) !important;
}

.paper-list-container :deep(.el-table--striped .el-table__body tr.el-table__row--striped) {
  background-color: rgba(245, 245, 247, 0.5);
}

.paper-list-container :deep(.el-table--striped .el-table__body tr.el-table__row--striped:hover) {
  background-color: var(--apple-gray-1) !important;
}

/* ========== 状态标签 ========== */
.paper-list-container :deep(.el-tag) {
  border-radius: 6px;
  font-weight: var(--font-weight-medium);
  padding: 4px 12px;
  border: none;
  font-size: 13px;
}

.paper-list-container :deep(.el-tag--info) {
  background-color: var(--apple-gray-2);
  color: var(--apple-text-secondary);
}

.paper-list-container :deep(.el-tag--warning) {
  background-color: var(--apple-orange-light);
  color: var(--apple-orange);
}

.paper-list-container :deep(.el-tag--primary) {
  background-color: var(--apple-blue-light);
  color: var(--apple-blue);
}

.paper-list-container :deep(.el-tag--success) {
  background-color: var(--apple-green-light);
  color: var(--apple-green);
}

.paper-list-container :deep(.el-tag--danger) {
  background-color: var(--apple-red-light);
  color: var(--apple-red);
}

/* ========== 操作按钮 ========== */
.paper-list-container :deep(.el-button.is-link) {
  font-size: 14px;
  font-weight: var(--font-weight-medium);
  transition: all var(--duration-base) var(--ease-out);
  padding: 8px 12px;
  border-radius: var(--radius-sm);
}

.paper-list-container :deep(.el-button--primary.is-link) {
  color: #ffffff;
  background-color: var(--apple-blue);
}

.paper-list-container :deep(.el-button--primary.is-link:hover) {
  background-color: var(--apple-blue-hover);
}

.paper-list-container :deep(.el-button--success.is-link) {
  color: var(--apple-green);
}

.paper-list-container :deep(.el-button--success.is-link:hover) {
  background-color: var(--apple-green-light);
}

.paper-list-container :deep(.el-button--danger.is-link) {
  color: var(--apple-red);
}

.paper-list-container :deep(.el-button--danger.is-link:hover) {
  background-color: var(--apple-red-light);
}

/* ========== 加载状态 ========== */
.paper-list-container :deep(.el-loading-mask) {
  background-color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
}

/* ========== 响应式设计 ========== */
@media (max-width: 768px) {
  .paper-list-container {
    padding: var(--spacing-md);
  }

  .card-header {
    flex-direction: column;
    gap: var(--spacing-sm);
    align-items: flex-start;
  }

  .filter-form {
    flex-direction: column;
  }

  .filter-form :deep(.el-form-item) {
    width: 100%;
  }

  .filter-form :deep(.el-select) {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .paper-list-container {
    padding: var(--spacing-sm);
  }

  .card-header span {
    font-size: 18px;
  }
}

/* 查看按钮样式 */
.view-btn {
  color: white !important;
  font-size: 12px !important;
}

.view-btn:hover {
  color: rgba(255, 255, 255, 0.8) !important;
}
</style>
