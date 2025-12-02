<template>
  <div class="paper-detail-container">
    <!-- 顶部导航栏 -->
    <div class="top-nav">
      <el-button class="back-button" @click="router.back()" :icon="ArrowLeft">
        返回
      </el-button>
    </div>

    <!-- 页面标题卡片 -->
    <el-card class="header-card" shadow="hover" v-loading="loading">
      <div class="header-content">
        <div class="header-left">
          <div class="header-icon">
            <el-icon :size="36"><Document /></el-icon>
          </div>
          <div class="header-text">
            <h2 class="page-title">{{ paper?.courseName || '试卷详情' }}</h2>
            <p class="page-subtitle" v-if="paper">
              <span class="subtitle-item">
                <el-icon><Calendar /></el-icon>
                {{ paper.semester }}
              </span>
              <span class="subtitle-item">
                <el-icon><School /></el-icon>
                {{ paper.department }}
              </span>
            </p>
          </div>
        </div>
        <div class="header-right" v-if="paper">
          <el-tag :type="getStatusType(paper.status)" size="large" class="status-tag" effect="dark">
            <el-icon style="margin-right: 4px">
              <component :is="getStatusIcon(paper.status)" />
            </el-icon>
            {{ getStatusLabel(paper.status) }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- 试卷信息卡片组 -->
    <div class="info-cards-wrapper" v-if="paper && !loading">
      <div class="info-cards-grid">
        <!-- 基本信息卡片 -->
        <el-card class="info-card" shadow="hover">
          <template #header>
            <div class="info-card-header">
              <el-icon class="header-icon-small"><InfoFilled /></el-icon>
              <span class="header-title">基本信息</span>
            </div>
          </template>
          <div class="info-items">
            <div class="info-item">
              <span class="info-label">
                <el-icon><Reading /></el-icon>
                课程名称
              </span>
              <span class="info-value">{{ paper.courseName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">
                <el-icon><Document /></el-icon>
                考试类型
              </span>
              <span class="info-value">
                <el-tag :type="getExamTypeTag(paper.examType)" size="small">
                  {{ paper.examType }}
                </el-tag>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">
                <el-icon><Calendar /></el-icon>
                学期
              </span>
              <span class="info-value">{{ paper.semester }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">
                <el-icon><School /></el-icon>
                院系
              </span>
              <span class="info-value">{{ paper.department }}</span>
            </div>
          </div>
        </el-card>

        <!-- 提交信息卡片 -->
        <el-card class="info-card" shadow="hover">
          <template #header>
            <div class="info-card-header">
              <el-icon class="header-icon-small"><User /></el-icon>
              <span class="header-title">提交信息</span>
            </div>
          </template>
          <div class="info-items">
            <div class="info-item">
              <span class="info-label">
                <el-icon><UserFilled /></el-icon>
                提交教师
              </span>
              <span class="info-value teacher-badge">
                <el-icon><Avatar /></el-icon>
                {{ paper.teacherName }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">
                <el-icon><Clock /></el-icon>
                创建时间
              </span>
              <span class="info-value">{{ paper.createTime }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">
                <el-icon><Finished /></el-icon>
                当前状态
              </span>
              <span class="info-value">
                <el-tag :type="getStatusType(paper.status)" size="small" effect="plain">
                  {{ getStatusLabel(paper.status) }}
                </el-tag>
              </span>
            </div>
            <div class="info-item" v-if="paper.id">
              <span class="info-label">
                <el-icon><Key /></el-icon>
                试卷编号
              </span>
              <span class="info-value id-badge">#{{ paper.id }}</span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 加密信息卡片 -->
      <el-card class="crypto-card" shadow="hover" v-if="cryptoInfo">
        <template #header>
          <div class="content-card-header">
            <div class="header-left">
              <el-icon class="header-icon-large"><Lock /></el-icon>
              <span class="header-title">加密信息</span>
            </div>
            <div class="header-right">
              <el-tag :type="cryptoInfo.isPkiEncrypted ? 'success' : 'info'" size="small">
                {{ cryptoInfo.isPkiEncrypted ? '🔐 PKI加密' : '📄 明文' }}
              </el-tag>
            </div>
          </div>
        </template>
        <div class="crypto-info-grid">
          <div class="crypto-item">
            <span class="crypto-label">内容哈希(SHA256)</span>
            <span class="crypto-value hash-value">{{ cryptoInfo.contentHash || '暂无' }}</span>
          </div>
          <div class="crypto-item" v-if="cryptoInfo.blockchainTxId">
            <span class="crypto-label">区块链交易ID</span>
            <span class="crypto-value">{{ cryptoInfo.blockchainTxId }}</span>
          </div>
          <div class="crypto-item" v-if="cryptoInfo.chainTime">
            <span class="crypto-label">上链时间</span>
            <span class="crypto-value">{{ cryptoInfo.chainTime }}</span>
          </div>
        </div>
        <!-- 解密区域 -->
        <div class="decrypt-section" v-if="cryptoInfo.isPkiEncrypted && !isDecrypted">
          <el-divider>
            <el-icon><Key /></el-icon>
            需要私钥解密
          </el-divider>
          <div class="decrypt-hint">
            <el-alert type="warning" :closable="false" show-icon>
              <template #title>
                此试卷使用您的RSA公钥加密，请上传私钥文件或手动输入私钥内容进行解密
              </template>
            </el-alert>
          </div>
          <!-- 输入方式切换 -->
          <div class="decrypt-mode-wrapper">
            <el-radio-group v-model="keyInputMode" class="decrypt-mode-switch">
              <el-radio-button value="file">文件上传</el-radio-button>
              <el-radio-button value="input">手动输入</el-radio-button>
            </el-radio-group>
          </div>
          <!-- 文件上传方式 -->
          <div class="decrypt-actions" v-if="keyInputMode === 'file'">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :show-file-list="false"
              accept=".pem"
              :on-change="handlePrivateKeyFile"
            >
              <el-button type="primary" :icon="Upload" :loading="decrypting">
                {{ decrypting ? '解密中...' : '选择私钥文件解密' }}
              </el-button>
            </el-upload>
          </div>
          <!-- 手动输入方式 -->
          <div class="decrypt-input-section" v-else>
            <el-input
              v-model="privateKeyText"
              type="textarea"
              :rows="5"
              placeholder="请粘贴私钥内容（支持PEM格式或纯Base64编码）
-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQE...
-----END PRIVATE KEY-----"
              class="private-key-textarea"
            />
            <div class="decrypt-input-actions">
              <el-button type="primary" :icon="Key" :loading="decrypting" @click="handleManualDecrypt" :disabled="!privateKeyText.trim()">
                {{ decrypting ? '解密中...' : '解密' }}
              </el-button>
            </div>
          </div>
        </div>
        <!-- 解密成功提示 -->
        <div class="decrypt-success" v-if="isDecrypted">
          <el-alert type="success" :closable="false" show-icon>
            <template #title>
              ✅ 解密成功！内容完整性已验证
            </template>
          </el-alert>
        </div>
      </el-card>

      <!-- 试卷内容卡片 -->
      <el-card class="content-card" shadow="hover">
        <template #header>
          <div class="content-card-header">
            <div class="header-left">
              <el-icon class="header-icon-large"><Tickets /></el-icon>
              <span class="header-title">试卷内容</span>
            </div>
            <div class="header-right">
              <el-tag type="info" size="small">
                <el-icon><DocumentCopy /></el-icon>
                {{ displayContent ? `${displayContent.length} 字符` : '暂无内容' }}
              </el-tag>
            </div>
          </div>
        </template>
        <div class="content-area" v-if="displayContent">
          <div class="content-text">{{ displayContent }}</div>
        </div>
        <div class="content-encrypted" v-else-if="cryptoInfo?.isPkiEncrypted && !isDecrypted">
          <el-empty description="内容已加密，请上传私钥解密" :image-size="100">
            <template #image>
              <el-icon :size="80" color="#909399"><Lock /></el-icon>
            </template>
          </el-empty>
        </div>
        <el-empty v-else description="暂无试卷内容" :image-size="100" />
      </el-card>

      <!-- 附件卡片 (如果有filePath) -->
      <el-card class="attachment-card" shadow="hover" v-if="paper.filePath">
        <template #header>
          <div class="content-card-header">
            <div class="header-left">
              <el-icon class="header-icon-large"><Paperclip /></el-icon>
              <span class="header-title">附件文件</span>
            </div>
          </div>
        </template>
        <div class="attachment-item">
          <el-icon class="file-icon"><Document /></el-icon>
          <span class="file-name">{{ paper.filePath }}</span>
          <el-button type="primary" size="small" :icon="Download">下载</el-button>
        </div>
      </el-card>

      <!-- 解密记录卡片 -->
      <el-card class="decrypt-records-card" shadow="hover" v-loading="loadingRecords">
        <template #header>
          <div class="content-card-header">
            <div class="header-left">
              <el-icon class="header-icon-large"><Tickets /></el-icon>
              <span class="header-title">解密记录（区块链审计）</span>
            </div>
            <div class="header-right">
              <el-tag type="info" size="small">
                共 {{ decryptRecords.length }} 条记录
              </el-tag>
            </div>
          </div>
        </template>
        <div v-if="decryptRecords.length > 0" class="decrypt-records-list">
          <div v-for="record in decryptRecords" :key="record.id" class="decrypt-record-item">
            <div class="record-main">
              <div class="record-user">
                <el-icon><Avatar /></el-icon>
                <span class="user-name">{{ record.userName }}</span>
                <el-tag size="small" :type="getRoleTagType(record.userRole)">
                  {{ getRoleLabel(record.userRole) }}
                </el-tag>
              </div>
              <div class="record-time">
                <el-icon><Clock /></el-icon>
                {{ record.decryptTime }}
              </div>
            </div>
            <div class="record-details">
              <span class="detail-item" v-if="record.ipAddress">
                <el-icon><Location /></el-icon>
                IP: {{ record.ipAddress }}
              </span>
              <span class="detail-item" v-if="record.blockchainTxId">
                <el-icon><Link /></el-icon>
                交易ID: {{ record.blockchainTxId }}
                <el-tag type="success" size="small" effect="plain">已上链</el-tag>
              </span>
              <span class="detail-item" v-else>
                <el-tag type="warning" size="small" effect="plain">待上链</el-tag>
              </span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无解密记录" :image-size="80" />
      </el-card>
    </div>

    <!-- 空状态 -->
    <el-card v-if="!paper && !loading" class="empty-card" shadow="never">
      <el-empty description="未找到试卷信息" :image-size="200" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPaperDetail, getPaperCryptoInfo, decryptPaperWithAudit, getDecryptRecords } from '@/api/paper'
import {
  ArrowLeft,
  Document,
  Calendar,
  School,
  InfoFilled,
  Reading,
  User,
  UserFilled,
  Clock,
  Finished,
  Key,
  Avatar,
  Tickets,
  DocumentCopy,
  Paperclip,
  Download,
  Edit,
  CircleCheck,
  CloseBold,
  Lock,
  Upload,
  Location,
  Link
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const paper = ref(null)
const loading = ref(false)
const cryptoInfo = ref(null)
const decryptedContent = ref(null)
const isDecrypted = ref(false)
const decrypting = ref(false)
const keyInputMode = ref('file')
const privateKeyText = ref('')
const decryptRecords = ref([])
const loadingRecords = ref(false)

// 计算显示的内容
const displayContent = computed(() => {
  if (isDecrypted.value && decryptedContent.value) {
    return decryptedContent.value
  }
  // 如果不是PKI加密，直接显示内容
  if (!cryptoInfo.value?.isPkiEncrypted && paper.value?.content) {
    return paper.value.content
  }
  return null
})

const loadPaperDetail = async () => {
  try {
    loading.value = true
    const response = await getPaperDetail(route.params.id)
    if (response.code === 200) {
      paper.value = response.data
    }
  } catch (error) {
    console.error('加载试卷详情失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载加密信息
const loadCryptoInfo = async () => {
  try {
    const response = await getPaperCryptoInfo(route.params.id)
    if (response.code === 200) {
      cryptoInfo.value = response.data
    }
  } catch (error) {
    console.error('加载加密信息失败:', error)
  }
}

// 处理私钥文件上传
const handlePrivateKeyFile = async (file) => {
  try {
    decrypting.value = true

    // 读取PEM文件内容
    const fileContent = await readFileAsText(file.raw)

    // 提取私钥（去掉PEM头尾和换行）
    const privateKey = extractPrivateKey(fileContent)

    if (!privateKey) {
      ElMessage.error('无效的私钥文件格式')
      return
    }

    await doDecrypt(privateKey)
  } catch (error) {
    console.error('解密失败:', error)
    ElMessage.error('解密失败：' + (error.message || '私钥不匹配或格式错误'))
  } finally {
    decrypting.value = false
  }
}

// 读取文件为文本
const readFileAsText = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => resolve(e.target.result)
    reader.onerror = (e) => reject(e)
    reader.readAsText(file)
  })
}

// 提取私钥（支持PEM格式和纯Base64）
const extractPrivateKey = (content) => {
  if (!content || !content.trim()) {
    return null
  }

  // 支持PKCS8格式和传统RSA格式
  const pkcs8Regex = /-----BEGIN PRIVATE KEY-----\s*([\s\S]*?)\s*-----END PRIVATE KEY-----/
  const rsaRegex = /-----BEGIN RSA PRIVATE KEY-----\s*([\s\S]*?)\s*-----END RSA PRIVATE KEY-----/

  let match = content.match(pkcs8Regex) || content.match(rsaRegex)

  if (match && match[1]) {
    // 去掉所有换行和空格
    return match[1].replace(/[\r\n\s]/g, '')
  }

  // 尝试作为纯Base64处理
  let cleaned = content
    .replace(/-----BEGIN.*?-----/g, '')
    .replace(/-----END.*?-----/g, '')
  cleaned = cleaned.replace(/\s/g, '')

  // 验证是否为有效的Base64编码（私钥长度通常较长）
  if (/^[A-Za-z0-9+/=]+$/.test(cleaned) && cleaned.length > 100) {
    return cleaned
  }

  return null
}

// 通用解密函数（带区块链审计）
const doDecrypt = async (privateKey) => {
  // 调用带审计的解密API
  const response = await decryptPaperWithAudit(route.params.id, privateKey)

  if (response.code === 200) {
    decryptedContent.value = response.data.content
    isDecrypted.value = true
    ElMessage.success('解密成功！操作已记录到区块链')
    // 刷新解密记录
    loadDecryptRecordsData()
  } else {
    ElMessage.error(response.message || '解密失败')
  }
}

// 加载解密记录
const loadDecryptRecordsData = async () => {
  try {
    loadingRecords.value = true
    const response = await getDecryptRecords(route.params.id)
    if (response.code === 200) {
      decryptRecords.value = response.data || []
    }
  } catch (error) {
    console.error('加载解密记录失败:', error)
  } finally {
    loadingRecords.value = false
  }
}

// 手动输入解密
const handleManualDecrypt = async () => {
  try {
    decrypting.value = true

    const privateKey = extractPrivateKey(privateKeyText.value)

    if (!privateKey) {
      ElMessage.error('无效的私钥格式，请检查输入内容')
      return
    }

    await doDecrypt(privateKey)
  } catch (error) {
    console.error('解密失败:', error)
    ElMessage.error('解密失败：' + (error.message || '私钥不匹配或格式错误'))
  } finally {
    decrypting.value = false
  }
}

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

const getStatusIcon = (status) => {
  const iconMap = {
    draft: Edit,
    pending: Clock,
    dept_approved: CircleCheck,
    college_approved: Finished,
    rejected: CloseBold
  }
  return iconMap[status] || Document
}

const getExamTypeTag = (examType) => {
  const typeMap = {
    '期末考试': 'primary',
    '期中考试': 'success',
    '补考': 'warning',
    '重修': 'danger'
  }
  return typeMap[examType] || 'info'
}

const getRoleLabel = (role) => {
  const labelMap = {
    'teacher': '教师',
    'dept_admin': '系主任',
    'college_admin': '院长'
  }
  return labelMap[role] || role
}

const getRoleTagType = (role) => {
  const typeMap = {
    'teacher': 'info',
    'dept_admin': 'warning',
    'college_admin': 'success'
  }
  return typeMap[role] || 'info'
}

onMounted(() => {
  loadPaperDetail()
  loadCryptoInfo()
  loadDecryptRecordsData()
})
</script>

<style scoped>
/* ==================== Container + CSS Variables ==================== */
.paper-detail-container {
  /* CSS Variables - Apple Style Design System */
  /* Colors */
  --color-primary: #007aff;
  --color-success: #34c759;
  --color-warning: #ff9500;
  --color-danger: #ff3b30;
  --color-info: #5ac8fa;

  /* Grays */
  --color-text-primary: #1d1d1f;
  --color-text-secondary: #6e6e73;
  --color-text-tertiary: #86868b;
  --color-bg-primary: #ffffff;
  --color-bg-secondary: #f5f5f7;
  --color-bg-tertiary: #e8e8ed;
  --color-border: #d2d2d7;

  /* Gradients */
  --gradient-primary: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  --gradient-success: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  --gradient-warning: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  --gradient-info: linear-gradient(135deg, #30cfd0 0%, #330867 100%);

  /* Spacing */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;

  /* Border Radius */
  --radius-sm: 6px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;

  /* Shadows */
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.04);
  --shadow-md: 0 4px 16px rgba(0, 0, 0, 0.08);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.12);
  --shadow-xl: 0 12px 32px rgba(0, 0, 0, 0.16);

  /* Typography */
  --font-size-xs: 12px;
  --font-size-sm: 14px;
  --font-size-base: 16px;
  --font-size-lg: 18px;
  --font-size-xl: 24px;
  --font-size-2xl: 32px;

  /* Transitions */
  --transition-fast: 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-base: 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: 0.5s cubic-bezier(0.4, 0, 0.2, 1);

  /* Container Styles */
  min-height: 100vh;
  padding: var(--spacing-lg);
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 50%, #f0f3f7 100%);
  position: relative;
}

/* Background decoration */
.paper-detail-container::before {
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

.paper-detail-container::after {
  content: '';
  position: fixed;
  bottom: -50%;
  left: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(118, 75, 162, 0.03) 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
}

.paper-detail-container > * {
  position: relative;
  z-index: 1;
}

/* ==================== Top Navigation ==================== */
.top-nav {
  margin-bottom: var(--spacing-lg);
}

.back-button {
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 20px;
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-text-primary);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-sm);
}

.back-button:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* ==================== Header Card ==================== */
.header-card {
  margin-bottom: var(--spacing-lg);
  border: none;
  border-radius: var(--radius-lg);
  overflow: hidden;
  position: relative;
  background: white;
  box-shadow: var(--shadow-md);
  transition: all var(--transition-base);
}

/* Top gradient border */
.header-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--gradient-primary);
  z-index: 1;
}

.header-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-xl);
}

