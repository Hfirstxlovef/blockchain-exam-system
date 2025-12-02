<template>
  <div class="approval-list-container">
    <!-- 页面标题卡片 -->
    <el-card class="header-card" shadow="hover">
      <div class="header-content">
        <div class="header-left">
          <div class="header-icon">
            <el-icon :size="32"><CircleCheck /></el-icon>
          </div>
          <div class="header-text">
            <h2 class="page-title">待审批试卷</h2>
            <p class="page-subtitle">Review & Approve Exam Papers</p>
          </div>
        </div>
        <div class="header-right">
          <el-tag :type="roleTagType" size="large" class="role-tag">
            <el-icon style="margin-right: 4px"><User /></el-icon>
            {{ roleLabel }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- 筛选器卡片 -->
    <el-card class="filter-card" shadow="hover">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="12" :md="8">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索课程名称或教师姓名..."
            :prefix-icon="Search"
            clearable
            @input="handleSearch"
          />
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <el-select
            v-model="filterDepartment"
            placeholder="筛选院系"
            clearable
            style="width: 100%"
            @change="handleFilter"
          >
            <el-option label="全部院系" value="" />
            <el-option label="计算机系" value="计算机系" />
            <el-option label="数学系" value="数学系" />
            <el-option label="物理系" value="物理系" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="24" :md="8" class="filter-stats">
          <div class="stats-text">
            共 <span class="stats-number">{{ filteredPaperList.length }}</span> 份待审批
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 表格卡片 -->
    <el-card class="table-card" shadow="hover">
      <el-table
        :data="filteredPaperList"
        v-loading="loading"
        class="approval-table"
        :header-cell-style="{ background: '#fafafa', color: '#1d1d1f', fontWeight: '600' }"
        :row-class-name="tableRowClassName"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="courseName" label="课程名称" min-width="150">
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
        <el-table-column prop="department" label="院系" width="140" align="center" />
        <el-table-column prop="createTime" label="提交时间" width="180" align="center">
          <template #default="{ row }">
            <div class="time-text">
              <el-icon><Clock /></el-icon>
              <span>{{ formatTime(row.createTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                link
                type="primary"
                :icon="View"
                class="view-btn"
                @click="handleViewDetail(row.id)"
              >
                查看
              </el-button>
              <el-button
                link
                type="success"
                :icon="CircleCheck"
                @click="handleApprove(row.id)"
              >
                审批
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 暂无数据 -->
      <div v-if="!loading && filteredPaperList.length === 0" class="empty-state">
        <el-empty description="暂无待审批试卷">
          <template #image>
            <el-icon :size="100" color="#d2d2d7"><DocumentChecked /></el-icon>
          </template>
          <template #description>
            <p class="empty-title">暂无待审批试卷</p>
            <p class="empty-subtitle">目前没有需要审批的试卷</p>
          </template>
        </el-empty>
      </div>
    </el-card>

    <!-- 审批对话框 -->
    <el-dialog
      v-model="approvalDialogVisible"
      title="审批试卷"
      width="540px"
      class="approval-dialog"
      @close="handleDialogClose"
    >
      <template #header>
        <div class="dialog-header">
          <div class="dialog-icon">
            <el-icon :size="24"><CircleCheck /></el-icon>
          </div>
          <div class="dialog-title">
            <h3>审批试卷</h3>
            <p>Review Exam Paper</p>
          </div>
        </div>
      </template>

      <el-form
        ref="approvalFormRef"
        :model="approvalForm"
        :rules="approvalRules"
        label-width="100px"
        class="approval-form"
      >
        <el-form-item label="审批意见" prop="comment">
          <el-input
            v-model="approvalForm.comment"
            type="textarea"
            :rows="5"
            placeholder="请详细说明审批意见..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="数字签名" prop="privateKey">
          <!-- 输入方式切换 -->
          <el-radio-group v-model="keyInputMode" class="key-mode-switch">
            <el-radio-button value="file">文件上传</el-radio-button>
            <el-radio-button value="input">手动输入</el-radio-button>
          </el-radio-group>

          <!-- 文件上传方式 -->
          <div v-if="keyInputMode === 'file'" class="private-key-upload">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :show-file-list="false"
              accept=".pem"
              :on-change="handleKeyFileChange"
            >
              <template #trigger>
                <el-button type="primary" :icon="Key">
                  {{ approvalForm.privateKey ? '已选择私钥文件' : '选择PEM私钥文件' }}
                </el-button>
              </template>
            </el-upload>
            <span v-if="keyFileName" class="key-file-name">{{ keyFileName }}</span>
          </div>

          <!-- 手动输入方式 -->
          <div v-else class="private-key-input">
            <el-input
              v-model="privateKeyText"
              type="textarea"
              :rows="4"
              placeholder="请粘贴私钥内容（支持PEM格式或纯Base64编码）
-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQE...
-----END PRIVATE KEY-----"
            />
          </div>

          <div class="form-tip">
            <el-icon><Lock /></el-icon>
            <span>请上传或粘贴您的RSA私钥（PEM格式），用于生成数字签名，确保审批不可抵赖</span>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button
            class="cancel-btn"
            @click="approvalDialogVisible = false"
          >
            取消
          </el-button>
          <el-button
            type="danger"
            :icon="CloseBold"
            class="reject-btn"
            @click="handleReject"
          >
            驳回
          </el-button>
          <el-button
            type="success"
            :icon="CircleCheck"
            :loading="submitting"
            class="approve-btn"
            @click="handleConfirmApprove"
          >
            {{ submitting ? '审批中...' : '通过' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheck,
  User,
  Search,
  Document,
  Clock,
  View,
  DocumentChecked,
  Lock,
  CloseBold,
  Key
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getPendingPapers, approvePaper, rejectPaper } from '@/api/approval'

const router = useRouter()
const userStore = useUserStore()

// 数据
const paperList = ref([])
const loading = ref(false)

// 筛选
const searchKeyword = ref('')
const filterDepartment = ref('')

// 审批对话框
const approvalDialogVisible = ref(false)
const currentPaperId = ref(null)
const submitting = ref(false)

// 审批表单
const approvalFormRef = ref(null)
const uploadRef = ref(null)
const keyFileName = ref('')
const keyInputMode = ref('file')  // 'file' | 'input'
const privateKeyText = ref('')
const approvalForm = reactive({
  comment: '',
  privateKey: ''
})

// 审批表单验证规则
const approvalRules = {
  comment: [
    { required: true, message: '请输入审批意见', trigger: 'blur' },
    { min: 5, message: '审批意见至少5个字符', trigger: 'blur' }
  ],
  privateKey: [
    { required: true, message: '请上传您的RSA私钥文件', trigger: 'change' }
  ]
}

/**
 * 处理私钥文件上传
 */
const handleKeyFileChange = async (file) => {
  try {
    const content = await file.raw.text()
    const privateKey = extractPrivateKey(content)
    if (privateKey) {
      approvalForm.privateKey = privateKey
      keyFileName.value = file.name
      ElMessage.success('私钥文件读取成功')
    } else {
      ElMessage.error('无效的PEM私钥文件')
    }
  } catch (error) {
    console.error('读取私钥文件失败:', error)
    ElMessage.error('读取私钥文件失败')
  }
}

/**
 * 从PEM文件提取私钥
 */
const extractPrivateKey = (pemContent) => {
  // 移除PEM头尾和空白
  const lines = pemContent.split('\n')
  const keyLines = lines.filter(line =>
    !line.includes('-----BEGIN') &&
    !line.includes('-----END') &&
    line.trim() !== ''
  )
  const base64Key = keyLines.join('')

  // 验证是否为有效的Base64
  try {
    if (base64Key.length < 100) {
      return null
    }
    return base64Key
  } catch {
    return null
  }
}

// 角色标签
const roleLabel = computed(() => {
  return userStore.isDeptAdmin ? '系管理员' : '院管理员'
})

const roleTagType = computed(() => {
  return userStore.isDeptAdmin ? 'warning' : 'danger'
})

// 过滤后的试卷列表
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

  // 院系过滤
  if (filterDepartment.value) {
    list = list.filter(paper => paper.department === filterDepartment.value)
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
 * 加载待审批试卷
 */
const loadPapers = async () => {
  try {
    loading.value = true
    const response = await getPendingPapers()
    if (response.code === 200) {
      paperList.value = response.data || []
    }
  } catch (error) {
    console.error('加载待审批试卷失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 查看详情
 */
const handleViewDetail = (id) => {
  router.push(`/approval/${id}`)
}

/**
 * 审批
 */
const handleApprove = (id) => {
  currentPaperId.value = id
  approvalDialogVisible.value = true
}

/**
 * 获取私钥（支持文件上传和手动输入两种方式）
 */
const getPrivateKey = () => {
  if (keyInputMode.value === 'file') {
    return approvalForm.privateKey
  } else {
    return extractPrivateKey(privateKeyText.value)
  }
}

/**
 * 确认通过
 */
const handleConfirmApprove = async () => {
  if (!approvalFormRef.value) return

  // 验证审批意见
  if (!approvalForm.comment || approvalForm.comment.length < 5) {
    ElMessage.warning('请输入审批意见（至少5个字符）')
    return
  }

  // 获取私钥
  const privateKey = getPrivateKey()
  if (!privateKey) {
    ElMessage.warning(keyInputMode.value === 'file' ? '请先上传您的RSA私钥文件' : '请输入有效的私钥内容')
    return
  }

  try {
    submitting.value = true

    const response = await approvePaper({
      paperId: currentPaperId.value,
      comment: approvalForm.comment,
      privateKey: privateKey
    })

    if (response.code === 200) {
      ElMessage.success('审批通过成功，数字签名已生成')
      approvalDialogVisible.value = false
      loadPapers()
    }
  } catch (error) {
    console.error('审批失败:', error)
  } finally {
    submitting.value = false
  }
}

/**
 * 驳回
 */
const handleReject = async () => {
  if (!approvalForm.comment) {
    ElMessage.warning('请输入驳回原因')
    return
  }

  // 获取私钥
  const privateKey = getPrivateKey()
  if (!privateKey) {
    ElMessage.warning(keyInputMode.value === 'file' ? '请先上传您的RSA私钥文件' : '请输入有效的私钥内容')
    return
  }

  try {
    await ElMessageBox.confirm('确定要驳回该试卷吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    submitting.value = true

    const response = await rejectPaper({
      paperId: currentPaperId.value,
      comment: approvalForm.comment,
      privateKey: privateKey
    })

    if (response.code === 200) {
      ElMessage.success('已驳回，数字签名已生成')
      approvalDialogVisible.value = false
      loadPapers()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('驳回失败:', error)
    }
  } finally {
    submitting.value = false
  }
}

/**
 * 对话框关闭
 */
const handleDialogClose = () => {
  approvalForm.comment = ''
  approvalForm.privateKey = ''
  keyFileName.value = ''
  currentPaperId.value = null
  if (approvalFormRef.value) {
    approvalFormRef.value.resetFields()
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadPapers()
})
</script>

<style scoped>
/* ========== Apple 高雅白 UI 设计系统 ========== */

/* ========== 容器 + CSS变量定义 ========== */
.approval-list-container {
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
  --apple-green: #43e97b;
  --apple-red: #f56c6c;

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
  background: linear-gradient(135deg, #fafafa 0%, #f0f0f2 100%);
  font-family: var(--font-apple);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* ========== 头部卡片 ========== */
.header-card {
  margin-bottom: var(--spacing-lg);
  border-radius: var(--radius-lg);
  border: 0.5px solid var(--apple-border);
  transition: all var(--duration-base) var(--ease-out);
}

.header-card:hover {
  transform: translateY(-2px);
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
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  border-radius: var(--radius-md);
  color: white;
  box-shadow: 0 4px 16px rgba(67, 233, 123, 0.3);
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

.role-tag {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: var(--radius-sm);
}

/* ========== 筛选卡片 ========== */
.filter-card {
  margin-bottom: var(--spacing-lg);
  border-radius: var(--radius-lg);
  border: 0.5px solid var(--apple-border);
}

.filter-stats {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.stats-text {
  font-size: 15px;
  color: var(--apple-text-secondary);
}

.stats-number {
  font-size: 20px;
  font-weight: 600;
  color: var(--apple-blue);
  margin: 0 4px;
}

/* ========== 表格卡片 ========== */
.table-card {
  border-radius: var(--radius-lg);
  border: 0.5px solid var(--apple-border);
  overflow: hidden;
}

/* Element Plus表格样式定制 */
.table-card :deep(.el-table) {
  font-family: var(--font-apple);
  color: var(--apple-text-primary);
}

.table-card :deep(.el-table tr) {
  transition: background-color var(--duration-base) var(--ease-out);
}

.table-card :deep(.el-table .even-row) {
  background-color: #fafafa;
}

.table-card :deep(.el-table .odd-row) {
  background-color: #ffffff;
}

.table-card :deep(.el-table tr:hover) {
  background-color: var(--apple-gray-1) !important;
}

.table-card :deep(.el-table th) {
  font-weight: 600;
  font-size: 13px;
  letter-spacing: 0.3px;
}

.table-card :deep(.el-table td) {
  font-size: 14px;
}

/* 表格内容样式 */
.course-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.teacher-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.semester-text {
  color: var(--apple-text-secondary);
  font-size: 13px;
}

.time-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--apple-text-tertiary);
  font-size: 13px;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

/* 查看按钮样式 */
.view-btn {
  color: white !important;
  font-size: 12px !important;
}

.view-btn:hover {
  color: rgba(255, 255, 255, 0.8) !important;
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

/* ========== 审批对话框 ========== */
.approval-dialog :deep(.el-dialog) {
  border-radius: var(--radius-lg);
  padding: 0;
}

.dialog-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--apple-gray-2);
}

.dialog-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  border-radius: var(--radius-md);
  color: white;
}

.dialog-title h3 {
  font-size: 20px;
  font-weight: 600;
  color: var(--apple-text-primary);
  margin: 0 0 4px 0;
}

.dialog-title p {
  font-size: 13px;
  color: var(--apple-text-secondary);
  margin: 0;
}

.approval-form {
  padding: var(--spacing-lg) 0;
}

.approval-form :deep(.el-textarea__inner) {
  font-family: var(--font-apple);
  padding: 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--apple-gray-3);
  transition: all var(--duration-base) var(--ease-out);
}

.approval-form :deep(.el-textarea__inner:focus) {
  border-color: var(--apple-blue);
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.1);
}

.approval-form :deep(.el-input__wrapper) {
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--apple-gray-3);
  box-shadow: none;
  transition: all var(--duration-base) var(--ease-out);
}

.approval-form :deep(.el-input__wrapper:hover) {
  border-color: var(--apple-gray-3);
}

.approval-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--apple-blue);
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.1);
}

.form-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--apple-text-tertiary);
}

.key-mode-switch {
  margin-bottom: 12px;
  display: block;
}

.key-mode-switch :deep(.el-radio-button__inner) {
  padding: 8px 16px;
}

.private-key-upload {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.private-key-input {
  width: 100%;
}

.private-key-input :deep(.el-textarea__inner) {
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
}

.key-file-name {
  font-size: 13px;
  color: var(--apple-blue);
  font-weight: 500;
}

.dialog-footer {
  display: flex;
  gap: var(--spacing-sm);
  justify-content: flex-end;
  padding: var(--spacing-lg);
  border-top: 1px solid var(--apple-gray-2);
}

.dialog-footer .el-button {
  padding: 10px 20px;
  border-radius: var(--radius-sm);
  font-weight: 500;
  transition: all var(--duration-base) var(--ease-out);
}

.cancel-btn {
  border: 1px solid var(--apple-gray-3);
  color: var(--apple-text-secondary);
}

.cancel-btn:hover {
  background-color: var(--apple-gray-1);
  border-color: var(--apple-gray-3);
}

.reject-btn:hover {
  transform: scale(1.02);
}

.approve-btn:hover {
  transform: scale(1.02);
}

/* ========== 响应式设计 ========== */
@media (max-width: 768px) {
  .approval-list-container {
    padding: var(--spacing-md);
  }

  .page-title {
    font-size: 24px;
  }

  .header-icon {
    width: 48px;
    height: 48px;
  }

  .filter-stats {
    justify-content: flex-start;
    margin-top: var(--spacing-sm);
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

  .role-tag {
    width: 100%;
    justify-content: center;
  }
}
</style>
