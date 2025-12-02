/**
 * 审批管理API
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */

import { get, post } from '@/utils/request'

/**
 * 获取待审批试卷列表
 */
export function getPendingPapers() {
  return get('/approval/pending')
}

/**
 * 审批通过
 * @param {object} data - 审批数据
 * @param {number} data.paperId - 试卷ID
 * @param {string} data.comment - 审批意见
 * @param {string} data.privateKey - 用户RSA私钥（Base64编码）
 */
export function approvePaper(data) {
  return post('/approval/approve', data)
}

/**
 * 审批驳回
 * @param {object} data - 审批数据
 * @param {number} data.paperId - 试卷ID
 * @param {string} data.comment - 驳回原因
 * @param {string} data.privateKey - 用户RSA私钥（Base64编码）
 */
export function rejectPaper(data) {
  return post('/approval/reject', data)
}

/**
 * 查询审批记录
 * @param {number} paperId - 试卷ID
 */
export function getApprovalRecords(paperId) {
  return get(`/approval/records/${paperId}`)
}

/**
 * 验证数字签名
 * @param {number} recordId - 审批记录ID
 */
export function verifySignature(recordId) {
  return post(`/approval/verify/${recordId}`)
}
