<template>
  <div class="paper-create-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑试卷' : '创建试卷' }}</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        class="paper-form"
      >
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="form.courseName" placeholder="请输入课程名称" clearable />
        </el-form-item>

        <el-form-item label="考试类型" prop="examType">
          <el-select v-model="form.examType" placeholder="请选择考试类型" style="width: 100%">
            <el-option label="期末考试" value="期末考试" />
            <el-option label="期中考试" value="期中考试" />
            <el-option label="平时测验" value="平时测验" />
            <el-option label="补考" value="补考" />
            <el-option label="重修" value="重修" />
          </el-select>
        </el-form-item>

        <el-form-item label="学期" prop="semester">
          <el-input v-model="form.semester" placeholder="如：2024-2025-1" clearable />
        </el-form-item>

        <el-form-item label="院系" prop="department">
          <el-input v-model="form.department" placeholder="请输入院系" clearable />
        </el-form-item>

        <el-form-item label="试卷内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="请输入试卷内容（支持Markdown格式）"
          />
        </el-form-item>

        <el-form-item label="附件上传">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
            accept=".pdf,.doc,.docx"
          >
            <el-button type="primary" :icon="Upload">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                只能上传 PDF/Word 文件，且不超过 10MB
              </div>
            </template>
          </el-upload>
          <div v-if="uploadProgress > 0 && uploadProgress < 100" class="upload-progress">
            <el-progress :percentage="uploadProgress" />
          </div>
        </el-form-item>

        <el-form-item label="数字签名" prop="privateKey">
          <!-- 输入方式切换 -->
          <el-radio-group v-model="keyInputMode" class="key-mode-switch">
            <el-radio-button value="file">文件上传</el-radio-button>
            <el-radio-button value="input">手动输入</el-radio-button>
          </el-radio-group>

          <!-- 文件上传方式 -->
          <div v-if="keyInputMode === 'file'" class="private-key-upload">
            <el-upload
              :auto-upload="false"
              :show-file-list="false"
              accept=".pem"
              :on-change="handleKeyFileChange"
            >
              <template #trigger>
                <el-button type="warning" plain :icon="Key">
                  {{ form.privateKey ? '已选择私钥文件 ✓' : '选择PEM私钥文件' }}
                </el-button>
              </template>
            </el-upload>
            <div class="key-tip">
              请选择您注册时下载的私钥文件（.pem）进行身份验证
            </div>
          </div>

          <!-- 手动输入方式 -->
          <div v-else class="private-key-input">
            <el-input
              v-model="privateKeyText"
              type="textarea"
              :rows="4"
              placeholder="请粘贴私钥内容（支持PEM格式或纯Base64编码）"
              @input="handleKeyTextInput"
            />
            <div class="key-tip">
              直接粘贴私钥内容，系统会自动处理格式（去除BEGIN/END标记和空白字符）
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit('draft')">
            保存草稿
          </el-button>
          <el-button type="success" :loading="loading" @click="handleSubmit('submit')">
            保存并提交审批
          </el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload, Key } from '@element-plus/icons-vue'
import { createPaper, updatePaper, getPaperDetail, submitPaper } from '@/api/paper'
import { uploadFile } from '@/api/file'

const router = useRouter()
const route = useRoute()

// 是否编辑模式
const isEdit = ref(false)
const paperId = ref(null)

// 表单引用
const formRef = ref(null)
const uploadRef = ref(null)

// 表单数据
const form = reactive({
  courseName: '',
  examType: '',
  semester: '',
  department: '',
  content: '',
  filePath: '',
  privateKey: ''  // 用户RSA私钥（用于数字签名）
})

