# 安装与启动指南

## 环境要求

| 软件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 8+ (推荐17) | 后端运行环境 |
| Maven | 3.6+ | 项目构建工具 |
| MySQL | 8.0 | 数据库 |
| Redis | 6.0+ | 缓存服务 |
| Node.js | 16+ | 前端运行环境 |
| npm | 8+ | 前端包管理器 |

## 一、数据库配置

### 1.1 启动MySQL服务

```bash
# macOS (Homebrew)
brew services start mysql

# Linux
sudo systemctl start mysql

# Windows
net start mysql
```

### 1.2 创建数据库

```bash
# 登录MySQL
mysql -u root -p

# 执行数据库脚本
source docs/database.sql
```

或者直接执行：

```bash
mysql -u root -p < docs/database.sql
```

### 1.3 修改数据库配置

编辑每个节点的配置文件，修改数据库连接信息：

**节点1** - `blockchain-exam-node1/src/main/resources/application.yml`
**节点2** - `blockchain-exam-node2/src/main/resources/application.yml`
**节点3** - `blockchain-exam-node3/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blockchain_exam_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 你的数据库密码  # 修改为你的密码
```

## 二、启动Redis

```bash
# macOS (Homebrew)
brew services start redis

# Linux
sudo systemctl start redis

# Windows
redis-server

# 验证Redis是否启动
redis-cli ping
# 返回 PONG 表示成功
```

## 三、启动后端服务

需要启动3个区块链节点，建议打开3个终端窗口分别执行：

### 3.1 启动节点1（计算机系 - 端口8081）

```bash
cd blockchain-exam-node1
mvn clean spring-boot:run -Dspring-boot.run.profiles=node1
```

### 3.2 启动节点2（软件工程系 - 端口8082）

```bash
cd blockchain-exam-node2
mvn clean spring-boot:run -Dspring-boot.run.profiles=node2
```

### 3.3 启动节点3（信息学院 - 端口8083）

```bash
cd blockchain-exam-node3
mvn clean spring-boot:run -Dspring-boot.run.profiles=node3
```

### 3.4 验证后端启动

```bash
# 检查节点1状态
curl http://localhost:8081/api/p2p/health

# 检查节点2状态
curl http://localhost:8082/api/p2p/health

# 检查节点3状态
curl http://localhost:8083/api/p2p/health
```

## 四、启动前端

```bash
cd blockchain-exam-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

启动成功后访问：**http://localhost:5173**

## 五、快速启动脚本

### macOS/Linux 一键启动

创建 `start.sh`：

```bash
#!/bin/bash

echo "启动 Redis..."
brew services start redis

echo "启动节点1..."
cd blockchain-exam-node1 && mvn spring-boot:run -Dspring-boot.run.profiles=node1 &

echo "启动节点2..."
cd ../blockchain-exam-node2 && mvn spring-boot:run -Dspring-boot.run.profiles=node2 &

echo "启动节点3..."
cd ../blockchain-exam-node3 && mvn spring-boot:run -Dspring-boot.run.profiles=node3 &

echo "启动前端..."
cd ../blockchain-exam-frontend && npm run dev &

echo "所有服务启动中..."
echo "前端访问地址: http://localhost:5173"
echo "节点1 API: http://localhost:8081/api"
echo "节点2 API: http://localhost:8082/api"
echo "节点3 API: http://localhost:8083/api"
```

## 六、测试账号

所有账号密码均为：**123456**

| 用户名 | 角色 | 部门 | 对应节点 |
|--------|------|------|----------|
| cs_teacher1 | 教师 | 计算机系 | node1 (8081) |
| cs_teacher2 | 教师 | 计算机系 | node1 (8081) |
| cs_dept_admin | 系主任 | 计算机系 | node1 (8081) |
| se_teacher1 | 教师 | 软件工程系 | node2 (8082) |
| se_teacher2 | 教师 | 软件工程系 | node2 (8082) |
| se_dept_admin | 系主任 | 软件工程系 | node2 (8082) |
| college_admin1 | 院长 | 信息学院 | node3 (8083) |
| college_admin2 | 院长 | 信息学院 | node3 (8083) |

## 七、常用API测试

```bash
# 查看区块链
curl http://localhost:8081/api/blockchain/chain

# 查看最新区块
curl http://localhost:8081/api/blockchain/latest

# 手动挖矿
curl -X POST http://localhost:8081/api/blockchain/mine

# 查看节点状态
curl http://localhost:8081/api/p2p/nodes/current

# 查看网络统计
curl http://localhost:8081/api/p2p/network/stats

# 验证区块链完整性
curl http://localhost:8081/api/blockchain/validate
```

## 八、常见问题

### Q1: Maven构建失败
```bash
# 清理并重新构建
mvn clean install -DskipTests
```

### Q2: 端口被占用
```bash
# 查找占用端口的进程
lsof -i :8081
# 杀死进程
kill -9 <PID>
```

### Q3: MySQL连接失败
- 检查MySQL服务是否启动
- 检查用户名密码是否正确
- 检查数据库是否已创建

### Q4: Redis连接失败
- 检查Redis服务是否启动：`redis-cli ping`
- 默认端口：6379

### Q5: 前端启动失败
```bash
# 删除node_modules重新安装
rm -rf node_modules
npm install
```

## 九、停止服务

```bash
# 停止所有Java进程
pkill -f spring-boot

# 停止前端
# 在前端终端按 Ctrl+C

# 停止Redis
brew services stop redis

# 停止MySQL
brew services stop mysql
```

---

**技术支持**：如有问题请提交Issue
