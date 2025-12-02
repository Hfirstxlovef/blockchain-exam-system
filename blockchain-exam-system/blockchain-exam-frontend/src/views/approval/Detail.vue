<template>
  <div class="approval-detail-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>审批详情</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <!-- 试卷信息 -->
      <el-descriptions title="试卷信息" :column="2" border v-if="paper">
        <el-descriptions-item label="课程名称">{{ paper.courseName }}</el-descriptions-item>
        <el-descriptions-item label="考试类型">{{ paper.examType }}</el-descriptions-item>
        <el-descriptions-item label="学期">{{ paper.semester }}</el-descriptions-item>
        <el-descriptions-item label="院系">{{ paper.department }}</el-descriptions-item>
        <el-descriptions-item label="提交教师">{{ paper.teacherName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(paper.status)">
            {{ getStatusLabel(paper.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="试卷内容" :span="2">
          <!-- 加密状态 - 需要解密 -->
          <div v-if="isEncrypted && !isDecrypted" class="decrypt-section">
            <el-alert type="warning" :closable="false" show-icon class="decrypt-alert">
              <template #title>
                试卷内容已加密，请上传私钥或手动输入进行解密（解密操作将记录到区块链审计账本）
              </template>
            </el-alert>

            <!-- 所有用户统一使用私钥解密 -->
            <!-- 输入方式切换 -->
            <el-radio-group v-model="keyInputMode" class="decrypt-mode-switch">
              <el-radio-button value="file">文件上传</el-radio-button>
              <el-radio-button value="input">手动输入</el-radio-button>
            </el-radio-group>

            <!-- 文件上传方式 -->
            <div v-if="keyInputMode === 'file'" class="decrypt-actions">
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept=".pem"
                :on-change="handlePrivateKeyFile"
              >
                <el-button type="primary" :loading="decrypting">
                  {{ decrypting ? '解密中...' : '选择私钥文件解密' }}
                </el-button>
              </el-upload>
            </div>

            <!-- 手动输入方式 -->
            <div v-else class="decrypt-input">
              <el-input
                v-model="privateKeyText"
                type="textarea"
                :rows="4"
                placeholder="请粘贴私钥内容（支持PEM格式或纯Base64编码）"
              />
              <el-button
                type="primary"
                :loading="decrypting"
                :disabled="!privateKeyText.trim()"
                @click="handleManualDecrypt"
                style="margin-top: 12px;"
              >
                {{ decrypting ? '解密中...' : '解密查看' }}
              </el-button>
            </div>
          </div>

          <!-- 已解密或未加密 - 显示内容 -->
          <div v-else class="content-area">
            {{ displayContent }}
            <el-tag v-if="isDecrypted" type="success" size="small" style="margin-left: 10px;">
              已解密
            </el-tag>
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 审批记录 -->
      <el-divider />
      <h3>审批记录</h3>
      <el-timeline v-if="records.length > 0">
        <el-timeline-item
          v-for="record in records"
          :key="record.id"
          :timestamp="record.createTime"
          placement="top"
        >
          <el-card>
            <p><strong>审批人：</strong>{{ record.approverName }}</p>
            <p><strong>操作：</strong>
              <el-tag :type="record.action === 'approve' ? 'success' : 'danger'" size="small">
                {{ record.action === 'approve' ? '通过' : '驳回' }}
              </el-tag>
            </p>
            <p><strong>意见：</strong>{{ record.comment }}</p>
            <p>
              <strong>数字签名：</strong>
              <el-button link type="primary" size="small" @click="handleVerify(record.id)">
                验证签名
              </el-button>
            </p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审批记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPaperDetail, getPaperCryptoInfo, decryptPaperWithAudit } from '@/api/paper'
import { getApprovalRecords, verifySignature } from '@/api/approval'

const router = useRouter()
const route = useRoute()

const paper = ref(null)
const records = ref([])
const loading = ref(false)

// 解密相关
const cryptoInfo = ref(null)
const isEncrypted = ref(false)
const isDecrypted = ref(false)
const decrypting = ref(false)
const decryptedContent = ref('')
const keyInputMode = ref('file')
const privateKeyText = ref('')

// 显示内容计算属性
const displayContent = computed(() => {
  if (isDecrypted.value && decryptedContent.value) {
    return decryptedContent.value
  }
  return paper.value?.content || ''
})

const loadDetail = async () => {
  try {
    loading.value = true

    // 加载试卷详情
    const paperResponse = await getPaperDetail(route.params.id)
    if (paperResponse.code === 200) {
      paper.value = paperResponse.data
    }

    // 加载审批记录
    const recordsResponse = await getApprovalRecords(route.params.id)
    if (recordsResponse.code === 200) {
      records.value = recordsResponse.data || []
    }
  } catch (error) {
    console.error('加载详情失败:', error)
  } finally {
    loading.value = false
  }
}

const handleVerify = async (recordId) => {
  try {
    const response = await verifySignature(recordId)
    if (response.code === 200 && response.data) {
      if (response.data.valid) {
        ElMessage.success(response.data.message)
      } else {
        ElMessage.error(response.data.message)
      }
    }
  } catch (error) {
    console.error('验证签名失败:', error)
  }
}

// 加载加密信息
const loadCryptoInfo = async () => {
  try {
    const response = await getPaperCryptoInfo(route.params.id)
    if (response.code === 200) {
      cryptoInfo.value = response.data
      isEncrypted.value = response.data?.isPkiEncrypted || false
    }
  } catch (error) {
    console.error('加载加密信息失败:', error)
  }
}

// 提取私钥
const extractPrivateKey = (content) => {
  if (!content || !content.trim()) return null
  let cleaned = content
    .replace(/-----BEGIN.*?-----/g, '')
    .replace(/-----END.*?-----/g, '')
  cleaned = cleaned.replace(/\s/g, '')
  if (/^[A-Za-z0-9+/=]+$/.test(cleaned) && cleaned.length > 100) {
    return cleaned
  }
  return null
}

// 执行解密（带审计记录）
const doDecrypt = async (privateKey) => {
  try {
    decrypting.value = true
    const response = await decryptPaperWithAudit(route.params.id, privateKey)
    if (response.code === 200) {
      decryptedContent.value = response.data.content
      isDecrypted.value = true
      ElMessage.success('解密成功！操作已记录到区块链')
    } else {
      ElMessage.error(response.message || '解密失败')
    }
  } catch (error) {
    ElMessage.error('解密失败：私钥不匹配或格式错误')
  } finally {
    decrypting.value = false
  }
}

// 文件上传解密
const handlePrivateKeyFile = async (file) => {
  const content = await file.raw.text()
  const privateKey = extractPrivateKey(content)
  if (!privateKey) {
    ElMessage.error('无效的私钥文件格式')
    return
  }
  await doDecrypt(privateKey)
}

// 手动输入解密
const handleManualDecrypt = async () => {
  const privateKey = extractPrivateKey(privateKeyText.value)
  if (!privateKey) {
    ElMessage.error('无效的私钥格式')
    return
  }
  await doDecrypt(privateKey)
}

const getStatusType = (status) => {
  const typeMap = {
    pending: 'warning',
    dept_approved: 'primary',
    college_approved: 'success',
    rejected: 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusLabel = (status) => {
  const labelMap = {
    pending: '待审批',
    dept_approved: '系已审批',
    college_approved: '院已审批',
    rejected: '已驳回'
  }
  return labelMap[status] || status
}

onMounted(() => {
  loadDetail()
  loadCryptoInfo()
})
</script>

<style scoped>
.approval-detail-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-area {
  white-space: pre-wrap;
  line-height: 1.8;
}

h3 {
  margin: 20px 0 15px 0;
}

/* 解密区域样式 */
.decrypt-section {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.decrypt-alert {
  margin-bottom: 16px;
}

.decrypt-mode-switch {
  margin-bottom: 16px;
}

.decrypt-actions {
  text-align: center;
}

.decrypt-hint {
  margin-top: 10px;
  color: #909399;
  font-size: 13px;
}

.decrypt-input {
  display: flex;
  flex-direction: column;
}

.decrypt-input :deep(.el-textarea__inner) {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
}

/* 解密按钮白色字体 */
.decrypt-actions :deep(.el-button),
.decrypt-input :deep(.el-button) {
  color: #ffffff !important;
}
</style>
