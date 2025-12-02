/**
 * 区块链账本API
 * 用于透明审计，所有用户都可以查看
 *
 * @author 网络信息安全大作业
 * @date 2025-11-29
 */

import { get, post } from '@/utils/request'

/**
 * 获取账本统计信息
 */
export function getLedgerStats() {
  return get('/blockchain/ledger/stats')
}

/**
 * 获取统一审计日志
 * @param {number} page - 页码（从1开始）
 * @param {number} size - 每页数量
 * @param {string} operationType - 操作类型过滤（ALL/CREATE/APPROVE/DECRYPT）
 */
export function getAuditLogs(page = 1, size = 20, operationType = 'ALL') {
  return get('/blockchain/ledger/audit-logs', { page, size, operationType })
}

/**
 * 分页查询解密记录
 * @param {number} page - 页码（从1开始）
 * @param {number} size - 每页数量
 */
export function getDecryptRecords(page = 1, size = 20) {
  return get('/blockchain/ledger/records', { page, size })
}

/**
 * 获取解密记录详情
 * @param {number} recordId - 记录ID
 */
export function getRecordDetail(recordId) {
  return get(`/blockchain/ledger/records/${recordId}`)
}

/**
 * 验证解密签名
 * @param {number} recordId - 记录ID
 */
export function verifySignature(recordId) {
  return post('/blockchain/ledger/verify-signature', { recordId })
}

/**
 * 获取试卷生命周期
 * @param {number} paperId - 试卷ID
 */
export function getPaperLifecycle(paperId) {
  return get(`/blockchain/ledger/paper-lifecycle/${paperId}`)
}

/**
 * 搜索交易
 * @param {object} params - 搜索参数
 * @param {string} params.type - 交易类型（可选）
 * @param {string} params.status - 交易状态（可选）
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页数量
 */
export function searchTransactions(params) {
  return get('/blockchain/ledger/transactions', params)
}

/**
 * 获取交易详情
 * @param {number} txId - 交易ID
 */
export function getTransactionDetail(txId) {
  return get(`/blockchain/ledger/transactions/${txId}`)
}
