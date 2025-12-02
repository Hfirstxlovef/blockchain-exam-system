<template>
  <div class="all-papers-container">
    <!-- 页面标题卡片 -->
    <el-card class="header-card" shadow="hover">
      <div class="header-content">
        <div class="header-left">
          <div class="header-icon">
            <el-icon :size="32"><Files /></el-icon>
          </div>
          <div class="header-text">
            <h2 class="page-title">所有试卷</h2>
            <p class="page-subtitle">All Exam Papers Management</p>
          </div>
        </div>
        <div class="header-right">
          <div class="stats-cards">
            <div class="mini-stat">
              <div class="mini-stat-value">{{ paperList.length }}</div>
              <div class="mini-stat-label">总数</div>
            </div>
            <div class="mini-stat">
              <div class="mini-stat-value">{{ filteredPaperList.length }}</div>
              <div class="mini-stat-label">筛选</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 筛选器卡片 -->
    <el-card class="filter-card" shadow="hover">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="12" :md="6">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索课程名称或教师..."
            :prefix-icon="Search"
            clearable
            @input="handleSearch"
          />
        </el-col>
        <el-col :xs="24" :sm="12" :md="5">
          <el-select
            v-model="queryParams.status"
            placeholder="筛选状态"
            clearable
            style="width: 100%"
            @change="handleFilter"
          >
            <el-option label="全部状态" value="" />
            <el-option label="草稿" value="draft" />
            <el-option label="待审批" value="pending" />
            <el-option label="系已审批" value="dept_approved" />
            <el-option label="院已审批" value="college_approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="12" :md="5">
          <el-select
            v-model="queryParams.department"
            placeholder="筛选院系"
            clearable
            style="width: 100%"
            @change="handleFilter"
          >
            <el-option label="全部院系" value="" />
            <el-option label="计算机系" value="计算机系" />
            <el-option label="数学系" value="数学系" />
            <el-option label="物理系" value="物理系" />
            <el-option label="化学系" value="化学系" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="12" :md="4">
          <el-select
            v-model="sortBy"
            placeholder="排序方式"
            style="width: 100%"
            @change="handleSort"
          >
            <el-option label="最新创建" value="createTime_desc" />
            <el-option label="最早创建" value="createTime_asc" />
            <el-option label="ID升序" value="id_asc" />
            <el-option label="ID降序" value="id_desc" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="24" :md="4" class="filter-actions">
          <el-button
            type="primary"
            :icon="Refresh"
            @click="loadPapers"
            :loading="loading"
          >
            刷新
          </el-button>
        </el-col>
      </el-row>

      <!-- 快速筛选标签 -->
      <div class="quick-filters">
        <el-tag
          v-for="filter in quickFilters"
          :key="filter.value"
          :type="queryParams.status === filter.value ? filter.type : 'info'"
          :effect="queryParams.status === filter.value ? 'dark' : 'plain'"
          class="quick-filter-tag"
          @click="handleQuickFilter(filter.value)"
        >
          <el-icon><component :is="filter.icon" /></el-icon>
          <span>{{ filter.label }}</span>
        </el-tag>
      </div>
    </el-card>

    <!-- 表格卡片 -->
    <el-card class="table-card" shadow="hover">
      <el-table
        :data="filteredPaperList"
        v-loading="loading"
        class="papers-table"
        :header-cell-style="{ background: '#fafafa', color: '#1d1d1f', fontWeight: '600' }"
        :row-class-name="tableRowClassName"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="courseName" label="课程名称" min-width="160">
          <template #default="{ row }">
            <div class="course-name">
              <el-icon color="#409eff"><Document /></el-icon>
              <span>{{ row.courseName || row.subject }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="examType" label="考试类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getExamTypeTag(row.examType)" size="small">
              {{ row.examType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="semester" label="学期" width="120" align="center">
          <template #default="{ row }">
            <span class="semester-text">{{ row.semester || row.grade }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="提交教师" width="120" align="center">
          <template #default="{ row }">
            <div class="teacher-info">
              <el-icon color="#67c23a"><User /></el-icon>
              <span>{{ row.teacherName || row.creatorName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="院系" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light" class="status-tag">
              <el-icon><component :is="getStatusIcon(row.status)" /></el-icon>
              <span>{{ getStatusLabel(row.status) }}</span>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" align="center">
          <template #default="{ row }">
            <div class="time-text">
              <el-icon><Clock /></el-icon>
              <span>{{ formatTime(row.createTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :icon="View"
              class="view-btn"
              @click="handleView(row.id)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 暂无数据 -->
      <div v-if="!loading && filteredPaperList.length === 0" class="empty-state">
        <el-empty description="暂无试卷数据">
          <template #image>
            <el-icon :size="100" color="#d2d2d7"><FolderOpened /></el-icon>
          </template>
          <template #description>
            <p class="empty-title">暂无试卷数据</p>
            <p class="empty-subtitle">
              {{ paperList.length === 0 ? '系统中还没有试卷' : '没有符合筛选条件的试卷' }}
            </p>
          </template>
        </el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Files,
  Search,
  Refresh,
  Document,
  User,
  Clock,
  View,
  FolderOpened,
  Edit,
  CircleCheck,
  Clock as ClockIcon,
  CloseBold,
  Finished
} from '@element-plus/icons-vue'
import { getAllPapers } from '@/api/paper'

const router = useRouter()

// 查询参数
const queryParams = reactive({
  status: '',
  department: ''
})

// 数据
const paperList = ref([])
const loading = ref(false)

// 筛选和排序
const searchKeyword = ref('')
const sortBy = ref('createTime_desc')

// 快速筛选选项
const quickFilters = [
  { label: '全部', value: '', icon: Files, type: 'info' },
  { label: '草稿', value: 'draft', icon: Edit, type: 'info' },
  { label: '待审批', value: 'pending', icon: ClockIcon, type: 'warning' },
  { label: '系已审批', value: 'dept_approved', icon: CircleCheck, type: 'primary' },
  { label: '院已审批', value: 'college_approved', icon: Finished, type: 'success' },
  { label: '已驳回', value: 'rejected', icon: CloseBold, type: 'danger' }
]

// 过滤和排序后的试卷列表
const filteredPaperList = computed(() => {
  let list = paperList.value

  // 搜索过滤
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(paper => {
      const courseName = (paper.courseName || paper.subject || '').toLowerCase()
      const teacherName = (paper.teacherName || paper.creatorName || '').toLowerCase()
      return courseName.includes(keyword) || teacherName.includes(keyword)
    })
  }

  // 状态过滤
  if (queryParams.status) {
    list = list.filter(paper => paper.status === queryParams.status)
  }

  // 院系过滤
  if (queryParams.department) {
    list = list.filter(paper => paper.department === queryParams.department)
  }

  // 排序
  if (sortBy.value) {
    const [field, order] = sortBy.value.split('_')
    list = [...list].sort((a, b) => {
      let aVal = a[field]
      let bVal = b[field]

      if (field === 'createTime') {
        aVal = new Date(aVal).getTime()
        bVal = new Date(bVal).getTime()
      }

      if (order === 'asc') {
        return aVal > bVal ? 1 : -1
      } else {
        return aVal < bVal ? 1 : -1
      }
    })
  }

  return list
})

/**
 * 获取考试类型标签
 */
const getExamTypeTag = (examType) => {
  const typeMap = {
    '期末考试': 'primary',
    '期中考试': 'success',
    '随堂测验': 'warning',
    '补考': 'danger'
  }
  return typeMap[examType] || 'info'
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

/**
 * 获取状态图标
 */
const getStatusIcon = (status) => {
  const iconMap = {
    draft: Edit,
    pending: ClockIcon,
    dept_approved: CircleCheck,
    college_approved: Finished,
    rejected: CloseBold
  }
  return iconMap[status] || Document
}

/**
 * 表格行类名
 */
const tableRowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 0 ? 'even-row' : 'odd-row'
}

/**
 * 格式化时间
 */
const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

/**
 * 搜索
 */
const handleSearch = () => {
  // 搜索由computed自动处理
}

/**
 * 筛选
 */
const handleFilter = () => {
  // 筛选由computed自动处理
}

/**
 * 排序
 */
const handleSort = () => {
  // 排序由computed自动处理
}

/**
 * 快速筛选
 */
const handleQuickFilter = (status) => {
  queryParams.status = status
}

/**
 * 加载试卷列表
 */
const loadPapers = async () => {
  try {
    loading.value = true
    const response = await getAllPapers(queryParams.status, queryParams.department)
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
 * 查看详情
 */
const handleView = (id) => {
  router.push(`/papers/${id}`)
}

// 组件挂载时加载数据
onMounted(() => {
  loadPapers()
})
</script>

<style scoped>
/* ========== Apple 高雅白 UI 设计系统 ========== */

/* ========== 容器 + CSS变量定义 ========== */
.all-papers-container {
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

  /* 间距系统 (8px基准) */
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 16px;
  --spacing-lg: 20px;
  --spacing-xl: 24px;

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

  /* 字体系统 */
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text",
                "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;

  /* 动画系统 */
  --ease-out: cubic-bezier(0.25, 0.46, 0.45, 0.94);
  --duration-base: 0.3s;

  /* 布局 */
  min-height: 100vh;
  padding: var(--spacing-xl);
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 50%, #f0f3f7 100%);
  position: relative;
  font-family: var(--font-apple);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.all-papers-container::before {
  content: '';
  position: fixed;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.05) 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
}

.all-papers-container::after {
  content: '';
  position: fixed;
  bottom: -50%;
  left: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(118, 75, 162, 0.05) 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
}

.all-papers-container > * {
  position: relative;
  z-index: 1;
}

/* ========== 头部卡片 ========== */
.header-card {
  margin-bottom: var(--spacing-lg);
  border-radius: var(--radius-lg);
  border: none;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  box-shadow: var(--shadow-card);
  transition: all var(--duration-base) var(--ease-out);
  overflow: hidden;
  position: relative;
}

.header-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
}

.header-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card-hover);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: var(--radius-md);
  color: white;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.header-text {
  flex: 1;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  letter-spacing: -0.5px;
  color: var(--apple-text-primary);
  margin: 0 0 4px 0;
  line-height: 1.2;
}

.page-subtitle {
  font-size: 14px;
  color: var(--apple-text-secondary);
  margin: 0;
  line-height: 1.4;
}

/* 统计卡片 */
.stats-cards {
  display: flex;
  gap: var(--spacing-md);
}

.mini-stat {
  text-align: center;
  padding: var(--spacing-md) var(--spacing-lg);
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border-radius: var(--radius-md);
  min-width: 100px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all var(--duration-base) var(--ease-out);
  border: 1px solid rgba(102, 126, 234, 0.1);
}

.mini-stat:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
  border-color: rgba(102, 126, 234, 0.3);
}

.mini-stat-value {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: 8px;
}

.mini-stat-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--apple-text-secondary);
  line-height: 1;
  letter-spacing: 0.5px;
}

/* ========== 筛选卡片 ========== */
.filter-card {
  margin-bottom: var(--spacing-lg);
  border-radius: var(--radius-lg);
  border: none;
  background: #ffffff;
  box-shadow: var(--shadow-card);
  transition: all var(--duration-base) var(--ease-out);
}

.filter-card:hover {
  box-shadow: var(--shadow-card-hover);
}

.filter-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.filter-actions .el-button {
  width: 100%;
}

/* 快速筛选标签 */
.quick-filters {
  display: flex;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
  padding-top: var(--spacing-md);
  border-top: 2px solid var(--apple-gray-2);
  flex-wrap: wrap;
}

.quick-filter-tag {
  cursor: pointer;
  transition: all var(--duration-base) var(--ease-out);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-weight: 500;
  font-size: 13px;
  border-radius: 20px;
  border: 2px solid transparent;
}

.quick-filter-tag:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  border-color: currentColor;
}

/* ========== 表格卡片 ========== */
.table-card {
  border-radius: var(--radius-lg);
  border: none;
  background: #ffffff;
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

/* Element Plus表格样式定制 */
.table-card :deep(.el-table) {
  font-family: var(--font-apple);
  color: var(--apple-text-primary);
  border-radius: var(--radius-lg);
}

.table-card :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.table-card :deep(.el-table tr) {
  transition: all var(--duration-base) var(--ease-out);
}

.table-card :deep(.el-table .even-row) {
  background-color: #fafbfc;
}

.table-card :deep(.el-table .odd-row) {
  background-color: #ffffff;
}

.table-card :deep(.el-table tbody tr:hover) {
  background: linear-gradient(90deg, #f8f9fa 0%, #ffffff 100%) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transform: scale(1.002);
}

.table-card :deep(.el-table th) {
  font-weight: 600;
  font-size: 13px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  color: var(--apple-text-secondary);
  background: linear-gradient(135deg, #f8f9fa 0%, #f0f1f3 100%) !important;
  border-bottom: 2px solid var(--apple-gray-3);
}

.table-card :deep(.el-table td) {
  font-size: 14px;
  padding: 16px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.table-card :deep(.el-table--border::after),
.table-card :deep(.el-table--group::after),
.table-card :deep(.el-table::before) {
  display: none;
}

/* 表格内容样式 */
.course-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  font-size: 15px;
  color: var(--apple-text-primary);
}

.course-name .el-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  border-radius: 8px;
}

.teacher-info {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  border-radius: 12px;
  font-weight: 500;
}

.teacher-info .el-icon {
  font-size: 16px;
}

.semester-text {
  color: var(--apple-text-secondary);
  font-size: 13px;
  font-weight: 500;
  padding: 4px 10px;
  background: var(--apple-gray-1);
  border-radius: 6px;
  display: inline-block;
}

.time-text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--apple-text-tertiary);
  font-size: 13px;
  font-weight: 500;
  padding: 4px 8px;
  background: #fafafa;
  border-radius: 6px;
}

.status-tag {
  display: inline-flex !important;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  padding: 6px 14px !important;
  border-radius: 12px !important;
  font-size: 13px !important;
}

/* ========== 空状态 ========== */
.empty-state {
  padding: 60px 20px;
  text-align: center;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-text-primary);
  margin: 16px 0 8px 0;
}

.empty-subtitle {
  font-size: 14px;
  color: var(--apple-text-secondary);
  margin: 0;
}

/* ========== 响应式设计 ========== */
@media (max-width: 768px) {
  .all-papers-container {
    padding: var(--spacing-md);
  }

  .page-title {
    font-size: 24px;
  }

  .header-icon {
    width: 48px;
    height: 48px;
  }

  .stats-cards {
    flex-direction: column;
  }

  .mini-stat {
    min-width: auto;
  }

  .filter-actions {
    justify-content: flex-start;
    margin-top: var(--spacing-sm);
  }

  .quick-filters {
    gap: var(--spacing-xs);
  }

  .table-card :deep(.el-table) {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .header-content {
    flex-direction: column;
    gap: var(--spacing-md);
    align-items: flex-start;
  }

  .header-right {
    width: 100%;
  }

  .stats-cards {
    width: 100%;
    flex-direction: row;
  }

  .mini-stat {
    flex: 1;
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