.header-card :deep(.el-card__body) {
  padding: var(--spacing-xl);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--spacing-lg);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  flex: 1;
}

.header-icon {
  width: 72px;
  height: 72px;
  background: var(--gradient-primary);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
  transition: all var(--transition-base);
}

.header-card:hover .header-icon {
  transform: scale(1.05) rotate(5deg);
}

.header-text {
  flex: 1;
}

.page-title {
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-sm) 0;
  line-height: 1.2;
}

.page-subtitle {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

.subtitle-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.header-right .status-tag {
  padding: 12px 20px;
  font-size: var(--font-size-base);
  font-weight: 600;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
}

/* ==================== Info Cards Grid ==================== */
.info-cards-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.info-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: var(--spacing-lg);
}

.info-card {
  border: none;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: white;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-base);
  height: 100%;
}

.info-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.info-card :deep(.el-card__header) {
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-bottom: 2px solid #f0f0f5;
  padding: var(--spacing-md) var(--spacing-lg);
}

.info-card :deep(.el-card__body) {
  padding: var(--spacing-lg);
}

.info-card-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.header-icon-small {
  font-size: 20px;
  color: var(--color-primary);
}

.header-title {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text-primary);
}

.info-items {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md);
  background: #f8f9fa;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.info-item:hover {
  background: #f0f1f3;
  transform: translateX(4px);
}

