<template>
  <div class="nodes-container">
    <!-- 当前节点信息 -->
    <div class="current-node-card">
      <div class="card-header">
        <h2 class="card-title">当前节点</h2>
        <div class="status-badge" :class="currentNode.status === 'ONLINE' ? 'online' : 'offline'">
          {{ currentNode.status === 'ONLINE' ? '在线' : '离线' }}
        </div>
      </div>

      <div class="node-info-grid">
        <div class="info-item">
          <span class="info-label">节点ID</span>
          <span class="info-value">{{ currentNode.nodeId || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">节点名称</span>
          <span class="info-value">{{ currentNode.nodeName || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">主机地址</span>
          <span class="info-value">{{ currentNode.host || '-' }}:{{ currentNode.port || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">部门</span>
          <span class="info-value">{{ currentNode.department || '-' }}</span>
        </div>
      </div>
    </div>

    <!-- 网络统计 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon blue">
          <el-icon :size="24"><Monitor /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ networkStats.totalNodes || 0 }}</p>
          <p class="stat-label">总节点数</p>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon green">
          <el-icon :size="24"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ networkStats.onlineNodes || 0 }}</p>
          <p class="stat-label">在线节点</p>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon orange">
          <el-icon :size="24"><Connection /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ networkStats.neighborCount || 0 }}</p>
          <p class="stat-label">邻居节点</p>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon purple">
          <el-icon :size="24"><Refresh /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ networkStats.syncCount || 0 }}</p>
          <p class="stat-label">同步次数</p>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button type="primary" @click="handleHeartbeat" :loading="heartbeating">
        <el-icon><Connection /></el-icon>
        发送心跳
      </el-button>
      <el-button @click="handleHealthCheck" :loading="checking">
        <el-icon><CircleCheck /></el-icon>
        健康检查
      </el-button>
      <el-button @click="loadData">
        <el-icon><RefreshRight /></el-icon>
        刷新数据
      </el-button>
    </div>

    <!-- 节点列表 -->
    <div class="nodes-section">
      <div class="section-header">
        <h2 class="section-title">P2P 网络节点</h2>
        <span class="section-subtitle">{{ nodes.length }} 个节点</span>
      </div>

      <div class="nodes-list" v-loading="loading">
        <div
          v-for="node in nodes"
          :key="node.nodeId"
          class="node-card"
          :class="{ 'is-current': node.nodeId === currentNode.nodeId }"
        >
          <div class="node-header">
            <div class="node-name">
              <el-icon><Monitor /></el-icon>
              {{ node.nodeName || node.nodeId }}
            </div>
            <div class="node-status" :class="node.status === 'ONLINE' ? 'online' : 'offline'">
              {{ node.status === 'ONLINE' ? '在线' : '离线' }}
            </div>
          </div>

          <div class="node-details">
            <div class="detail-item">
              <span class="detail-label">地址</span>
              <span class="detail-value">{{ node.host }}:{{ node.port }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">部门</span>
              <span class="detail-value">{{ node.department || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">最后心跳</span>
              <span class="detail-value">{{ formatTime(node.lastSeenTime) }}</span>
            </div>
          </div>

          <div class="node-tag" v-if="node.nodeId === currentNode.nodeId">
            当前节点
          </div>
        </div>

        <el-empty v-if="!loading && nodes.length === 0" description="暂无节点数据" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Monitor,
  CircleCheck,
  Connection,
  Refresh,
  RefreshRight
} from '@element-plus/icons-vue'
import {
  getAllNodes,
  getCurrentNode,
  getNetworkStats,
  heartbeat,
  healthCheck
} from '@/api/p2p'

// 状态
const loading = ref(false)
const heartbeating = ref(false)
const checking = ref(false)

// 数据
const nodes = ref([])
const currentNode = ref({
  nodeId: '',
  nodeName: '',
  host: '',
  port: '',
  department: '',
  status: 'OFFLINE'
})
const networkStats = ref({
  totalNodes: 0,
  onlineNodes: 0,
  neighborCount: 0,
  syncCount: 0
})

// 定时器
let refreshTimer = null

/**
 * 加载数据
 */
const loadData = async () => {
  loading.value = true
  try {
    // 并行加载数据
    const [nodesRes, currentRes, statsRes] = await Promise.all([
      getAllNodes().catch(() => ({ data: [] })),
      getCurrentNode().catch(() => ({ data: {} })),
      getNetworkStats().catch(() => ({ data: {} }))
    ])

    nodes.value = nodesRes.data || []
    currentNode.value = currentRes.data || {}

    const stats = statsRes.data || {}
    networkStats.value = {
      totalNodes: nodes.value.length,
      onlineNodes: nodes.value.filter(n => n.status === 'ONLINE').length,
      neighborCount: stats.neighborCount || 0,
      syncCount: stats.syncCount || 0
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

/**
 * 发送心跳
 */
const handleHeartbeat = async () => {
  heartbeating.value = true
  try {
    const res = await heartbeat()
    if (res.code === 200) {
      ElMessage.success('心跳发送成功')
      loadData()
    } else {
      ElMessage.warning(res.message || '心跳发送失败')
    }
  } catch (error) {
    ElMessage.error('心跳请求失败')
  } finally {
    heartbeating.value = false
  }
}

/**
 * 健康检查
 */
const handleHealthCheck = async () => {
  checking.value = true
  try {
    const res = await healthCheck()
    if (res.code === 200) {
      ElMessage.success('节点健康状态良好')
    } else {
      ElMessage.warning(res.message || '健康检查异常')
    }
  } catch (error) {
    ElMessage.error('健康检查请求失败')
  } finally {
    checking.value = false
  }
}

/**
 * 格式化时间
 */
const formatTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadData()
  // 每30秒自动刷新
  refreshTimer = setInterval(loadData, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.nodes-container {
  max-width: 1200px;
}

/* 当前节点卡片 */
.current-node-card {
  background: linear-gradient(135deg, #0071e3 0%, #005bb5 100%);
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 24px;
  color: white;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.online {
  background: rgba(52, 199, 89, 0.2);
  color: #4ade80;
}

.status-badge.offline {
  background: rgba(255, 59, 48, 0.2);
  color: #ff6b6b;
}

.node-info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  opacity: 0.7;
}

.info-value {
  font-size: 15px;
  font-weight: 500;
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

/* 节点列表 */
.nodes-section {
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

.nodes-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.node-card {
  position: relative;
  padding: 20px;
  background: var(--apple-gray-1, #f5f5f7);
  border-radius: 12px;
  transition: all 0.2s ease;
}

.node-card:hover {
  background: var(--apple-gray-2, #e8e8ed);
}

.node-card.is-current {
  background: rgba(0, 113, 227, 0.08);
  border: 1px solid rgba(0, 113, 227, 0.2);
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.node-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--apple-text-primary, #1d1d1f);
}

.node-status {
  padding: 4px 10px;
  border-radius: 100px;
  font-size: 11px;
  font-weight: 500;
}

.node-status.online {
  background: rgba(52, 199, 89, 0.12);
  color: #34c759;
}

.node-status.offline {
  background: rgba(255, 59, 48, 0.12);
  color: #ff3b30;
}

.node-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-label {
  font-size: 13px;
  color: var(--apple-text-tertiary, #86868b);
}

.detail-value {
  font-size: 13px;
  color: var(--apple-text-primary, #1d1d1f);
}

.node-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 2px 8px;
  background: #0071e3;
  color: white;
  font-size: 10px;
  font-weight: 500;
  border-radius: 100px;
}

/* 响应式 */
@media (max-width: 1024px) {
  .stats-grid,
  .node-info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .stats-grid,
  .node-info-grid {
    grid-template-columns: 1fr;
  }

  .action-bar {
    flex-wrap: wrap;
  }
}
</style>
