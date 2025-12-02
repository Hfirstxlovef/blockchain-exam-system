# 分布式区块链试卷管理系统

> 信息安全大作业 - 基于区块链和P2P技术的分布式试卷审批系统

## 项目概述

本项目是一个基于区块链技术和P2P网络的分布式试卷管理系统，实现了试卷的分布式存储、审批流程上链、以及节点间的数据同步。

### 核心特性

- ✅ **分布式架构**：3个独立节点（计算机系、软件工程系、信息学院）
- ✅ **区块链技术**：自实现区块链，PoW共识算法（difficulty=4）
- ✅ **P2P通信**：简化的P2P网络，HTTP REST API通信
- ✅ **加密安全**：RSA-1024 + AES-128-CBC + SM3国密算法
- ✅ **业务上链**：审批记录、试卷哈希、数字签名全部上链
- ✅ **三级审批**：教师提交 → 系主任审批 → 院长终审

### 项目亮点

| 特性 | 说明 |
|------|------|
| 完整区块链实现 | PoW共识、自动挖矿、区块验证 |
| P2P网络通信 | 节点心跳、区块同步、交易广播 |
| 三级审批工作流 | 教师→系主任→院长的完整审批链 |
| 数字签名 | RSA签名确保审批不可抵赖 |
| 防篡改机制 | SHA-256哈希上链，数据不可篡改 |
| 审计追踪 | 完整的操作日志和审计记录 |

### 技术栈

**后端**：
- Spring Boot 2.7.18
- MyBatis Plus 3.5.3.1
- MySQL 8.0
- Redis
- Spring Security + JWT

**区块链**：
- 自实现区块链核心
- SHA-256 哈希算法
- PoW 共识机制（difficulty=4）
- 链式存储

**前端**：
- Vue 3 + Vite
- Element Plus
- Apple高雅白设计风格

## 项目结构

```
blockchain-exam-system/
├── blockchain-exam-node1/          # 节点1：计算机系（8081端口）
│   ├── src/main/java/
│   │   ├── com.blockchain.exam/    # 区块链核心模块
│   │   │   ├── blockchain/         # 区块链实现
│   │   │   ├── p2p/                # P2P通信
│   │   │   └── integration/        # 业务集成
│   │   └── com.exam.approval/      # 业务模块
│   └── pom.xml
├── blockchain-exam-node2/          # 节点2：软件工程系（8082端口）
├── blockchain-exam-node3/          # 节点3：信息学院（8083端口）
├── blockchain-exam-frontend/       # 前端项目（Vue 3）
├── docs/                           # 文档目录
│   ├── database.sql                # 数据库初始化脚本
│   ├── 阶段1总结.md                 # 基础环境搭建
│   ├── 阶段2总结.md                 # 区块链核心实现
│   ├── 阶段3总结.md                 # P2P通信实现
│   ├── 阶段4总结.md                 # 业务功能集成
│   └── 项目总结.md                  # 完整项目总结
└── README.md                       # 项目说明文档
```

## 数据库设计

### 共享表（7张）

| 表名 | 说明 |
|------|------|
| sys_user | 用户表（含RSA密钥） |
| exam_paper | 试卷表（含内容哈希） |
| approval_record | 审批记录表（含区块哈希） |
| approval_workflow | 审批工作流表 |
| p2p_node | P2P节点配置表 |
| p2p_sync_log | P2P同步记录表 |
| audit_log | 审计日志表 |

### 节点独立表（6张）

| 表名 | 说明 |
|------|------|
| node1_blockchain | 节点1区块链表 |
| node1_block_pool | 节点1交易池表 |
| node2_blockchain | 节点2区块链表 |
| node2_block_pool | 节点2交易池表 |
| node3_blockchain | 节点3区块链表 |
| node3_block_pool | 节点3交易池表 |

## 快速开始

### 1. 环境要求

- JDK 8+
- Maven 3.6+
- MySQL 8.0
- Redis
- Node.js 16+（前端）

### 2. 数据库初始化

```bash
# 执行数据库脚本
mysql -u root -p < docs/database.sql
```

### 3. 启动后端节点

```bash
# 启动节点1（计算机系）
cd blockchain-exam-node1
mvn spring-boot:run

# 启动节点2（软件工程系）
cd blockchain-exam-node2
mvn spring-boot:run

# 启动节点3（信息学院）
cd blockchain-exam-node3
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd blockchain-exam-frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

### 5. API测试

```bash
# 查看节点状态
curl http://localhost:8081/api/p2p/nodes/current

# 查看区块链
curl http://localhost:8081/api/blockchain/chain

