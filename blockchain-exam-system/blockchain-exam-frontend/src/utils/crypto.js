/**
 * 前端加密工具类
 *
 * 功能：
 * 1. AES加密/解密（对称加密，用于数据传输）
 * 2. RSA加密/解密（非对称加密，用于密钥协商）
 * 3. SM3哈希（国密标准）
 * 4. 与后端加密体系配套
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */

import CryptoJS from 'crypto-js'
import JSEncrypt from 'jsencrypt'

/**
 * AES加密工具
 */
export const AES = {
  /**
   * AES-256-CBC加密
   * @param {string} plainText - 明文
   * @param {string} key - 密钥（Base64格式）
   * @returns {string} 密文（Base64格式）
   */
  encrypt(plainText, key) {
    try {
      // 将Base64密钥转换为WordArray
      const keyWordArray = CryptoJS.enc.Base64.parse(key)

      // 生成随机IV
      const iv = CryptoJS.lib.WordArray.random(16)

      // 加密
      const encrypted = CryptoJS.AES.encrypt(plainText, keyWordArray, {
        iv: iv,
        mode: CryptoJS.mode.CBC,
        padding: CryptoJS.pad.Pkcs7
      })

      // 拼接IV和密文：IV(16字节) + 密文
      const combined = iv.concat(encrypted.ciphertext)

      // 返回Base64编码
      return CryptoJS.enc.Base64.stringify(combined)
    } catch (error) {
      console.error('AES加密失败:', error)
      throw new Error('数据加密失败')
    }
  },

  /**
   * AES-256-CBC解密
   * @param {string} cipherText - 密文（Base64格式）
   * @param {string} key - 密钥（Base64格式）
   * @returns {string} 明文
   */
  decrypt(cipherText, key) {
    try {
      // 将Base64密钥转换为WordArray
      const keyWordArray = CryptoJS.enc.Base64.parse(key)

      // 解析密文
      const combined = CryptoJS.enc.Base64.parse(cipherText)

      // 提取IV（前16字节）
      const iv = CryptoJS.lib.WordArray.create(combined.words.slice(0, 4))

      // 提取密文（16字节后）
      const encrypted = CryptoJS.lib.WordArray.create(combined.words.slice(4))

      // 解密
      const decrypted = CryptoJS.AES.decrypt(
        { ciphertext: encrypted },
        keyWordArray,
        {
          iv: iv,
          mode: CryptoJS.mode.CBC,
          padding: CryptoJS.pad.Pkcs7
        }
      )

      // 返回UTF-8字符串
      return decrypted.toString(CryptoJS.enc.Utf8)
    } catch (error) {
      console.error('AES解密失败:', error)
      throw new Error('数据解密失败')
    }
  },

  /**
   * 生成AES密钥
   * @returns {string} Base64格式的密钥
   */
  generateKey() {
    // 生成256位随机密钥
    const key = CryptoJS.lib.WordArray.random(32)
    return CryptoJS.enc.Base64.stringify(key)
  }
}

/**
 * RSA加密工具
 */