.info-label {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-weight: 500;
}

.info-label .el-icon {
  font-size: 16px;
  color: var(--color-primary);
}

.info-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-weight: 600;
}

.teacher-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: 4px 12px;
  background: var(--gradient-primary);
  color: white;
  border-radius: 20px;
  font-size: var(--font-size-xs);
}

.id-badge {
  font-family: 'SF Mono', 'Monaco', 'Courier New', monospace;
  padding: 4px 10px;
  background: linear-gradient(135deg, #e8ecf1 0%, #f5f7fa 100%);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

/* ==================== Content Card ==================== */
.content-card,
.attachment-card {
  border: none;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: white;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-base);
}

.content-card:hover,
.attachment-card:hover {
  box-shadow: var(--shadow-lg);
}

.content-card :deep(.el-card__header),
.attachment-card :deep(.el-card__header) {
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-bottom: 2px solid #f0f0f5;
  padding: var(--spacing-lg);
}

.content-card :deep(.el-card__body),
.attachment-card :deep(.el-card__body) {
  padding: var(--spacing-xl);
}

.content-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-card-header .header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.header-icon-large {
  font-size: 24px;
  color: var(--color-primary);
}

.content-area {
  background: #f8f9fa;
  border-radius: var(--radius-md);
  padding: var(--spacing-xl);
  min-height: 300px;
}

.content-text {
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* ==================== Attachment Card ==================== */
.attachment-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  background: #f8f9fa;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.attachment-item:hover {
  background: #f0f1f3;
  transform: translateX(4px);
}

.file-icon {
  font-size: 32px;
  color: var(--color-primary);
}

.file-name {
  flex: 1;
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-weight: 500;
}

/* ==================== Empty State ==================== */
.empty-card {
  background: white;
  border: none;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.empty-card :deep(.el-card__body) {
  padding: calc(var(--spacing-xl) * 2);
}

/* ==================== Responsive Design ==================== */
@media (max-width: 768px) {
  .paper-detail-container {
    padding: var(--spacing-md);
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-icon {
    width: 56px;
    height: 56px;
  }

  .page-title {
    font-size: var(--font-size-xl);
  }

  .page-subtitle {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-xs);
  }

  .info-cards-grid {
    grid-template-columns: 1fr;
  }

  .content-card :deep(.el-card__body),
  .attachment-card :deep(.el-card__body) {
    padding: var(--spacing-lg);
  }

  .content-area {
    padding: var(--spacing-lg);
    min-height: 200px;
  }
}

/* ==================== Loading State ==================== */
.header-card :deep(.el-loading-mask) {
  border-radius: var(--radius-lg);
}

/* ==================== Crypto Card ==================== */
.crypto-card {
  border: none;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: white;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-base);
  margin-bottom: var(--spacing-lg);
}

.crypto-card:hover {
  box-shadow: var(--shadow-lg);
}

.crypto-card :deep(.el-card__header) {
  background: linear-gradient(135deg, #e8f5e9 0%, #ffffff 100%);
  border-bottom: 2px solid #c8e6c9;
  padding: var(--spacing-lg);
}

.crypto-card :deep(.el-card__body) {
  padding: var(--spacing-xl);
}

.crypto-info-grid {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.crypto-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  padding: var(--spacing-md);
  background: #f8f9fa;
  border-radius: var(--radius-sm);
}

.crypto-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  font-weight: 500;
}

.crypto-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-weight: 600;
}