// 验证规则
const rules = {
  courseName: [
    { required: true, message: '请输入课程名称', trigger: 'blur' }
  ],
  examType: [
    { required: true, message: '请选择考试类型', trigger: 'change' }
  ],
  semester: [
    { required: true, message: '请输入学期', trigger: 'blur' }
  ],
  department: [
    { required: true, message: '请输入院系', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入试卷内容', trigger: 'blur' }
  ],
  privateKey: [
    { required: true, message: '请上传私钥文件或手动输入私钥内容', trigger: 'change' }
  ]
}

// 加载状态
const loading = ref(false)
const uploadProgress = ref(0)

// 选择的文件
const selectedFile = ref(null)

// 私钥输入方式：file / input
const keyInputMode = ref('file')

// 手动输入的私钥文本
const privateKeyText = ref('')

/**
 * 文件选择变化
 */
const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

/**
 * 文件数量超限
 */
const handleExceed = () => {
  ElMessage.warning('只能上传一个文件')
}

/**
 * 私钥文件选择处理
 */
const handleKeyFileChange = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    const pemContent = e.target.result
    form.privateKey = extractPrivateKey(pemContent)
    if (form.privateKey) {
      ElMessage.success('私钥文件已加载')
      // 手动触发表单验证（静默处理验证失败）
      formRef.value?.validateField('privateKey').catch(() => {})
    } else {
      ElMessage.error('无效的私钥文件格式')
    }
  }
  reader.onerror = () => {
    ElMessage.error('读取私钥文件失败')
  }
  reader.readAsText(file.raw)
}

/**
 * 从PEM格式或纯文本中提取Base64编码的私钥
 */
const extractPrivateKey = (content) => {
  if (!content || !content.trim()) return ''

  // 移除PEM头尾标记
  let cleaned = content
    .replace(/-----BEGIN.*?-----/g, '')
    .replace(/-----END.*?-----/g, '')

  // 移除所有空白字符（换行、空格等）
  cleaned = cleaned.replace(/\s/g, '')

  // 验证是否为有效的Base64字符串（RSA-2048私钥约1600+字符）
  if (/^[A-Za-z0-9+/=]+$/.test(cleaned) && cleaned.length > 100) {
    return cleaned
  }

  return ''
}

/**
 * 手动输入私钥处理
 */
const handleKeyTextInput = () => {
  const cleaned = extractPrivateKey(privateKeyText.value)
  form.privateKey = cleaned
  // 手动触发表单验证（静默处理验证失败）
  formRef.value?.validateField('privateKey').catch(() => {})
}

// 切换输入方式时清空已输入的内容
watch(keyInputMode, () => {
  form.privateKey = ''
  privateKeyText.value = ''
  // 清除验证状态
  formRef.value?.clearValidate('privateKey')
})

/**
 * 上传文件
 */
const uploadPaperFile = async () => {
  if (!selectedFile.value) {
    return form.filePath
  }

  try {
    uploadProgress.value = 0
    const response = await uploadFile(selectedFile.value, (percent) => {
      uploadProgress.value = percent
    })

    if (response.code === 200 && response.data) {
      ElMessage.success('文件上传成功')
      return response.data
    }
  } catch (error) {
    console.error('文件上传失败:', error)
    ElMessage.error('文件上传失败')
  } finally {
    uploadProgress.value = 0
  }

  return form.filePath
}

/**
 * 提交表单
 */