export const RSA = {
  /**
   * RSA公钥加密
   * @param {string} plainText - 明文
   * @param {string} publicKey - 公钥（PEM格式）
   * @returns {string} 密文（Base64格式）
   */
  encrypt(plainText, publicKey) {
    try {
      const encrypt = new JSEncrypt()
      encrypt.setPublicKey(publicKey)
      const encrypted = encrypt.encrypt(plainText)

      if (!encrypted) {
        throw new Error('RSA加密失败')
      }

      return encrypted
    } catch (error) {
      console.error('RSA加密失败:', error)
      throw new Error('密钥加密失败')
    }
  },

  /**
   * RSA私钥解密
   * @param {string} cipherText - 密文（Base64格式）
   * @param {string} privateKey - 私钥（PEM格式）
   * @returns {string} 明文
   */
  decrypt(cipherText, privateKey) {
    try {
      const decrypt = new JSEncrypt()
      decrypt.setPrivateKey(privateKey)
      const decrypted = decrypt.decrypt(cipherText)

      if (!decrypted) {
        throw new Error('RSA解密失败')
      }

      return decrypted
    } catch (error) {
      console.error('RSA解密失败:', error)
      throw new Error('密钥解密失败')
    }
  },

  /**
   * RSA签名（使用私钥）
   * @param {string} data - 待签名数据
   * @param {string} privateKey - 私钥（PEM格式）
   * @returns {string} 签名（Base64格式）
   */
  sign(data, privateKey) {
    try {
      const sign = new JSEncrypt()
      sign.setPrivateKey(privateKey)
      const signature = sign.sign(data, CryptoJS.SHA256, 'sha256')

      if (!signature) {
        throw new Error('RSA签名失败')
      }

      return signature
    } catch (error) {
      console.error('RSA签名失败:', error)
      throw new Error('数字签名失败')
    }
  },

  /**
   * RSA验签（使用公钥）
   * @param {string} data - 原始数据
   * @param {string} signature - 签名（Base64格式）
   * @param {string} publicKey - 公钥（PEM格式）
   * @returns {boolean} 验证结果
   */
  verify(data, signature, publicKey) {
    try {
      const verify = new JSEncrypt()
      verify.setPublicKey(publicKey)
      return verify.verify(data, signature, CryptoJS.SHA256)
    } catch (error) {
      console.error('RSA验签失败:', error)
      return false
    }
  }
}

/**
 * SM3哈希工具
 */
export const SM3 = {
  /**
   * SM3哈希（使用SHA-256模拟，实际项目应使用sm-crypto库）
   * @param {string} data - 数据
   * @returns {string} 哈希值（16进制）
   */
  hash(data) {
    try {
      // 注意：这里使用SHA-256模拟SM3
      // 实际项目应该使用 sm-crypto 库的真实SM3实现
      return CryptoJS.SHA256(data).toString()
    } catch (error) {
      console.error('SM3哈希失败:', error)
      throw new Error('哈希计算失败')
    }
  },

  /**
   * SM3-HMAC
   * @param {string} data - 数据
   * @param {string} key - 密钥
   * @returns {string} HMAC值（16进制）
   */
  hmac(data, key) {
    try {
      return CryptoJS.HmacSHA256(data, key).toString()
    } catch (error) {
      console.error('SM3-HMAC失败:', error)
      throw new Error('HMAC计算失败')
    }
  }
}

/**
 * Base64工具
 */
export const Base64 = {
  /**
   * Base64编码
   * @param {string} str - 字符串
   * @returns {string} Base64编码
   */
  encode(str) {
    return CryptoJS.enc.Base64.stringify(CryptoJS.enc.Utf8.parse(str))
  },

  /**
   * Base64解码
   * @param {string} base64 - Base64编码
   * @returns {string} 字符串
   */
  decode(base64) {
    return CryptoJS.enc.Base64.parse(base64).toString(CryptoJS.enc.Utf8)
  }
}

/**
 * 密码强度检查
 */
export const PasswordStrength = {
  /**
   * 检查密码强度
   * @param {string} password - 密码
   * @returns {object} { level: 0-4, message: string }
   */
  check(password) {
    if (!password) {
      return { level: 0, message: '密码不能为空' }
    }

    let level = 0
    const checks = {
      length: password.length >= 8,
      lowercase: /[a-z]/.test(password),
      uppercase: /[A-Z]/.test(password),
      number: /\d/.test(password),
      special: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)
    }

    // 计算强度等级
    if (checks.length) level++
    if (checks.lowercase) level++
    if (checks.uppercase) level++
    if (checks.number) level++
    if (checks.special) level++

    const messages = {
      0: '密码太弱',
      1: '密码太弱',
      2: '密码较弱',
      3: '密码中等',
      4: '密码较强',
      5: '密码很强'
    }

    return {
      level,
      message: messages[level],
      checks
    }
  }
}