.hash-value {
  font-family: 'SF Mono', 'Monaco', 'Courier New', monospace;
  word-break: break-all;
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  background: linear-gradient(135deg, #e3f2fd 0%, #f5f5f5 100%);
  padding: var(--spacing-sm);
  border-radius: var(--radius-sm);
}

/* Decrypt Section */
.decrypt-section {
  margin-top: var(--spacing-lg);
}

.decrypt-hint {
  margin-bottom: var(--spacing-lg);
}

.decrypt-mode-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: var(--spacing-lg);
}

.decrypt-mode-switch {
  background: #f5f5f7;
  padding: 4px;
  border-radius: var(--radius-md);
}

.decrypt-mode-switch :deep(.el-radio-button__inner) {
  border: none;
  background: transparent;
  box-shadow: none;
  padding: 10px 24px;
  font-weight: 500;
  color: var(--color-text-secondary);
  border-radius: var(--radius-sm);
  transition: all var(--transition-base);
}

.decrypt-mode-switch :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: white;
  color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

.decrypt-actions {
  display: flex;
  justify-content: center;
}

.decrypt-input-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.private-key-textarea {
  font-family: 'SF Mono', 'Monaco', 'Courier New', monospace;
}

.private-key-textarea :deep(.el-textarea__inner) {
  border-radius: var(--radius-md);
  border: 2px solid var(--color-border);
  padding: var(--spacing-md);
  font-size: var(--font-size-sm);
  line-height: 1.6;
  transition: all var(--transition-base);
}

