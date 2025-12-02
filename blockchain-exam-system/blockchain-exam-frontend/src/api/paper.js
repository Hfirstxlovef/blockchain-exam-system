/**
 * 试卷管理API
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */

import { get, post, put, del } from '@/utils/request'
import { PaperCrypto } from '@/utils/crypto'

/**
 * 创建试卷（加密数据 + 数字签名）
 * @param {object} data - 试卷数据
 * @param {string} data.courseName - 课程名称
 * @param {string} data.examType - 考试类型
 * @param {string} data.semester - 学期
 * @param {string} data.department - 院系
 * @param {string} data.content - 试卷内容
 * @param {string} data.filePath - 文件路径（可选）
 * @param {string} data.privateKey - 用户RSA私钥（Base64编码，用于数字签名验证）
 */
export async function createPaper(data) {
  // 分离私钥和试卷数据
  const { privateKey, ...paperData } = data
  // 加密试卷数据
  const encryptedData = await PaperCrypto.encryptPaper(paperData)
  // 将私钥一起发送用于签名验证
  encryptedData.privateKey = privateKey
  return post('/exam-paper/create', encryptedData)
}

/**
 * 提交审批
 * @param {number} id - 试卷ID
 */
export function submitPaper(id) {
  return post(`/exam-paper/submit/${id}`)
}

/**
 * 查询我的试卷
 * @param {string} status - 状态（可选）
 */
export function getMyPapers(status) {
  return get('/exam-paper/my-papers', { status })
}

/**
 * 查询所有试卷（管理员）
 * @param {string} status - 状态（可选）
 * @param {string} department - 院系（可选）
 */
export function getAllPapers(status, department) {
  return get('/exam-paper/all', { status, department })
}

/**
 * 查询试卷详情
 * @param {number} id - 试卷ID
 */
export function getPaperDetail(id) {
  return get(`/exam-paper/${id}`)
}

/**
 * 更新试卷
 * @param {number} id - 试卷ID
 * @param {object} data - 试卷数据
 */
export function updatePaper(id, data) {
  return put(`/exam-paper/${id}`, data)
}

/**
 * 删除试卷
 * @param {number} id - 试卷ID
 */
export function deletePaper(id) {
  return del(`/exam-paper/${id}`)
}

/**
 * 统计信息
 */
export function getStatistics() {
  return get('/exam-paper/statistics')
}

/**
 * 获取试卷加密信息
 * @param {number} id - 试卷ID
 */
export function getPaperCryptoInfo(id) {
  return get(`/exam-paper/${id}/crypto-info`)
}

/**
 * 解密试卷内容（用户私钥解密）
 * @param {number} paperId - 试卷ID
 * @param {string} privateKey - 用户RSA私钥（Base64编码）
 */
export function decryptPaper(paperId, privateKey) {
  return post('/exam-paper/decrypt', { paperId, privateKey })
}

/**
 * 服务端解密试卷内容（审批人专用）
 * 无需提供私钥，服务端使用系统私钥解密
 * @param {number} paperId - 试卷ID
 * @deprecated 不符合区块链透明理念，请使用 decryptPaperWithAudit 替代
 */
export function decryptPaperBySystem(paperId) {
  return get(`/exam-paper/${paperId}/decrypt-by-system`)
}

/**
 * 解密试卷内容（带区块链审计）
 * 使用用户私钥解密，并记录操作到区块链
 * @param {number} paperId - 试卷ID
 * @param {string} privateKey - 用户RSA私钥（Base64编码）
 */
export function decryptPaperWithAudit(paperId, privateKey) {
  return post('/exam-paper/decrypt-with-audit', { paperId, privateKey })
}

/**
 * 查询试卷的解密记录
 * @param {number} paperId - 试卷ID
 */
export function getDecryptRecords(paperId) {
  return get(`/exam-paper/${paperId}/decrypt-records`)
}

/**
 * 验证解密签名
 * @param {number} recordId - 解密记录ID
 */
export function verifyDecryptSignature(recordId) {
  return post('/exam-paper/verify-decrypt-signature', { recordId })
}
