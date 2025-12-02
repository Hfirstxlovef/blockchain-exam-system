# 试卷审批系统 - 后端

## 项目简介
基于Spring Boot的试卷审批管理系统后端，融合密码学与网络安全技术。

## 技术栈
- Spring Boot 2.7.18
- Spring Security + JWT
- MyBatis-Plus 3.5.3
- MySQL 8.0
- Redis
- Bouncy Castle（密码学库）
- Hutool工具类
- Knife4j（API文档）

## 核心功能
1. **用户认证授权**：JWT令牌、RBAC权限控制
2. **试卷管理**：试卷CRUD、文件加密存储
3. **审批工作流**：三级审批（教师→系→院）、数字签名
4. **加密功能**：
   - AES-256-GCM数据库字段加密
   - RSA-2048数字签名
   - API传输加密
   - 文件加密存储
5. **安全防护**：防SQL注入、XSS、CSRF、防重放攻击、接口限流

## 项目结构
```
src/main/java/com/exam/approval/
├── controller/          # 控制器层
├── service/            # 业务逻辑层
├── mapper/             # 数据访问层
├── entity/             # 实体类
├── security/           # 安全模块
│   ├── config/         # Spring Security配置
│   ├── filter/         # 认证过滤器
│   └── util/           # 安全工具类
├── interceptor/        # 拦截器（加密、签名验证）
├── util/               # 工具类（AES、RSA、SM3等）
└── common/             # 公共模块
    ├── annotation/     # 自定义注解
    ├── enums/          # 枚举类
    ├── exception/      # 异常处理
    └── result/         # 统一返回结果
```

## 快速开始

### 1. 环境要求
- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Redis 5.0+

### 2. 数据库初始化
```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE exam_approval DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入SQL脚本
source docs/database.sql
```

### 3. 配置修改
修改 `application.yml` 中的数据库连接信息和Redis配置。

### 4. 运行项目
```bash
# Maven方式
mvn clean install
mvn spring-boot:run

# 或直接运行主类
java -jar target/approval-system-1.0.0.jar
```

### 5. 访问API文档
启动成功后访问：http://localhost:8080/api/doc.html

## 安全特性

### 数据库加密
- 试卷内容、审批意见等敏感字段使用AES-256-GCM加密
- 用户密码使用BCrypt哈希
- MyBatis拦截器实现透明加解密

### API传输加密
- 前后端协商AES会话密钥（用RSA加密传输）
- 所有请求/响应体使用AES加密
- HMAC-SHA256签名防篡改
- 时间戳+随机数防重放

### 数字签名
- 每次审批操作生成RSA数字签名
- 审批记录不可抵赖、可验证

### 安全防护
- Spring Security权限控制
- SQL注入防护（参数化查询）
- XSS防护（输入验证、输出编码）
- CSRF Token验证
- 接口限流（Guava RateLimiter）
- 操作审计日志

---

## 📚 课程亮点 - 密码学技术应用

> 📊 **详细流程图**: [密码学技术流程图](docs/密码学技术流程图.md) - 5个核心密码学技术的可视化流程

### 1. 对称加密 - AES-256-GCM

**理论应用**: 数据库敏感字段加密、API传输加密