.private-key-textarea :deep(.el-textarea__inner):focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.1);
}

.decrypt-input-actions {
  display: flex;
  justify-content: center;
}

.decrypt-success {
  margin-top: var(--spacing-lg);
}

/* Content Encrypted State */
.content-encrypted {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
  background: linear-gradient(135deg, #f5f5f5 0%, #fafafa 100%);
  border-radius: var(--radius-md);
}

/* ==================== Decrypt Records Card ==================== */
.decrypt-records-card {
  border: none;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: white;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-base);
}

.decrypt-records-card:hover {
  box-shadow: var(--shadow-lg);
}

.decrypt-records-card :deep(.el-card__header) {
  background: linear-gradient(135deg, #e3f2fd 0%, #ffffff 100%);
  border-bottom: 2px solid #bbdefb;
  padding: var(--spacing-lg);
}

.decrypt-records-card :deep(.el-card__body) {
  padding: var(--spacing-xl);
}

.decrypt-records-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.decrypt-record-item {
  padding: var(--spacing-lg);
  background: #f8f9fa;
  border-radius: var(--radius-md);
  border-left: 4px solid var(--color-primary);
  transition: all var(--transition-fast);
}

.decrypt-record-item:hover {
  background: #f0f1f3;
  transform: translateX(4px);
}

.record-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}

.record-user {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.record-user .el-icon {
  font-size: 18px;
  color: var(--color-primary);
}

.user-name {
  font-weight: 600;
  color: var(--color-text-primary);
}

.record-time {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.record-details {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.detail-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.detail-item .el-icon {
  font-size: 14px;
}

/* ==================== Print Styles ==================== */
@media print {
  .top-nav,
  .back-button {
    display: none;
  }

  .paper-detail-container {
    background: white;
    padding: 0;
  }

  .header-card,
  .info-card,
  .content-card {
    box-shadow: none;
    page-break-inside: avoid;
  }
}
</style>
