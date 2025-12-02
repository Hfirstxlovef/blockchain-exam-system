# 试卷审批系统 - 前端

## 项目简介
基于Vue3 + Element Plus的试卷审批管理系统前端，实现安全加密通信。

## 技术栈
- Vue 3.4（Composition API）
- Vite 5.0（构建工具）
- Vue Router 4.2（路由管理）
- Pinia 2.1（状态管理）
- Element Plus 2.5（UI组件库）
- Axios 1.6（HTTP请求）
- CryptoJS 4.2（AES加密）
- JSEncrypt 3.3（RSA加密）

## 核心功能
1. **用户登录**：JWT认证、角色识别
2. **教师端**：
   - 试卷列表、创建、编辑、删除
   - 文件上传（支持加密）
   - 提交审批
   - 查看审批进度
3. **管理员端**：
   - 待审批列表
   - 审批详情查看
   - 审批操作（通过/驳回）
   - 数字签名确认
4. **系统管理**（院管理员）：
   - 用户管理
   - 审计日志查看
5. **安全特性**：
   - API请求/响应自动加解密
   - HMAC签名验证
   - XSS防护
   - 路由权限控制

## 项目结构
```
src/
├── api/                # API接口定义
├── assets/             # 静态资源
│   ├── images/         # 图片
│   └── styles/         # 样式文件
├── components/         # 组件
│   ├── common/         # 公共组件
│   └── layout/         # 布局组件
├── router/             # 路由配置
├── store/              # 状态管理
├── utils/              # 工具类
│   ├── crypto.js       # 加密工具
│   ├── request.js      # Axios封装
│   └── validate.js     # 表单验证
├── views/              # 页面组件
│   ├── login/          # 登录页
│   ├── teacher/        # 教师端页面
│   ├── admin/          # 管理员端页面
│   └── system/         # 系统管理页面
├── App.vue             # 根组件
└── main.js             # 入口文件
```

## 快速开始

### 1. 安装依赖
```bash
npm install
# 或
yarn install
```

### 2. 开发模式
```bash
npm run dev
```
访问：http://localhost:3000

### 3. 生产构建
```bash
npm run build
```

### 4. 预览构建结果
```bash
npm run preview
```

## 环境配置

### 开发环境
Vite会自动代理 `/api` 请求到后端 `http://localhost:8080`

### 生产环境
需要在Nginx中配置反向代理：
```nginx
location /api {
    proxy_pass http://backend-server:8080/api;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

## 安全特性

### API加密通信
1. 前端生成AES会话密钥
2. 使用后端RSA公钥加密会话密钥
3. 后端解密得到会话密钥
4. 双方使用会话密钥进行AES加密通信

### 请求格式
```json
{
  "encryptedData": "AES加密后的JSON数据",
  "timestamp": 1699999999999,
  "nonce": "随机字符串",
  "sign": "HMAC-SHA256签名"
}
```

### 防护措施
- XSS防护：Vue自动转义、DOMPurify净化
- CSRF防护：自定义Token
- 路由守卫：权限验证
- 敏感信息清理：退出登录清除缓存

## 测试账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| teacher1 | 123456 | teacher | 教师 |
| dept_admin1 | 123456 | dept_admin | 系管理员 |
| college_admin1 | 123456 | college_admin | 院管理员 |

## 开发者
网络信息安全大作业

## 许可证
MIT License
