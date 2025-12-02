/**
 * 认证相关API
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */

import { post } from '@/utils/request'

/**
 * 用户登录（支持加密密码）
 * @param {string} username - 用户名
 * @param {string} encryptedPassword - 加密后的密码（Base64格式）
 * @param {number} timestamp - 时间戳
 * @param {string} nonce - 随机数（UUID）
 */
export function login(username, encryptedPassword, timestamp, nonce) {
  return post('/auth/login', {
    username,
    encryptedPassword,
    timestamp,
    nonce
  })
}

/**
 * 退出登录
 */
export function logout() {
  return post('/auth/logout')
}
