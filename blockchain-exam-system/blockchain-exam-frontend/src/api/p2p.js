/**
 * P2P 节点 API
 * 提供节点管理、网络状态、同步等功能
 */

import { get, post } from '@/utils/request'

/**
 * 获取所有节点
 */
export function getAllNodes() {
  return get('/p2p/nodes')
}

/**
 * 获取在线节点
 */
export function getOnlineNodes() {
  return get('/p2p/nodes/online')
}

/**
 * 获取邻居节点
 */
export function getNeighborNodes() {
  return get('/p2p/nodes/neighbors')
}

/**
 * 获取当前节点信息
 */
export function getCurrentNode() {
  return get('/p2p/nodes/current')
}

/**
 * 获取网络统计信息
 */
export function getNetworkStats() {
  return get('/p2p/network/stats')
}

/**
 * 获取同步统计信息
 */
export function getSyncStats() {
  return get('/p2p/sync/stats')
}

/**
 * 切换同步开关
 */
export function toggleSync() {
  return post('/p2p/sync/toggle')
}

/**
 * 心跳检测
 */
export function heartbeat() {
  return get('/p2p/heartbeat')
}

/**
 * 健康检查
 */
export function healthCheck() {
  return get('/p2p/health')
}
