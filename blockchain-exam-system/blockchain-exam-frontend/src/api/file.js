/**
 * 文件管理API
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */

import { upload, download, del } from '@/utils/request'

/**
 * 上传文件
 * @param {File} file - 文件对象
 * @param {function} onProgress - 上传进度回调
 */
export function uploadFile(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return upload('/file/upload', formData, onProgress)
}

/**
 * 下载文件
 * @param {string} path - 文件路径
 * @param {string} filename - 保存的文件名
 */
export function downloadFile(path, filename) {
  return download('/file/download', { path }, filename)
}

/**
 * 删除文件
 * @param {string} path - 文件路径
 */
export function deleteFile(path) {
  return del('/file/delete', { path })
}