**核心实现**:
- **[`AESUtil.java`](src/main/java/com/exam/approval/security/util/AESUtil.java#L31-L92)** - AES-256-GCM加密/解密
  - 自动生成随机IV (12字节)
  - GCM模式认证标签 (128位)
  - Base64编码存储

- **[`MybatisCryptoInterceptor.java`](src/main/java/com/exam/approval/interceptor/MybatisCryptoInterceptor.java#L113-L176)** - 透明字段加密
  - 拦截SQL参数设置,自动加密 `@Encrypted` 字段
  - 拦截查询结果,自动解密返回明文
  - 支持ParamMap递归处理

- **[`PaperCryptoService.java`](src/main/java/com/exam/approval/service/PaperCryptoService.java#L76-L77)** - API传输加密
  - 前端AES-CBC加密数据传输
  - 后端解密处理

**关键代码片段**:
```java
// AES-GCM加密实现 (AESUtil.java:71-78)
byte[] iv = new byte[12];
new SecureRandom().nextBytes(iv);
GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
```

---

### 2. 非对称加密 - RSA-2048

**理论应用**: 数字签名、密钥交换、小数据加密

**核心实现**:
- **[`RSAUtil.java`](src/main/java/com/exam/approval/security/util/RSAUtil.java)** - RSA加密/签名工具
  - 生成2048位密钥对
  - PKCS1填充模式加密
  - SHA256withRSA数字签名

- **[`PaperCryptoService.java`](src/main/java/com/exam/approval/service/PaperCryptoService.java#L53-L54)** - RSA解密AES会话密钥
  - 前端用系统公钥加密AES密钥
  - 后端用私钥解密获取会话密钥

- **数字签名应用** - 审批记录不可抵赖

**密钥配置**: **[`application.yml`](src/main/resources/application.yml#L88-L90)**

---

### 3. 哈希函数与消息认证码

**理论应用**: 密码存储、数据完整性校验

**核心实现**:

**BCrypt - 密码哈希**
- Spring Security默认密码编码器
- 自动加盐,防彩虹表攻击
- 10轮迭代增强安全性

**HMAC-SHA256 - 数据完整性**
```java
// 请求签名验证 (PaperCryptoService.java:58-64)
String expectedHmac = HmacUtils.hmacSha256Hex(aesKey, request.toSignData());
if (!expectedHmac.equals(request.getHmac())) {
    throw new RuntimeException("数据完整性校验失败");
}
```

**SM3 (国密算法)** - **[`SM3Util.java`](src/main/java/com/exam/approval/security/util/SM3Util.java)**
- 中国商用密码标准
- 可用于文件完整性校验

---

### 4. 数字签名与不可抵赖性

**理论应用**: 审批操作不可抵赖,防篡改

**实现机制**:
- **签名生成**: 审批时使用审批人私钥对 `paperId|userId|action|timestamp|comment` 签名
- **签名存储**: 存储到 `approval_record.signature` 字段
- **签名验证**: 读取审批记录时验证签名真实性

**数据库设计**:
- `approval_record.signature` - Base64编码的RSA签名
- `encryption_key` 表 - 用户RSA密钥对管理

---

### 5. 密钥管理

**理论应用**: 密钥生成、存储、分发

**密钥层次**:

**主密钥 (Master Key)**
- **配置**: **[`application.yml:82`](src/main/resources/application.yml#L82)** - AES-256主密钥
- **用途**: 数据库字段加密
- **生产建议**: 使用环境变量或KMS

**系统RSA密钥对**
- **配置**: **[`application.yml:88-90`](src/main/resources/application.yml#L88-L90)**
- **用途**: 登录密码加密、API密钥交换

**用户密钥对**
- 每个用户独立的RSA密钥对
- 存储在 `encryption_key` 表
- 用于审批签名

**会话密钥**
- 前端生成随机AES密钥
- 用系统公钥加密后传输
- 单次会话有效

**密钥生成工具**: [`GenerateSystemKeys.java`](GenerateSystemKeys.java)

---

### 6. 安全协议与防护机制

**理论应用**: 身份认证、访问控制、攻击防护

#### 6.1 JWT无状态认证
- **[`JwtUtil.java`](src/main/java/com/exam/approval/security/util/JwtUtil.java)** - Token生成/解析
- **[`JwtAuthenticationFilter.java`](src/main/java/com/exam/approval/security/filter/JwtAuthenticationFilter.java)** - Token验证过滤器
- HMAC-SHA256签名,防篡改

#### 6.2 防重放攻击
- **[`NonceService.java`](src/main/java/com/exam/approval/service/NonceService.java)** - Nonce验证
- 时间戳窗口检查 (5分钟)
- Redis存储已使用的nonce
- **[验证实现](src/main/java/com/exam/approval/service/PaperCryptoService.java#L46-L50)**

#### 6.3 RBAC权限控制
- **[`SecurityConfig.java`](src/main/java/com/exam/approval/config/SecurityConfig.java)** - Spring Security配置
- 角色: `ROLE_TEACHER`, `ROLE_DEPT_ADMIN`, `ROLE_COLLEGE_ADMIN`
- `@PreAuthorize` 注解方法级权限

#### 6.4 SQL注入防护
- MyBatis-Plus参数化查询
- 所有用户输入通过占位符传递

#### 6.5 XSS防护
- 输入验证、输出编码
- Content-Type正确设置

#### 6.6 CSRF防护
- **[`SecurityConfig.java`](src/main/java/com/exam/approval/config/SecurityConfig.java)** - CSRF Token配置
- 状态改变操作必须携带CSRF Token

#### 6.7 接口限流
- Guava RateLimiter限流
- 登录接口: 5次/分钟
- 普通接口: 100次/分钟
- **配置**: **[`application.yml:102-104`](src/main/resources/application.yml#L102-L104)**

---

### 7. 安全编码实践

**理论应用**: 纵深防御、安全开发生命周期

**实践要点**:
1. **最小权限原则**: 每个角色只有必需的权限
2. **默认拒绝**: 未明确允许的操作默认拒绝
3. **输入验证**: 所有外部输入都要验证
4. **错误处理**: 不暴露敏感信息的错误提示
5. **审计日志**: 记录关键操作用于审计
6. **安全配置**: 敏感配置不硬编码

**自定义注解**:
- **[`@Encrypted`](src/main/java/com/exam/approval/common/annotation/Encrypted.java)** - 标记需要加密的字段
- 声明式安全,降低遗漏风险

---

## 🎯 密码学技术对照表

| 密码学技术 | 应用场景 | 代码实现 | 配置文件 |
|----------|---------|---------|---------|
| **AES-256-GCM** | 数据库字段加密 | [`AESUtil.java`](src/main/java/com/exam/approval/security/util/AESUtil.java), [`MybatisCryptoInterceptor.java`](src/main/java/com/exam/approval/interceptor/MybatisCryptoInterceptor.java) | [`application.yml:82`](src/main/resources/application.yml#L82) |
| **AES-256-CBC** | API传输加密 | [`PaperCryptoService.java`](src/main/java/com/exam/approval/service/PaperCryptoService.java), `crypto.js` | - |
| **RSA-2048** | 数字签名、密钥交换 | [`RSAUtil.java`](src/main/java/com/exam/approval/security/util/RSAUtil.java) | [`application.yml:88-90`](src/main/resources/application.yml#L88-L90) |
| **BCrypt** | 密码存储 | Spring Security `PasswordEncoder` | - |
| **HMAC-SHA256** | 消息认证 | [`PaperCryptoService.java`](src/main/java/com/exam/approval/service/PaperCryptoService.java#L58-L64) | - |
| **SM3** | 文件完整性 | [`SM3Util.java`](src/main/java/com/exam/approval/security/util/SM3Util.java) | - |
| **JWT** | 身份认证 | [`JwtUtil.java`](src/main/java/com/exam/approval/security/util/JwtUtil.java), [`JwtAuthenticationFilter.java`](src/main/java/com/exam/approval/security/filter/JwtAuthenticationFilter.java) | [`application.yml:73-77`](src/main/resources/application.yml#L73-L77) |
| **Nonce** | 防重放攻击 | [`NonceService.java`](src/main/java/com/exam/approval/service/NonceService.java), Redis | [`application.yml:100`](src/main/resources/application.yml#L100) |

---

## 开发者
网络信息安全大作业

## 许可证
MIT License