/**
 * 安全随机数生成
 */
export const Random = {
  /**
   * 生成随机字符串
   * @param {number} length - 长度
   * @returns {string} 随机字符串
   */
  string(length = 32) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    let result = ''
    for (let i = 0; i < length; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    return result
  },

  /**
   * 生成UUID
   * @returns {string} UUID
   */
  uuid() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0
      const v = c === 'x' ? r : (r & 0x3 | 0x8)
      return v.toString(16)
    })
  }
}

/**
 * 登录加密工具
 * 用于加密登录密码，防止明文传输
 */
export const LoginCrypto = {
  // 缓存的公钥
  _cachedPublicKey: null,

  /**
   * Base64格式转PEM格式
   * @param {string} base64Key - Base64格式的公钥
   * @returns {string} PEM格式的公钥
   */
  base64ToPem(base64Key) {
    // 将Base64密钥按64字符分行
    const lines = []
    for (let i = 0; i < base64Key.length; i += 64) {
      lines.push(base64Key.substring(i, i + 64))
    }

    // 添加PEM头尾
    return '-----BEGIN PUBLIC KEY-----\n' +
           lines.join('\n') +
           '\n-----END PUBLIC KEY-----'
  },

  /**
   * 获取系统RSA公钥
   * @returns {Promise<string>} PEM格式的公钥
   */
  async getPublicKey() {
    // 如果已缓存，直接返回
    if (this._cachedPublicKey) {
      return this._cachedPublicKey
    }

    try {
      // 从后端获取公钥
      const response = await fetch('/api/auth/public-key')
      const result = await response.json()

      if (result.code === 200 && result.data && result.data.publicKey) {
        // 将Base64格式转换为PEM格式
        const pemKey = this.base64ToPem(result.data.publicKey)
        // 缓存公钥
        this._cachedPublicKey = pemKey
        return pemKey
      } else {
        throw new Error('获取公钥失败: ' + (result.message || '未知错误'))
      }
    } catch (error) {
      console.error('获取公钥失败:', error)
      throw new Error('获取公钥失败，请刷新页面重试')
    }
  },

  /**
   * 加密登录密码
   * @param {string} password - 原始密码
   * @returns {Promise<object>} { encryptedPassword, timestamp, nonce }
   */
  async encryptPassword(password) {
    try {
      // 获取公钥
      const publicKey = await this.getPublicKey()

      // 生成时间戳和nonce
      const timestamp = Date.now()
      const nonce = Random.uuid()

      // 使用RSA公钥加密密码
      const encryptedPassword = RSA.encrypt(password, publicKey)

      return {
        encryptedPassword,
        timestamp,
        nonce
      }
    } catch (error) {
      console.error('密码加密失败:', error)
      throw new Error('密码加密失败: ' + error.message)
    }
  },

  /**
   * 清除缓存的公钥
   * 用于强制重新获取公钥
   */
  clearCache() {
    this._cachedPublicKey = null
  }
}

/**
 * 试卷创建加密工具
 * 用于加密试卷字段，混合使用RSA和AES加密
 */
