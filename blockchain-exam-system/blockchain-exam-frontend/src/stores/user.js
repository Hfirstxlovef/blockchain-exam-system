/**
 * 用户状态管理
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getNodeUrl, NODE_MAP } from '@/config/nodes'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))
  const currentNodeUrl = ref(localStorage.getItem('nodeUrl') || NODE_MAP['default'])

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value.username || '')
  const realName = computed(() => userInfo.value.realName || '')
  const role = computed(() => userInfo.value.role || '')
  const department = computed(() => userInfo.value.department || '')

  // 角色判断
  const isTeacher = computed(() => role.value === 'teacher')
  const isDeptAdmin = computed(() => role.value === 'dept_admin')
  const isCollegeAdmin = computed(() => role.value === 'college_admin')
  const isAdmin = computed(() => isDeptAdmin.value || isCollegeAdmin.value)

  /**
   * 设置Token
   */
  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  /**
   * 设置用户信息
   */
  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
    // 根据用户院系自动切换节点
    if (info.department) {
      setNodeUrl(getNodeUrl(info.department))
    }
  }

  /**
   * 设置当前节点 URL
   */
  function setNodeUrl(url) {
    currentNodeUrl.value = url
    localStorage.setItem('nodeUrl', url)
  }

  /**
   * 清除认证信息
   */
  function clearAuth() {
    token.value = ''
    userInfo.value = {}
    currentNodeUrl.value = NODE_MAP['default']
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('nodeUrl')
  }

  /**
   * 更新用户信息
   */
  function updateUserInfo(updates) {
    userInfo.value = { ...userInfo.value, ...updates }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  return {
    // 状态
    token,
    userInfo,
    currentNodeUrl,

    // 计算属性
    isLoggedIn,
    username,
    realName,
    role,
    department,
    isTeacher,
    isDeptAdmin,
    isCollegeAdmin,
    isAdmin,

    // 方法
    setToken,
    setUserInfo,
    setNodeUrl,
    clearAuth,
    updateUserInfo
  }
}, {
  persist: false // 已使用localStorage，不需要额外持久化
})