const handleSubmit = async (action) => {
  if (!formRef.value) return

  // 验证表单
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    loading.value = true

    // 上传文件
    const filePath = await uploadPaperFile()
    form.filePath = filePath

    // 保存试卷
    let response
    if (isEdit.value) {
      response = await updatePaper(paperId.value, form)
    } else {
      response = await createPaper(form)
    }

    if (response.code === 200) {
      ElMessage.success(isEdit.value ? '保存成功' : '创建成功')

      // 如果是提交审批
      if (action === 'submit') {
        const id = isEdit.value ? paperId.value : response.data
        const submitResponse = await submitPaper(id)
        if (submitResponse.code === 200) {
          ElMessage.success('已提交审批')
        }
      }

      // 返回列表页
      router.push('/papers')
    }
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 加载试卷详情（编辑模式）
 */
const loadPaperDetail = async (id) => {
  try {
    const response = await getPaperDetail(id)
    if (response.code === 200 && response.data) {
      Object.assign(form, response.data)
    }
  } catch (error) {
    console.error('加载试卷详情失败:', error)
    router.back()
  }
}

// 组件挂载时检查是否编辑模式
onMounted(() => {
  const id = route.query.id
  if (id) {
    isEdit.value = true
    paperId.value = Number(id)
    loadPaperDetail(paperId.value)
  }
})
</script>

<style scoped>
/* ========== Apple 高雅白 UI 设计系统 ========== */

/* ========== 容器 + CSS变量定义 ========== */
.paper-create-container {
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
  --apple-blue-light: rgba(0, 113, 227, 0.1);
  --apple-green: #30d158;
  --apple-green-hover: #28c04d;
  --apple-green-light: rgba(48, 209, 88, 0.1);

  /* 间距系统 (8px基准) */
  --spacing-xs: 8px;
  --spacing-sm: 16px;
  --spacing-md: 24px;
  --spacing-lg: 32px;
  --spacing-xl: 48px;

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
  padding: var(--spacing-lg);
  min-height: 100vh;
  background: linear-gradient(135deg, #fafafa 0%, #f0f0f2 100%);
  font-family: var(--font-apple);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* ========== 主卡片 ========== */
.paper-create-container :deep(.el-card) {
  border-radius: var(--radius-xl) !important;
  border: 0.5px solid var(--apple-border) !important;
  box-shadow: var(--shadow-card) !important;
  transition: all var(--duration-base) var(--ease-out);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
}

.paper-create-container :deep(.el-card:hover) {
  box-shadow: var(--shadow-card-hover) !important;
}

.paper-create-container :deep(.el-card__header) {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--apple-gray-2);
  background: transparent;
}

.paper-create-container :deep(.el-card__body) {
  padding: var(--spacing-xl);
}

/* ========== 卡片头部 ========== */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  font-size: 20px;
  font-weight: var(--font-weight-semibold);
  color: var(--apple-text-primary);
  letter-spacing: -0.3px;
}

.card-header :deep(.el-button) {
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
  padding: 10px 20px;
  transition: all var(--duration-base) var(--ease-out);
  background: var(--apple-gray-1);
  border: 1px solid var(--apple-gray-2);
  color: var(--apple-text-primary);
}

.card-header :deep(.el-button:hover) {
  background: var(--apple-gray-2);
  border-color: var(--apple-gray-3);
  transform: translateY(-2px);
}

/* ========== 表单样式 ========== */
.paper-form {
  max-width: 800px;
  margin: 0 auto;
}

.paper-form :deep(.el-form-item__label) {
  font-weight: var(--font-weight-semibold);
  color: var(--apple-text-primary);
  font-size: 15px;
  letter-spacing: -0.2px;
}

.paper-form :deep(.el-form-item) {
  margin-bottom: var(--spacing-lg);
}

/* ========== 输入框样式 ========== */
.paper-form :deep(.el-input__wrapper) {
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background-color: var(--apple-gray-1);
  border: 1px solid transparent;
  box-shadow: none;
  transition: all var(--duration-base) var(--ease-out);
}

.paper-form :deep(.el-input__wrapper:hover) {
  background-color: var(--apple-gray-2);
  border-color: var(--apple-gray-3);
}

.paper-form :deep(.el-input__wrapper.is-focus) {
  background-color: #ffffff;
  border-color: var(--apple-blue);
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.1);
}

.paper-form :deep(.el-input__inner) {
  font-size: 15px;
  color: var(--apple-text-primary);
  font-weight: var(--font-weight-regular);
}

.paper-form :deep(.el-input__inner::placeholder) {
  color: var(--apple-text-tertiary);
}

/* ========== 选择框样式 ========== */
.paper-form :deep(.el-select) {
  width: 100%;
}

/* ========== 文本域样式 ========== */
.paper-form :deep(.el-textarea__inner) {
  padding: 16px;
  border-radius: var(--radius-md);
  background-color: var(--apple-gray-1);
  border: 1px solid transparent;
  box-shadow: none;
  transition: all var(--duration-base) var(--ease-out);
  font-size: 15px;
  color: var(--apple-text-primary);
  font-weight: var(--font-weight-regular);
  font-family: var(--font-apple);
  line-height: 1.6;
  resize: vertical;
}

