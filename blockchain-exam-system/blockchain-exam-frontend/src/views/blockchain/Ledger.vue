<template>
  <div class="ledger-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-icon">
          <el-icon :size="36"><Notebook /></el-icon>
        </div>
        <div class="header-text">
          <h1 class="page-title">区块链审计账本</h1>
          <p class="page-subtitle">透明可追溯的操作记录，所有用户均可查看和验证</p>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid" v-loading="loadingStats">
      <div class="stat-card" @click="filterByType('ALL')" :class="{ active: currentFilter === 'ALL' }">
        <div class="stat-icon blue">
          <el-icon :size="24"><List /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ stats.totalOperations || 0 }}</p>
          <p class="stat-label">总操作数</p>
        </div>
      </div>

      <div class="stat-card" @click="filterByType('CREATE')" :class="{ active: currentFilter === 'CREATE' }">
        <div class="stat-icon green">
          <el-icon :size="24"><Plus /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ stats.createRecordCount || 0 }}</p>
          <p class="stat-label">创建记录</p>
        </div>
      </div>

      <div class="stat-card" @click="filterByType('APPROVE')" :class="{ active: currentFilter === 'APPROVE' }">
        <div class="stat-icon orange">
          <el-icon :size="24"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ stats.approvalRecordCount || 0 }}</p>
          <p class="stat-label">审批记录</p>
        </div>
      </div>

      <div class="stat-card" @click="filterByType('DECRYPT')" :class="{ active: currentFilter === 'DECRYPT' }">
        <div class="stat-icon purple">
          <el-icon :size="24"><Key /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ stats.decryptRecordCount || 0 }}</p>
          <p class="stat-label">解密记录</p>
        </div>
      </div>
    </div>

    <!-- 审计日志列表 -->
    <el-card class="records-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><Document /></el-icon>
            <span class="header-title">审计操作记录</span>
            <el-tag v-if="currentFilter !== 'ALL'" size="small" closable @close="filterByType('ALL')">
              {{ filterLabels[currentFilter] }}
            </el-tag>
          </div>
          <div class="header-right">
            <el-select v-model="currentFilter" placeholder="操作类型" style="width: 140px; margin-right: 12px" @change="loadAuditLogs">
              <el-option label="全部操作" value="ALL" />
              <el-option label="创建试卷" value="CREATE" />
              <el-option label="审批操作" value="APPROVE" />
              <el-option label="解密查看" value="DECRYPT" />
            </el-select>
            <el-button @click="loadAuditLogs" :loading="loadingLogs">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="auditLogs"
        v-loading="loadingLogs"
        stripe
        highlight-current-row
        @row-click="showLogDetail"
        style="width: 100%"
      >
        <el-table-column type="index" width="60" label="#" />
        <el-table-column prop="operationLabel" label="操作类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getOperationTagType(row.operationType)" size="small" effect="dark">
              {{ row.operationLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paperTitle" label="试卷名称" min-width="180">
          <template #default="{ row }">
            <span class="paper-title">{{ row.paperTitle || '未知试卷' }}</span>
            <el-tag size="small" type="info" style="margin-left: 8px">
              #{{ row.paperId }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作用户" min-width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <el-icon><Avatar /></el-icon>
              <span>{{ row.operatorName || '未知用户' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="operatorRole" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.operatorRole)" size="small">
              {{ getRoleLabel(row.operatorRole) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operationTime" label="操作时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.operationTime) }}
          </template>
        </el-table-column>
        <el-table-column label="上链状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isChained" type="success" size="small" effect="dark">
              <el-icon style="margin-right: 4px"><Link /></el-icon>
              已上链
            </el-tag>
            <el-tag v-else type="warning" size="small" effect="plain">
              待上链
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.operationType === 'DECRYPT' && row.signature"
              type="primary"
              size="small"
              text
              @click.stop="verifyRecord(row)"
            >
              验证签名
            </el-button>
            <el-button
              v-else
              type="info"
              size="small"
              text
              @click.stop="showLogDetail(row)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadAuditLogs"
          @current-change="loadAuditLogs"
        />
      </div>
    </el-card>

    <!-- 记录详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="getDetailTitle()"
      width="600px"
      destroy-on-close
    >
      <div class="detail-content">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="label">操作类型</span>
              <span class="value">
                <el-tag :type="getOperationTagType(currentLog?.operationType)" size="small">
                  {{ currentLog?.operationLabel }}
                </el-tag>
              </span>
            </div>
            <div class="detail-item">
              <span class="label">试卷ID</span>
              <span class="value">#{{ currentLog?.paperId }}</span>
            </div>
            <div class="detail-item">
              <span class="label">试卷名称</span>
              <span class="value">{{ currentLog?.paperTitle || '未知' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">操作用户</span>
              <span class="value">{{ currentLog?.operatorName || '未知' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">用户角色</span>
              <span class="value">{{ getRoleLabel(currentLog?.operatorRole) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">操作时间</span>
              <span class="value">{{ formatTime(currentLog?.operationTime) }}</span>
            </div>
            <div class="detail-item" v-if="currentLog?.ipAddress">
              <span class="label">IP地址</span>
              <span class="value">{{ currentLog?.ipAddress }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section" v-if="currentLog?.blockchainTxId">
          <h4>区块链信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="label">交易ID</span>
              <span class="value">#{{ currentLog?.blockchainTxId }}</span>
            </div>
            <div class="detail-item">
              <span class="label">上链状态</span>
              <span class="value">
                <el-tag :type="currentLog?.isChained ? 'success' : 'warning'" size="small">
                  {{ currentLog?.isChained ? '已上链' : '待上链' }}
                </el-tag>
              </span>
            </div>
          </div>
        </div>

        <div class="detail-section" v-if="currentLog?.signature">
          <h4>数字签名</h4>
          <div class="signature-box">
            {{ currentLog?.signature?.substring(0, 100) }}...
          </div>
          <div class="verify-result" v-if="verifyResult !== null">
            <el-tag :type="verifyResult ? 'success' : 'danger'" size="large">
              {{ verifyResult ? '签名验证通过' : '签名验证失败' }}
            </el-tag>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
        <el-button
          v-if="currentLog?.operationType === 'DECRYPT' && currentLog?.signature"
          type="primary"
          @click="verifyRecord(currentLog)"
          :loading="verifying"
        >
          验证签名
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getLedgerStats, getAuditLogs, verifySignature } from '@/api/ledger'
import {
  Notebook,
  List,
  Plus,
  CircleCheck,
  Key,
  Document,
  Refresh,
  Avatar,
  Link
} from '@element-plus/icons-vue'

const stats = ref({})
const auditLogs = ref([])
const loadingStats = ref(false)
const loadingLogs = ref(false)
const verifying = ref(false)

const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const currentFilter = ref('ALL')

const showDetailDialog = ref(false)
const currentLog = ref(null)
const verifyResult = ref(null)

const filterLabels = {
  'CREATE': '创建试卷',
  'APPROVE': '审批操作',
  'DECRYPT': '解密查看'
}

const loadStats = async () => {
  try {
    loadingStats.value = true
    const response = await getLedgerStats()
    if (response.code === 200) {
      stats.value = response.data
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  } finally {
    loadingStats.value = false
  }
}

const loadAuditLogs = async () => {
  try {
    loadingLogs.value = true
    const response = await getAuditLogs(currentPage.value, pageSize.value, currentFilter.value)
    if (response.code === 200) {
      auditLogs.value = response.data.logs || []
      total.value = response.data.total || 0
    }
  } catch (error) {
    console.error('加载审计日志失败:', error)
  } finally {
    loadingLogs.value = false
  }
}

const filterByType = (type) => {
  currentFilter.value = type
  currentPage.value = 1
  loadAuditLogs()
}

const showLogDetail = (row) => {
  currentLog.value = row
  verifyResult.value = null
  showDetailDialog.value = true
}

const getDetailTitle = () => {
  if (!currentLog.value) return '记录详情'
  return `${currentLog.value.operationLabel} - 详情`
}

const verifyRecord = async (log) => {
  if (!log || !log.decryptRecordId) {
    ElMessage.warning('该记录不支持签名验证')
    return
  }
  try {
    verifying.value = true
    const response = await verifySignature(log.decryptRecordId)
    if (response.code === 200) {
      verifyResult.value = response.data.valid
      if (response.data.valid) {
        ElMessage.success('签名验证通过，确认为本人操作')
      } else {
        ElMessage.warning('签名验证失败，请注意！')
      }
    }
  } catch (error) {
    ElMessage.error('验证失败：' + error.message)
  } finally {
    verifying.value = false
  }
}

const getOperationTagType = (operationType) => {
  const typeMap = {
    'CREATE': '',           // 默认蓝色
    'APPROVE': 'success',   // 绿色
    'REJECT': 'danger',     // 红色
    'DECRYPT': 'warning'    // 橙色
  }
  return typeMap[operationType] || 'info'
}

const getRoleLabel = (role) => {
  const labelMap = {
    'teacher': '教师',
    'dept_admin': '系主任',
    'college_admin': '院长'
  }
  return labelMap[role] || role || '未知'
}

const getRoleTagType = (role) => {
  const typeMap = {
    'teacher': 'info',
    'dept_admin': 'warning',
    'college_admin': 'success'
  }
  return typeMap[role] || 'info'
}

const formatTime = (time) => {
  if (!time) return '-'
  if (typeof time === 'string') {
    return time.replace('T', ' ').substring(0, 19)
  }
  return time
}

onMounted(() => {
  loadStats()
  loadAuditLogs()
})
</script>

<style scoped>
.ledger-container {
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 50%, #f0f3f7 100%);
  min-height: 100vh;
}

.page-header {
  background: white;
  border-radius: 16px;
  padding: 24px 32px;
  margin-bottom: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  position: relative;
  overflow: hidden;
}

.page-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.header-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1d1d1f;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 14px;
  color: #6e6e73;
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  cursor: pointer;
  border: 2px solid transparent;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.stat-card.active {
  border-color: #667eea;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.2);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-icon.blue { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.stat-icon.green { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
.stat-icon.orange { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }
.stat-icon.purple { background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%); }

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1d1d1f;
  margin: 0;
}

.stat-label {
  font-size: 12px;
  color: #6e6e73;
  margin: 4px 0 0 0;
}

.records-card {
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.records-card :deep(.el-card__header) {
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-bottom: 2px solid #f0f0f5;
  padding: 16px 24px;
}

.records-card :deep(.el-card__body) {
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-left .header-icon {
  font-size: 20px;
  color: #667eea;
  width: auto;
  height: auto;
  background: none;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
}

.header-right {
  display: flex;
  align-items: center;
}

.paper-title {
  font-weight: 500;
  color: #1d1d1f;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-cell .el-icon {
  color: #667eea;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.detail-content {
  padding: 10px 0;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f5;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item .label {
  font-size: 12px;
  color: #6e6e73;
}

.detail-item .value {
  font-size: 14px;
  color: #1d1d1f;
  font-weight: 500;
}

.signature-box {
  font-family: 'SF Mono', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  background: #f5f5f7;
  padding: 12px;
  border-radius: 8px;
  word-break: break-all;
  color: #6e6e73;
}

.verify-result {
  margin-top: 16px;
  text-align: center;
}
</style>