export const PaperCrypto = {
  // 缓存的公钥（复用LoginCrypto的公钥）
  _cachedPublicKey: null,

  /**
   * Base64格式转PEM格式
   * @param {string} base64Key - Base64格式的公钥
   * @returns {string} PEM格式的公钥
   */
  base64ToPem(base64Key) {
    const lines = []
    for (let i = 0; i < base64Key.length; i += 64) {
      lines.push(base64Key.substring(i, i + 64))
    }
    return '-----BEGIN PUBLIC KEY-----\n' +
           lines.join('\n') +
           '\n-----END PUBLIC KEY-----'
  },

  /**
   * 获取系统RSA公钥
   * @returns {Promise<string>} PEM格式的公钥
   */
  async getPublicKey() {
    // 优先使用LoginCrypto缓存的公钥
    if (LoginCrypto._cachedPublicKey) {
      return LoginCrypto._cachedPublicKey
    }

    // 如果已缓存，直接返回
    if (this._cachedPublicKey) {
      return this._cachedPublicKey
    }

    try {
      // 从后端获取公钥
      const response = await fetch('/api/auth/public-key')
      const result = await response.json()

      if (result.code === 200 && result.data && result.data.publicKey) {
        const pemKey = this.base64ToPem(result.data.publicKey)
        this._cachedPublicKey = pemKey
        // 同时缓存到LoginCrypto
        LoginCrypto._cachedPublicKey = pemKey
        return pemKey
      } else {
        throw new Error('获取公钥失败: ' + (result.message || '未知错误'))
      }
    } catch (error) {
      console.error('获取公钥失败:', error)
      throw new Error('获取公钥失败，请刷新页面重试')
    }
  },

  /**
   * 加密试卷数据
   * 混合加密方案：小字段用RSA，大字段用AES
   *
   * @param {object} paperData - 试卷数据
   * @param {string} paperData.courseName - 课程名称
   * @param {string} paperData.examType - 考试类型
   * @param {string} paperData.semester - 学期
   * @param {string} paperData.department - 院系
   * @param {string} paperData.content - 试卷内容（可能很长）
   * @param {string} paperData.filePath - 文件路径（可选）
   * @returns {Promise<object>} 加密后的数据
   */
  async encryptPaper(paperData) {
    try {
      // 1. 获取公钥
      const publicKey = await this.getPublicKey()

      // 2. 生成AES会话密钥（用于加密大字段）
      const aesKey = AES.generateKey()

      // 3. 生成时间戳和nonce（防重放）
      const timestamp = Date.now()
      const nonce = Random.uuid()

      // 4. 加密小字段（RSA）
      const encryptedCourseName = RSA.encrypt(paperData.courseName || '', publicKey)
      const encryptedExamType = RSA.encrypt(paperData.examType || '', publicKey)
      const encryptedSemester = RSA.encrypt(paperData.semester || '', publicKey)
      const encryptedDepartment = RSA.encrypt(paperData.department || '', publicKey)

      // 5. 加密大字段（AES）
      const encryptedContent = AES.encrypt(paperData.content || '', aesKey)

      // 6. 加密文件路径（如果有）
      const encryptedFilePath = paperData.filePath
        ? RSA.encrypt(paperData.filePath, publicKey)
        : ''

      // 7. 使用RSA加密AES密钥
      const encryptedAesKey = RSA.encrypt(aesKey, publicKey)

      // 8. 构建待签名数据（用于HMAC）
      const signData = JSON.stringify({
        courseName: encryptedCourseName,
        examType: encryptedExamType,
        semester: encryptedSemester,
        department: encryptedDepartment,
        content: encryptedContent,
        filePath: encryptedFilePath,
        encryptedAesKey,
        timestamp,
        nonce
      })

      // 9. 生成HMAC签名（使用AES密钥）
      const hmac = SM3.hmac(signData, aesKey)

      // 10. 返回加密后的数据
      return {
        courseName: encryptedCourseName,
        examType: encryptedExamType,
        semester: encryptedSemester,
        department: encryptedDepartment,
        content: encryptedContent,
        filePath: encryptedFilePath,
        encryptedAesKey,
        timestamp,
        nonce,
        hmac
      }
    } catch (error) {
      console.error('试卷数据加密失败:', error)
      throw new Error('试卷数据加密失败: ' + error.message)
    }
  },

  /**
   * 清除缓存的公钥
   */
  clearCache() {
    this._cachedPublicKey = null
  }
}

/**
 * 默认导出
 */
export default {
  AES,
  RSA,
  SM3,
  Base64,
  PasswordStrength,
  Random,
  LoginCrypto,
  PaperCrypto
}