.paper-form :deep(.el-textarea__inner:hover) {
  background-color: var(--apple-gray-2);
  border-color: var(--apple-gray-3);
}

.paper-form :deep(.el-textarea__inner:focus) {
  background-color: #ffffff;
  border-color: var(--apple-blue);
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.1);
}

.paper-form :deep(.el-textarea__inner::placeholder) {
  color: var(--apple-text-tertiary);
}

/* ========== 文件上传样式 ========== */
.paper-form :deep(.el-upload) {
  width: 100%;
}

.paper-form :deep(.el-upload .el-button) {
  background: var(--apple-blue) !important;
  border: none !important;
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
  padding: 12px 24px;
  transition: all var(--duration-base) var(--ease-out);
  box-shadow: 0 2px 8px rgba(0, 113, 227, 0.15);
}

.paper-form :deep(.el-upload .el-button:hover) {
  background: var(--apple-blue-hover) !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 113, 227, 0.25);
}

.paper-form :deep(.el-upload__tip) {
  color: var(--apple-text-secondary);
  font-size: 13px;
  margin-top: var(--spacing-sm);
  font-weight: var(--font-weight-regular);
}

/* ========== 输入方式切换 ========== */
.key-mode-switch {
  margin-bottom: var(--spacing-md);
  width: 100%;
}

.key-mode-switch :deep(.el-radio-button__inner) {
  border-radius: 0;
  padding: 10px 24px;
  font-weight: var(--font-weight-medium);
  transition: all var(--duration-base) var(--ease-out);
}

.key-mode-switch :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: var(--radius-md) 0 0 var(--radius-md);
}

.key-mode-switch :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
}

.key-mode-switch :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--apple-blue);
  border-color: var(--apple-blue);
  box-shadow: none;
}

/* ========== 私钥上传样式 ========== */
.private-key-upload {
  width: 100%;
}

/* ========== 私钥手动输入样式 ========== */
.private-key-input {
  width: 100%;
}

.private-key-input :deep(.el-textarea__inner) {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.5;
  padding: 16px;
  border-radius: var(--radius-md);
  background-color: var(--apple-gray-1);
  border: 1px solid transparent;
  transition: all var(--duration-base) var(--ease-out);
}

.private-key-input :deep(.el-textarea__inner:hover) {
  background-color: var(--apple-gray-2);
  border-color: var(--apple-gray-3);
}

.private-key-input :deep(.el-textarea__inner:focus) {
  background-color: #ffffff;
  border-color: var(--apple-blue);
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.1);
}

.private-key-input :deep(.el-textarea__inner::placeholder) {
  color: var(--apple-text-tertiary);
  font-family: var(--font-apple);
}

.private-key-upload :deep(.el-button--warning.is-plain) {
  background: rgba(255, 149, 0, 0.1);
  border: 1px solid rgba(255, 149, 0, 0.3);
  color: #ff9500;
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
  padding: 12px 24px;
  transition: all var(--duration-base) var(--ease-out);
}

.private-key-upload :deep(.el-button--warning.is-plain:hover) {
  background: rgba(255, 149, 0, 0.15);
  border-color: rgba(255, 149, 0, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 149, 0, 0.15);
}

.key-tip {
  color: var(--apple-text-tertiary);
  font-size: 13px;
  margin-top: var(--spacing-sm);
  font-weight: var(--font-weight-regular);
  line-height: 1.5;
  padding: 8px 12px;
  background: rgba(255, 149, 0, 0.05);
  border-radius: var(--radius-sm);
  border-left: 3px solid rgba(255, 149, 0, 0.5);
}

/* ========== 上传进度条 ========== */
.upload-progress {
  margin-top: var(--spacing-md);
  padding: var(--spacing-md);
  background: var(--apple-gray-1);
  border-radius: var(--radius-md);
}

.upload-progress :deep(.el-progress__text) {
  font-weight: var(--font-weight-semibold);
  color: var(--apple-blue);
}

