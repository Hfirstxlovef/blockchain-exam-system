<template>
  <div class="blockchain-container">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon blue">
          <el-icon :size="24"><Box /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ stats.blockCount || 0 }}</p>
          <p class="stat-label">区块总数</p>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon green">
          <el-icon :size="24"><Tickets /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ stats.transactionCount || 0 }}</p>
          <p class="stat-label">交易总数</p>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon orange">
          <el-icon :size="24"><Clock /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ stats.pendingCount || 0 }}</p>
          <p class="stat-label">待打包交易</p>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon purple">
          <el-icon :size="24"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ stats.isValid ? '有效' : '无效' }}</p>
          <p class="stat-label">链完整性</p>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button type="primary" @click="handleMine" :loading="mining">
        <el-icon><Cpu /></el-icon>
        手动挖矿
      </el-button>
      <el-button @click="handleSync" :loading="syncing">
        <el-icon><Refresh /></el-icon>
        同步区块链
      </el-button>
      <el-button @click="handleValidate" :loading="validating">
        <el-icon><Select /></el-icon>
        验证区块链
      </el-button>
      <el-button @click="loadData">
        <el-icon><RefreshRight /></el-icon>
        刷新数据
      </el-button>
    </div>

    <!-- 区块列表 -->
    <div class="blocks-section">
      <div class="section-header">
        <h2 class="section-title">区块列表</h2>
        <span class="section-subtitle">按时间倒序排列</span>
      </div>

      <div class="blocks-list" v-loading="loading">
        <div
          v-for="block in blocks"
          :key="block.blockIndex"
          class="block-card"
          @click="showBlockDetail(block)"
        >
          <div class="block-header">
            <div class="block-index">#{{ block.blockIndex }}</div>
            <div class="block-time">{{ formatTime(block.timestamp) }}</div>
          </div>

          <div class="block-hash">
            <span class="hash-label">哈希</span>
            <span class="hash-value">{{ truncateHash(block.hash) }}</span>
          </div>

          <div class="block-hash">
            <span class="hash-label">前置</span>
            <span class="hash-value">{{ truncateHash(block.previousHash) }}</span>
          </div>

          <div class="block-meta">
            <div class="meta-item">
              <span class="meta-label">Nonce</span>
              <span class="meta-value">{{ block.nonce }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">难度</span>
              <span class="meta-value">{{ block.difficulty }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">交易数</span>
              <span class="meta-value">{{ getTransactionCount(block) }}</span>
            </div>
          </div>
        </div>

        <el-empty v-if="!loading && blocks.length === 0" description="暂无区块数据" />
      </div>
    </div>

    <!-- 区块详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="区块详情"
      width="700px"
      class="block-detail-dialog"
    >
      <div v-if="selectedBlock" class="block-detail">
        <div class="detail-row">
          <span class="detail-label">区块索引</span>
          <span class="detail-value">#{{ selectedBlock.blockIndex }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">时间戳</span>
          <span class="detail-value">{{ formatTime(selectedBlock.timestamp) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">区块哈希</span>
          <span class="detail-value hash">{{ selectedBlock.hash }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">前置哈希</span>
          <span class="detail-value hash">{{ selectedBlock.previousHash }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">Nonce</span>
          <span class="detail-value">{{ selectedBlock.nonce }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">难度</span>
          <span class="detail-value">{{ selectedBlock.difficulty }}</span>
        </div>
        <div class="detail-row full">
          <span class="detail-label">区块数据</span>
          <pre class="detail-value data">{{ formatBlockData(selectedBlock.data) }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Box,
  Tickets,
  Clock,
  CircleCheck,
  Cpu,
  Refresh,
  Select,
  RefreshRight
} from '@element-plus/icons-vue'
import {
  getBlockchain,
  getBlockchainStats,
  validateBlockchain,
  triggerMining,
  triggerSync,
  getPendingTransactions
} from '@/api/blockchain'

// 状态
const loading = ref(false)
const mining = ref(false)
const syncing = ref(false)
const validating = ref(false)

// 数据
const blocks = ref([])
const stats = ref({
  blockCount: 0,
  transactionCount: 0,
  pendingCount: 0,
  isValid: true
})

// 区块详情
const detailVisible = ref(false)
const selectedBlock = ref(null)

/**
 * 加载数据
 */
const loadData = async () => {
  loading.value = true
  try {
    // 并行加载区块链和统计数据
    const [chainRes, statsRes, pendingRes] = await Promise.all([
      getBlockchain().catch(() => ({ data: [] })),
      getBlockchainStats().catch(() => ({ data: {} })),
      getPendingTransactions().catch(() => ({ data: [] }))
    ])

    // 区块列表（倒序）
    blocks.value = (chainRes.data || []).reverse()

    // 统计数据
    const statsData = statsRes.data || {}
    stats.value = {
      blockCount: statsData.blockCount || blocks.value.length,
      transactionCount: statsData.transactionCount || 0,
      pendingCount: pendingRes.data?.length || 0,
      isValid: true
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

/**
 * 手动挖矿
 */
const handleMine = async () => {
  mining.value = true
  try {
    const res = await triggerMining()
    if (res.code === 200) {
      ElMessage.success('挖矿成功')
      loadData()
    } else {
      ElMessage.warning(res.message || '挖矿失败')
    }
  } catch (error) {
    ElMessage.error('挖矿请求失败')
  } finally {
    mining.value = false
  }
}

/**
 * 同步区块链
 */
const handleSync = async () => {
  syncing.value = true
  try {
    const res = await triggerSync()
    if (res.code === 200) {
      ElMessage.success('同步成功')
      loadData()
    } else {
      ElMessage.warning(res.message || '同步失败')
    }
  } catch (error) {
    ElMessage.error('同步请求失败')
  } finally {
    syncing.value = false
  }
}

/**
 * 验证区块链
 */
const handleValidate = async () => {
  validating.value = true
  try {
    const res = await validateBlockchain()
    if (res.code === 200 && res.data) {
      stats.value.isValid = true
      ElMessage.success('区块链验证通过，数据完整')
    } else {
      stats.value.isValid = false
      ElMessage.error('区块链验证失败，数据可能被篡改')
    }
  } catch (error) {
    ElMessage.error('验证请求失败')
  } finally {
    validating.value = false
  }
}

/**
 * 显示区块详情
 */
const showBlockDetail = (block) => {
  selectedBlock.value = block
  detailVisible.value = true
}

/**
 * 截断哈希显示
 */
const truncateHash = (hash) => {
  if (!hash) return '-'
  if (hash.length <= 16) return hash
  return hash.substring(0, 8) + '...' + hash.substring(hash.length - 8)
}

/**
 * 格式化时间
 */
const formatTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN')
}

/**
 * 获取交易数量
 */
const getTransactionCount = (block) => {
  if (!block.data) return 0
  try {
    const data = typeof block.data === 'string' ? JSON.parse(block.data) : block.data
    return data.transactions?.length || 0
  } catch {
    return 0
  }
}

/**
 * 格式化区块数据
 */
const formatBlockData = (data) => {
  if (!data) return '-'
  try {
    const parsed = typeof data === 'string' ? JSON.parse(data) : data
    return JSON.stringify(parsed, null, 2)
  } catch {
    return data
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.blockchain-container {
  max-width: 1200px;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #ffffff;
  border-radius: 16px;
  border: 0.5px solid var(--apple-border, rgba(0, 0, 0, 0.08));
  box-shadow: var(--shadow-card, 0 2px 8px rgba(0, 0, 0, 0.04));
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

.stat-icon.blue { background: linear-gradient(135deg, #0071e3 0%, #005bb5 100%); }
.stat-icon.green { background: linear-gradient(135deg, #34c759 0%, #28a745 100%); }
.stat-icon.orange { background: linear-gradient(135deg, #ff9500 0%, #ff7b00 100%); }
.stat-icon.purple { background: linear-gradient(135deg, #af52de 0%, #9333ea 100%); }

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--apple-text-primary, #1d1d1f);
  margin: 0;
}

.stat-label {
  font-size: 13px;
  color: var(--apple-text-secondary, #6e6e73);
  margin: 4px 0 0 0;
}

/* 操作按钮 */
.action-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

/* 区块列表 */
.blocks-section {
  background: #ffffff;
  border-radius: 16px;
  border: 0.5px solid var(--apple-border, rgba(0, 0, 0, 0.08));
  box-shadow: var(--shadow-card, 0 2px 8px rgba(0, 0, 0, 0.04));
  padding: 24px;
}

.section-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-text-primary, #1d1d1f);
  margin: 0;
}

.section-subtitle {
  font-size: 13px;
  color: var(--apple-text-tertiary, #86868b);
}

.blocks-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.block-card {
  padding: 16px;
  background: var(--apple-gray-1, #f5f5f7);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.block-card:hover {
  background: var(--apple-gray-2, #e8e8ed);
  transform: translateY(-2px);
}

.block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.block-index {
  font-size: 16px;
  font-weight: 600;
  color: #0071e3;
}

.block-time {
  font-size: 12px;
  color: var(--apple-text-tertiary, #86868b);
}

.block-hash {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
}

.hash-label {
  color: var(--apple-text-tertiary, #86868b);
  min-width: 32px;
}

.hash-value {
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--apple-text-secondary, #6e6e73);
}

.block-meta {
  display: flex;
  gap: 16px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--apple-gray-2, #e8e8ed);
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-label {
  font-size: 11px;
  color: var(--apple-text-tertiary, #86868b);
}

.meta-value {
  font-size: 13px;
  font-weight: 500;
  color: var(--apple-text-primary, #1d1d1f);
}

/* 区块详情对话框 */
.block-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.detail-row.full {
  flex-direction: column;
}

.detail-label {
  min-width: 80px;
  font-size: 13px;
  color: var(--apple-text-tertiary, #86868b);
}

.detail-value {
  font-size: 14px;
  color: var(--apple-text-primary, #1d1d1f);
  word-break: break-all;
}

.detail-value.hash {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 12px;
}

.detail-value.data {
  width: 100%;
  max-height: 300px;
  overflow: auto;
  padding: 12px;
  background: var(--apple-gray-1, #f5f5f7);
  border-radius: 8px;
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 12px;
  white-space: pre-wrap;
}

/* 响应式 */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .action-bar {
    flex-wrap: wrap;
  }
}
</style>