# 手动挖矿
curl -X POST http://localhost:8081/api/blockchain/mine
```

## 开发进度

### ✅ 阶段1：基础环境搭建（已完成）

- [x] 创建项目目录结构
- [x] 创建数据库并执行DDL脚本（13张表）
- [x] 创建3个节点项目
- [x] 配置application-node{X}.yml文件
- [x] 初始化测试用户数据（8个用户）
- [x] 配置P2P节点信息

### ✅ 阶段2：区块链核心实现（已完成）

- [x] 创建Block实体类和BlockData数据结构
- [x] 创建Transaction交易实体
- [x] 实现HashUtil（SHA-256）
- [x] 实现ConsensusService（PoW算法，difficulty=4）
- [x] 实现BlockchainService（区块链管理）
- [x] 实现MinerService（自动挖矿，30秒间隔）

### ✅ 阶段3：P2P通信实现（已完成）

- [x] 创建P2PConfig配置类
- [x] 实现NodeService（节点管理、心跳检测）
- [x] 实现SyncService（区块链同步，60秒间隔）
- [x] 实现TransactionService（交易广播、去重）
- [x] 多节点联调测试通过

### ✅ 阶段4：业务功能集成（已完成）

- [x] BlockchainIntegrationService（业务与区块链集成）
- [x] 审批记录自动上链
- [x] 试卷哈希自动上链
- [x] 数字签名验证
- [x] 审计日志记录

### 🔄 阶段5：前端界面（进行中）

- [x] 复用参考项目前端代码
- [x] Apple高雅白设计风格
- [ ] 新增区块链浏览器页面
- [ ] 新增节点管理页面

### ⏳ 阶段6：测试和演示

- [ ] 功能测试文档
- [ ] 演示脚本
- [ ] 系统演示

## 测试账号

所有账号密码均为：**123456**

| 用户名 | 角色 | 部门 | 节点 |
|--------|------|------|------|
| cs_teacher1 | 教师 | 计算机系 | node1 (8081) |
| cs_teacher2 | 教师 | 计算机系 | node1 (8081) |
| cs_dept_admin | 系主任 | 计算机系 | node1 (8081) |
| se_teacher1 | 教师 | 软件工程系 | node2 (8082) |
| se_teacher2 | 教师 | 软件工程系 | node2 (8082) |
| se_dept_admin | 系主任 | 软件工程系 | node2 (8082) |
| college_admin1 | 院长 | 信息学院 | node3 (8083) |
| college_admin2 | 院长 | 信息学院 | node3 (8083) |

## 核心功能

### 1. 区块链特性

| 功能 | 说明 |
|------|------|
| PoW共识 | difficulty=4，哈希前4位为0 |
| 自动挖矿 | 每30秒检查交易池并挖矿 |
| 批量打包 | 每次打包最多3笔交易 |
| 区块验证 | PoW验证 + 哈希链验证 |
| 创世区块 | 系统启动自动创建 |

### 2. P2P通信

| 功能 | 说明 |
|------|------|
| 心跳检测 | 每60秒更新节点状态 |
| 超时检测 | 每5分钟检测离线节点 |
| 区块同步 | 每60秒同步最长链 |
| 交易广播 | 自动广播到邻居节点 |
| 交易去重 | 基于内容的去重机制 |

### 3. 业务功能

| 功能 | 说明 |
|------|------|
| 三级审批 | 教师→系主任→院长 |
| 数字签名 | RSA-1024签名 |
| 审批上链 | 审批记录自动上链 |
| 试卷哈希 | SHA-256防篡改 |
| 审计日志 | 完整操作记录 |

### 4. 加密安全

| 算法 | 用途 |
|------|------|
| RSA-1024 | 数字签名、密钥交换 |
| AES-128-CBC | 试卷内容加密 |
| SM3 | 国密哈希算法 |
| SHA-256 | 区块链哈希 |
| BCrypt | 密码存储 |

## API文档

### 区块链API（/api/blockchain）

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | /chain | 获取完整区块链 |
| GET | /latest | 获取最新区块 |
| GET | /block/{index} | 查询指定区块 |
| GET | /stats | 获取统计信息 |
| GET | /validate | 验证区块链 |
| POST | /mine | 手动挖矿 |
| POST | /sync | 手动同步 |

### 交易API（/api/transaction）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | /create | 创建交易 |
| GET | /pending | 查询待打包交易 |
| GET | /{id} | 查询交易详情 |
| GET | /stats | 交易统计 |

### P2P API（/api/p2p）

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | /nodes | 获取所有节点 |
| GET | /nodes/online | 获取在线节点 |
| GET | /nodes/current | 当前节点信息 |
| GET | /network/stats | 网络统计 |
| GET | /health | 健康检查 |

## 代码统计

| 指标 | 数量 |
|------|------|
| 总文件数 | 216个 |
| 总代码行数 | 32,461行 |
| Git提交次数 | 6次 |
| 数据表数量 | 13张 |
| API端点数 | 30+ |

## 开发规范

### Claude八耻八荣

- ✅ 以认真查阅为荣，以暗猜接口为耻
- ✅ 以寻求确认为荣，以模糊执行为耻
- ✅ 以人类确认为荣，以盲想业务为耻
- ✅ 以复用现有为荣，以创造接口为耻
- ✅ 以主动测试为荣，以跳过验证为耻
- ✅ 以遵循规范为荣，以破坏架构为耻
- ✅ 以诚实无知为荣，以假装理解为耻
- ✅ 以谨慎重构为荣，以盲目修改为耻

## 许可证

本项目仅用于教育学习目的。

---

**最后更新时间**：2025-11-26
**当前阶段**：阶段1-4已完成，阶段5进行中
**项目状态**：核心功能全部完成 ✅