.upload-progress :deep(.el-progress-bar__outer) {
  background-color: var(--apple-gray-2);
  border-radius: 8px;
  height: 8px !important;
}

.upload-progress :deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, var(--apple-blue) 0%, var(--apple-blue-hover) 100%);
  border-radius: 8px;
}

/* ========== 表单提交按钮 ========== */
.paper-form :deep(.el-form-item:last-child) {
  margin-top: var(--spacing-xl);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--apple-gray-2);
}

.paper-form :deep(.el-form-item:last-child .el-button) {
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
  padding: 12px 28px;
  transition: all var(--duration-base) var(--ease-out);
  font-size: 15px;
  letter-spacing: 0.3px;
  min-width: 140px;
}

.paper-form :deep(.el-button--primary) {
  background: var(--apple-blue) !important;
  border: none !important;
  box-shadow: 0 2px 8px rgba(0, 113, 227, 0.15);
}

.paper-form :deep(.el-button--primary:hover) {
  background: var(--apple-blue-hover) !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 113, 227, 0.25);
}

.paper-form :deep(.el-button--primary:active) {
  transform: translateY(0);
}

.paper-form :deep(.el-button--success) {
  background: var(--apple-green) !important;
  border: none !important;
  box-shadow: 0 2px 8px rgba(48, 209, 88, 0.15);
}

.paper-form :deep(.el-button--success:hover) {
  background: var(--apple-green-hover) !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(48, 209, 88, 0.25);
}

.paper-form :deep(.el-button--success:active) {
  transform: translateY(0);
}

.paper-form :deep(.el-button:not(.el-button--primary):not(.el-button--success)) {
  background: var(--apple-gray-1);
  border: 1px solid var(--apple-gray-2);
  color: var(--apple-text-primary);
}

.paper-form :deep(.el-button:not(.el-button--primary):not(.el-button--success):hover) {
  background: var(--apple-gray-2);
  border-color: var(--apple-gray-3);
  transform: translateY(-2px);
}

/* ========== 表单验证错误提示 ========== */
.paper-form :deep(.el-form-item__error) {
  color: #ff3b30;
  font-size: 13px;
  font-weight: var(--font-weight-medium);
  padding-top: var(--spacing-xs);
}

/* ========== 加载状态 ========== */
.paper-form :deep(.el-button.is-loading) {
  opacity: 0.7;
  cursor: not-allowed;
}

/* ========== 下拉菜单样式 ========== */
.paper-form :deep(.el-select-dropdown) {
  border-radius: var(--radius-md) !important;
  border: 0.5px solid var(--apple-border) !important;
  box-shadow: var(--shadow-card) !important;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
}

.paper-form :deep(.el-select-dropdown__item) {
  font-weight: var(--font-weight-regular);
  color: var(--apple-text-primary);
  padding: 12px 16px;
  transition: all var(--duration-base) var(--ease-out);
}

.paper-form :deep(.el-select-dropdown__item:hover) {
  background-color: var(--apple-gray-1);
}

.paper-form :deep(.el-select-dropdown__item.selected) {
  color: var(--apple-blue);
  font-weight: var(--font-weight-semibold);
  background-color: var(--apple-blue-light);
}

/* ========== 响应式设计 ========== */
@media (max-width: 768px) {
  .paper-create-container {
    padding: var(--spacing-md);
  }

  .paper-form {
    max-width: 100%;
  }

  .card-header {
    flex-direction: column;
    gap: var(--spacing-sm);
    align-items: flex-start;
  }

  .paper-form :deep(.el-form-item__label) {
    width: 100% !important;
    text-align: left;
    margin-bottom: var(--spacing-xs);
  }

  .paper-form :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }

  .paper-form :deep(.el-form-item:last-child .el-button) {
    width: 100%;
    margin-bottom: var(--spacing-sm);
  }
}

@media (max-width: 480px) {
  .paper-create-container {
    padding: var(--spacing-sm);
  }

  .card-header span {
    font-size: 18px;
  }

  .paper-form :deep(.el-form-item:last-child .el-button) {
    min-width: auto;
    padding: 12px 20px;
  }
}
</style>
