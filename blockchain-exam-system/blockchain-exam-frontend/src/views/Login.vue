<template>
  <div class="login-container">
    <div class="login-card">
      <!-- 标题 -->
      <div class="login-header">
        <div class="login-icon">
          <el-icon :size="48"><Document /></el-icon>
        </div>
        <h1 class="login-title">试卷审批系统</h1>
        <p class="login-subtitle">Exam Approval Management System</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 底部信息 -->
      <div class="login-footer">
        <p>网络信息安全大作业</p>
        <p>
          <el-icon :size="14"><Lock /></el-icon>
          数据传输加密 | 数字签名验证 | 审批流程管理
        </p>
      </div>
    </div>

    <!-- 背景装饰 -->
    <div class="background-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'
import { LoginCrypto } from '@/utils/crypto'

const router = useRouter()
const userStore = useUserStore()

// 表单引用
const loginFormRef = ref(null)

// 加载状态
const loading = ref(false)

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' }
  ]
}

/**
 * 处理登录
 */
const handleLogin = async () => {
  if (!loginFormRef.value) return

  // 验证表单
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    loading.value = true

    // 加密密码
    const { encryptedPassword, timestamp, nonce } = await LoginCrypto.encryptPassword(
      loginForm.password
    )

    console.log('登录数据:', {
      username: loginForm.username,
      encryptedPassword: encryptedPassword.substring(0, 50) + '...',
      timestamp,
      nonce
    })

    // 调用登录API（使用加密后的密码）
    const response = await login(loginForm.username, encryptedPassword, timestamp, nonce)

    if (response.code === 200 && response.data) {
      // 保存登录信息到store
      userStore.setToken(response.data.token)
      userStore.setUserInfo(response.data.userInfo)

      ElMessage.success('登录成功')

      // 跳转到首页
      router.push('/')
    } else {
      ElMessage.error(response.message || '登录失败')
    }
  } catch (error) {
    console.error('登录失败:', error)
    // 错误消息已在request拦截器中处理
    ElMessage.error(error.message || '登录失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ========== Apple 高雅白 UI 设计系统 ========== */

/* ========== 登录容器 + CSS变量定义 ========== */
.login-container {
  /* 背景色系统 */
  --apple-white: #fbfbfd;
  --apple-gray-1: #f5f5f7;
  --apple-gray-2: #e8e8ed;
  --apple-gray-3: #d2d2d7;
  --apple-border: rgba(0, 0, 0, 0.08);

  /* 文字色系统 */
  --apple-text-primary: #1d1d1f;
  --apple-text-secondary: #6e6e73;
  --apple-text-tertiary: #86868b;

  /* 品牌色 */
  --apple-blue: #0071e3;
  --apple-blue-hover: #0077ed;

  /* 间距系统 (8px基准) */
  --spacing-xs: 8px;
  --spacing-sm: 16px;
  --spacing-md: 24px;
  --spacing-lg: 32px;
  --spacing-xl: 48px;
  --spacing-2xl: 64px;

  /* 阴影系统 */
  --shadow-card:
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 8px 32px rgba(0, 0, 0, 0.03);
  --shadow-card-hover:
    0 8px 24px rgba(0, 0, 0, 0.08),
    0 16px 48px rgba(0, 0, 0, 0.06);

  /* 圆角系统 */
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;

  /* 字体系统 */
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text",
                "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  --font-weight-regular: 400;
  --font-weight-medium: 500;
  --font-weight-semibold: 600;

  /* 动画系统 */
  --ease-out: cubic-bezier(0.25, 0.46, 0.45, 0.94);
  --duration-base: 0.3s;

  /* 布局样式 */
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  /* Apple 风格渐变背景 */
  background: linear-gradient(135deg, #fafafa 0%, #f0f0f2 100%);
  font-family: var(--font-apple);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow: hidden;
}

/* ========== 登录卡片 ========== */
.login-card {
  position: relative;
  z-index: 1;
  width: 460px;
  padding: var(--spacing-2xl) var(--spacing-xl);
  background: #ffffff;
  border-radius: var(--radius-xl);
  /* Apple 风格极细边框 */
  border: 0.5px solid var(--apple-border);
  /* 多层柔和阴影 */
  box-shadow: var(--shadow-card);
  transition: all var(--duration-base) var(--ease-out);
}

.login-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card-hover);
}

/* ========== 登录头部 ========== */
.login-header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
}

