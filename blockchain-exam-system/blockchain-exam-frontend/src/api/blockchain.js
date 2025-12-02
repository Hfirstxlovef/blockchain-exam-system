/**
 * 区块链 API
 * 提供区块链查询、统计、验证等功能
 */

import { get, post } from '@/utils/request'

/**
 * 获取完整区块链
 */
export function getBlockchain() {
  return get('/blockchain/chain')
}

/**
 * 获取最新区块
 */
export function getLatestBlock() {
  return get('/blockchain/latest')
}

/**
 * 获取指定区块
 * @param {number} index - 区块索引
 */
export function getBlockByIndex(index) {
  return get(`/blockchain/block/${index}`)
}

/**
 * 获取区块链统计信息
 */
export function getBlockchainStats() {
  return get('/blockchain/stats')
}

/**
 * 验证区块链完整性
 */
export function validateBlockchain() {
  return get('/blockchain/validate')
}

/**
 * 手动触发挖矿
 */
export function triggerMining() {
  return post('/blockchain/mine')
}

/**
 * 手动触发同步
 */
export function triggerSync() {
  return post('/blockchain/sync')
}

/**
 * 获取挖矿状态
 */
export function getMiningStatus() {
  return get('/blockchain/mining/status')
}

/**
 * 切换挖矿开关
 */
export function toggleMining() {
  return post('/blockchain/mining/toggle')
}

/**
 * 获取待处理交易
 */
export function getPendingTransactions() {
  return get('/transaction/pending')
}

/**
 * 获取交易统计
 */
export function getTransactionStats() {
  return get('/transaction/stats')
}

/**
 * 获取区块中的交易
 * @param {number} blockIndex - 区块索引
 */
export function getBlockTransactions(blockIndex) {
  return get(`/transaction/block/${blockIndex}`)
}
