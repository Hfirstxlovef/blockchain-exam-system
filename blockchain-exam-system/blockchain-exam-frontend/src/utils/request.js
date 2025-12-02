/**
 * Axios请求封装
 *
 * 功能：
 * 1. 统一配置baseURL和超时
 * 2. 请求拦截：自动添加Token
 * 3. 响应拦截：统一错误处理
 * 4. 支持可选的请求/响应加密
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */

import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// 创建axios实例
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000, // 30秒超时
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    const userStore = useUserStore()

    // 1. 动态设置 baseURL（根据用户院系切换节点）
    if (userStore.currentNodeUrl) {
      config.baseURL = userStore.currentNodeUrl
    }

    // 2. 添加Token
    const token = userStore.token

    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }

    // 3. 打印请求日志（开发环境）
    if (import.meta.env.DEV) {
      console.log('[Request]', config.method.toUpperCase(), config.baseURL + config.url, {
        params: config.params,
        data: config.data
      })
    }

    return config
  },
  error => {
    console.error('[Request Error]', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data

    // 打印响应日志（开发环境）
    if (import.meta.env.DEV) {
      console.log('[Response]', response.config.url, res)
    }

    // 处理文件下载
    if (response.config.responseType === 'blob') {
      return response
    }

    // 检查业务状态码
    if (res.code !== 200) {
      // 显示错误消息
      ElMessage.error(res.message || '请求失败')

      // 401未授权：清除登录信息并跳转登录页
      if (res.code === 401) {
        handleUnauthorized()
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res
  },
  error => {
    console.error('[Response Error]', error)

    let message = '网络请求失败'

    if (error.response) {
      const status = error.response.status

      switch (status) {
        case 400:
          message = '请求参数错误'
          break
        case 401:
          message = '未授权，请重新登录'
          handleUnauthorized()
          break
        case 403:
          message = '拒绝访问，权限不足'
          break
        case 404:
          message = '请求的资源不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
        case 502:
          message = '网关错误'
          break
        case 503:
          message = '服务不可用'
          break
        case 504:
          message = '网关超时'
          break
        default:
          message = error.response.data?.message || `请求失败（${status}）`
      }
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
    } else if (error.message === 'Network Error') {
      message = '网络连接失败，请检查网络'
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

/**
 * 处理未授权情况
 */
function handleUnauthorized() {
  const userStore = useUserStore()

  // 清除登录信息
  userStore.clearAuth()

  // 如果不在登录页，跳转到登录页
  if (router.currentRoute.value.path !== '/login') {
    ElMessageBox.confirm('登录已过期，请重新登录', '提示', {
      confirmButtonText: '重新登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      router.push('/login')
    }).catch(() => {
      router.push('/login')
    })
  }
}

/**
 * 通用GET请求
 * @param {string} url - 请求URL
 * @param {object} params - 查询参数
 * @param {object} config - 额外配置
 */
export function get(url, params = {}, config = {}) {
  return service.get(url, { params, ...config })
}

/**
 * 通用POST请求
 * @param {string} url - 请求URL
 * @param {object} data - 请求体数据
 * @param {object} config - 额外配置
 */
export function post(url, data = {}, config = {}) {
  return service.post(url, data, config)
}

/**
 * 通用PUT请求
 * @param {string} url - 请求URL
 * @param {object} data - 请求体数据
 * @param {object} config - 额外配置
 */
export function put(url, data = {}, config = {}) {
  return service.put(url, data, config)
}

/**
 * 通用DELETE请求
 * @param {string} url - 请求URL
 * @param {object} params - 查询参数
 * @param {object} config - 额外配置
 */
export function del(url, params = {}, config = {}) {
  return service.delete(url, { params, ...config })
}

/**
 * 文件上传
 * @param {string} url - 上传URL
 * @param {FormData} formData - 表单数据
 * @param {function} onProgress - 上传进度回调
 */
export function upload(url, formData, onProgress) {
  return service.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: progressEvent => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    }
  })
}

/**
 * 文件下载
 * @param {string} url - 下载URL
 * @param {object} params - 查询参数
 * @param {string} filename - 保存的文件名
 */
export function download(url, params = {}, filename) {
  return service.get(url, {
    params,
    responseType: 'blob'
  }).then(response => {
    // 创建blob链接
    const blob = new Blob([response.data])
    const downloadUrl = window.URL.createObjectURL(blob)

    // 创建a标签触发下载
    const link = document.createElement('a')
    link.href = downloadUrl

    // 从响应头获取文件名
    if (!filename) {
      const contentDisposition = response.headers['content-disposition']
      if (contentDisposition) {
        const match = contentDisposition.match(/filename\*?=(?:UTF-8'')?([^;]+)/)
        if (match) {
          filename = decodeURIComponent(match[1].replace(/['"]/g, ''))
        }
      }
    }

    link.download = filename || 'download'
    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)

    ElMessage.success('下载成功')
  }).catch(error => {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  })
}

/**
 * 默认导出axios实例
 */
export default service