.login-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  /* Apple 蓝渐变 */
  background: linear-gradient(135deg, var(--apple-blue) 0%, #005bb5 100%);
  border-radius: var(--radius-lg);
  color: white;
  margin-bottom: var(--spacing-md);
  box-shadow: 0 4px 16px rgba(0, 113, 227, 0.2);
}

.login-title {
  font-size: 32px;
  font-weight: var(--font-weight-semibold);
  letter-spacing: -0.5px;
  color: var(--apple-text-primary);
  margin: 0 0 var(--spacing-xs) 0;
  line-height: 1.2;
}

.login-subtitle {
  font-size: 15px;
  font-weight: var(--font-weight-regular);
  color: var(--apple-text-secondary);
  margin: 0;
  line-height: 1.4;
}

/* ========== 登录表单 ========== */
.login-form {
  margin-bottom: var(--spacing-lg);
}

/* Element Plus 输入框定制 */
.login-form :deep(.el-form-item) {
  margin-bottom: var(--spacing-md);
}

/* 隐藏表单验证错误提示文字 */
.login-form :deep(.el-form-item__error) {
  display: none;
}

.login-form :deep(.el-input__wrapper) {
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background-color: var(--apple-gray-1);
  border: 1px solid transparent;
  box-shadow: none;
  transition: all var(--duration-base) var(--ease-out);
}

.login-form :deep(.el-input__wrapper:hover) {
  background-color: var(--apple-gray-2);
  border-color: var(--apple-gray-3);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  background-color: #ffffff;
  border-color: var(--apple-blue);
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.1);
}

/* 错误状态样式优化（柔和处理） */
.login-form :deep(.el-input__wrapper.is-error) {
  background-color: var(--apple-gray-1);
  border-color: transparent;
  box-shadow: none;
}

.login-form :deep(.el-input__inner) {
  font-size: 15px;
  color: var(--apple-text-primary);
  font-weight: var(--font-weight-regular);
}

.login-form :deep(.el-input__inner::placeholder) {
  color: var(--apple-text-tertiary);
}

/* ========== 登录按钮 ========== */
.login-button {
  display: block !important;
  width: 100% !important;
  height: 52px;
  font-size: 16px;
  font-weight: var(--font-weight-medium);
  letter-spacing: 0.5px;
  background: var(--apple-blue) !important;
  border: none !important;
  border-radius: var(--radius-md);
  color: #ffffff !important;
  transition: all var(--duration-base) var(--ease-out);
  box-shadow: 0 2px 8px rgba(0, 113, 227, 0.15);
}

.login-button:hover {
  background: var(--apple-blue-hover);
  transform: scale(1.02);
  box-shadow: 0 4px 16px rgba(0, 113, 227, 0.25);
}

.login-button:active {
  transform: scale(0.98);
}

/* ========== 登录底部 ========== */
.login-footer {
  text-align: center;
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--apple-gray-2);
}

.login-footer p {
  margin: var(--spacing-xs) 0;
  font-size: 13px;
  color: var(--apple-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  line-height: 1.5;
}

.login-footer p:first-child {
  font-weight: var(--font-weight-medium);
  color: var(--apple-text-primary);
}

/* ========== 背景装饰（微妙渐变球） ========== */
.background-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  /* 极淡的渐变装饰 */
  background: radial-gradient(circle, rgba(0, 113, 227, 0.03) 0%, transparent 70%);
  animation: float 25s infinite ease-in-out;
}

.circle-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.circle-2 {
  width: 350px;
  height: 350px;
  bottom: -80px;
  right: -80px;
  animation-delay: 8s;
}

.circle-3 {
  width: 300px;
  height: 300px;
  top: 50%;
  right: -50px;
  animation-delay: 16s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
    opacity: 0.6;
  }
  50% {
    transform: translate(-20px, -20px) scale(1.1);
    opacity: 0.8;
  }
}

/* ========== 响应式设计 ========== */
@media (max-width: 768px) {
  .login-card {
    width: 90%;
    max-width: 400px;
    padding: var(--spacing-xl) var(--spacing-md);
  }

  .login-title {
    font-size: 28px;
  }

  .login-icon {
    width: 64px;
    height: 64px;
  }
}

@media (max-width: 480px) {
  .login-card {
    width: 95%;
    padding: var(--spacing-lg) var(--spacing-md);
  }

  .login-title {
    font-size: 24px;
  }

  .login-button {
    height: 48px;
    font-size: 15px;
  }
}
</style>
