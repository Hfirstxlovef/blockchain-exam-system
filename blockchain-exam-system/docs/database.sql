-- ========================================
-- 分布式区块链试卷管理系统数据库设计
-- 信息安全大作业
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `blockchain_exam_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `blockchain_exam_system`;

-- ========================================
-- 1. 用户表（共享表）
-- ========================================
CREATE TABLE `sys_user` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` VARCHAR(255) NOT NULL COMMENT '真实姓名（AES加密）',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：teacher-教师, dept_admin-系管理员, college_admin-院管理员',
  `department` VARCHAR(100) DEFAULT NULL COMMENT '所属部门',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `node_id` VARCHAR(20) DEFAULT NULL COMMENT '所属节点ID',
  `rsa_public_key` TEXT COMMENT 'RSA公钥（用于数字签名）',
  `rsa_private_key_encrypted` TEXT COMMENT 'RSA私钥（加密存储）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用, 1-启用',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`),
  KEY `idx_node` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ========================================
-- 2. 试卷表（共享表，新增node_id字段）
-- ========================================
CREATE TABLE `exam_paper` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '试卷ID',
  `title` VARCHAR(200) NOT NULL COMMENT '试卷标题',
  `subject` VARCHAR(50) NOT NULL COMMENT '科目',
  `grade` VARCHAR(20) NOT NULL COMMENT '年级',
  `content` TEXT COMMENT '试卷内容（AES加密存储）',
  `content_hash` VARCHAR(64) DEFAULT NULL COMMENT '试卷内容哈希（SHA-256）',
  `file_path` VARCHAR(500) DEFAULT NULL COMMENT '附件路径（加密存储）',
  `file_name` VARCHAR(255) DEFAULT NULL COMMENT '附件原始文件名',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft-草稿, pending-待审批, dept_approved-系已审批, college_approved-院已审批, rejected-已驳回',
  `creator_id` BIGINT(20) NOT NULL COMMENT '创建人ID',
  `creator_name` VARCHAR(100) NOT NULL COMMENT '创建人姓名',
  `node_id` VARCHAR(20) DEFAULT NULL COMMENT '创建节点ID',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_node` (`node_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷表';

-- ========================================
-- 3. 审批工作流表（共享表）
-- ========================================
CREATE TABLE `approval_workflow` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '工作流ID',
  `paper_id` BIGINT(20) NOT NULL COMMENT '试卷ID',
  `current_step` INT(11) NOT NULL DEFAULT 1 COMMENT '当前步骤：1-系审批, 2-院审批, 3-完成',
  `workflow_data` TEXT COMMENT '工作流数据（JSON格式）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending-进行中, completed-已完成, rejected-已驳回',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paper` (`paper_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批工作流表';

-- ========================================
-- 4. 审批记录表（共享表，新增block_index字段）
-- ========================================
CREATE TABLE `approval_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `workflow_id` BIGINT(20) NOT NULL COMMENT '工作流ID',
  `paper_id` BIGINT(20) NOT NULL COMMENT '试卷ID',
  `step` INT(11) NOT NULL COMMENT '审批步骤：1-系审批, 2-院审批',
  `approver_id` BIGINT(20) NOT NULL COMMENT '审批人ID',
  `approver_name` VARCHAR(100) NOT NULL COMMENT '审批人姓名',
  `action` VARCHAR(20) NOT NULL COMMENT '操作：approve-通过, reject-驳回',
  `comment` TEXT COMMENT '审批意见（AES加密）',
  `signature` TEXT COMMENT '数字签名（RSA签名）',
  `block_index` BIGINT(20) DEFAULT NULL COMMENT '关联的区块高度',
  `block_hash` VARCHAR(64) DEFAULT NULL COMMENT '关联的区块哈希',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
  PRIMARY KEY (`id`),
  KEY `idx_workflow` (`workflow_id`),
  KEY `idx_paper` (`paper_id`),
  KEY `idx_approver` (`approver_id`),
  KEY `idx_block` (`block_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批记录表';

-- ========================================
-- 5. 区块链表 - 节点1（独立表）
-- ========================================
CREATE TABLE `node1_blockchain` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '区块ID',
  `block_index` BIGINT(20) NOT NULL COMMENT '区块高度',
  `previous_hash` VARCHAR(64) NOT NULL COMMENT '前一区块哈希',
  `current_hash` VARCHAR(64) NOT NULL COMMENT '当前区块哈希',
  `timestamp` BIGINT(20) NOT NULL COMMENT '时间戳（毫秒）',
  `data` TEXT NOT NULL COMMENT '区块数据（JSON格式）',
  `nonce` INT(11) NOT NULL DEFAULT 0 COMMENT 'PoW随机数',
  `merkle_root` VARCHAR(64) DEFAULT NULL COMMENT 'Merkle树根哈希',
  `difficulty` INT(11) NOT NULL DEFAULT 4 COMMENT '挖矿难度',
  `miner_address` VARCHAR(50) NOT NULL COMMENT '矿工节点ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_block_index` (`block_index`),
  UNIQUE KEY `uk_current_hash` (`current_hash`),
  KEY `idx_previous_hash` (`previous_hash`),
  KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点1区块链表';

-- ========================================
-- 6. 区块链表 - 节点2（独立表）
-- ========================================
CREATE TABLE `node2_blockchain` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '区块ID',
  `block_index` BIGINT(20) NOT NULL COMMENT '区块高度',
  `previous_hash` VARCHAR(64) NOT NULL COMMENT '前一区块哈希',
  `current_hash` VARCHAR(64) NOT NULL COMMENT '当前区块哈希',
  `timestamp` BIGINT(20) NOT NULL COMMENT '时间戳（毫秒）',
  `data` TEXT NOT NULL COMMENT '区块数据（JSON格式）',
  `nonce` INT(11) NOT NULL DEFAULT 0 COMMENT 'PoW随机数',
  `merkle_root` VARCHAR(64) DEFAULT NULL COMMENT 'Merkle树根哈希',
  `difficulty` INT(11) NOT NULL DEFAULT 4 COMMENT '挖矿难度',
  `miner_address` VARCHAR(50) NOT NULL COMMENT '矿工节点ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_block_index` (`block_index`),
  UNIQUE KEY `uk_current_hash` (`current_hash`),
  KEY `idx_previous_hash` (`previous_hash`),
  KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点2区块链表';

-- ========================================
-- 7. 区块链表 - 节点3（独立表）
-- ========================================
CREATE TABLE `node3_blockchain` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '区块ID',
  `block_index` BIGINT(20) NOT NULL COMMENT '区块高度',
  `previous_hash` VARCHAR(64) NOT NULL COMMENT '前一区块哈希',
  `current_hash` VARCHAR(64) NOT NULL COMMENT '当前区块哈希',
  `timestamp` BIGINT(20) NOT NULL COMMENT '时间戳（毫秒）',
  `data` TEXT NOT NULL COMMENT '区块数据（JSON格式）',
  `nonce` INT(11) NOT NULL DEFAULT 0 COMMENT 'PoW随机数',
  `merkle_root` VARCHAR(64) DEFAULT NULL COMMENT 'Merkle树根哈希',
  `difficulty` INT(11) NOT NULL DEFAULT 4 COMMENT '挖矿难度',
  `miner_address` VARCHAR(50) NOT NULL COMMENT '矿工节点ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_block_index` (`block_index`),
  UNIQUE KEY `uk_current_hash` (`current_hash`),
  KEY `idx_previous_hash` (`previous_hash`),
  KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点3区块链表';

-- ========================================
-- 8. 交易池表 - 节点1（独立表）
-- ========================================
CREATE TABLE `node1_block_pool` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '交易ID',
  `transaction_type` VARCHAR(50) NOT NULL COMMENT '交易类型：APPROVAL_RECORD, PAPER_HASH, USER_AUTH, PAPER_CONTENT',
  `transaction_data` TEXT NOT NULL COMMENT '交易数据（JSON格式）',
  `creator_node` VARCHAR(50) NOT NULL COMMENT '创建节点ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待打包, MINED-已打包, INVALID-无效',
  `block_index` BIGINT(20) DEFAULT NULL COMMENT '所在区块高度',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点1交易池表';

-- ========================================
-- 9. 交易池表 - 节点2（独立表）
-- ========================================
CREATE TABLE `node2_block_pool` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '交易ID',
  `transaction_type` VARCHAR(50) NOT NULL COMMENT '交易类型：APPROVAL_RECORD, PAPER_HASH, USER_AUTH, PAPER_CONTENT',
  `transaction_data` TEXT NOT NULL COMMENT '交易数据（JSON格式）',
  `creator_node` VARCHAR(50) NOT NULL COMMENT '创建节点ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待打包, MINED-已打包, INVALID-无效',
  `block_index` BIGINT(20) DEFAULT NULL COMMENT '所在区块高度',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点2交易池表';

-- ========================================
-- 10. 交易池表 - 节点3（独立表）
-- ========================================
CREATE TABLE `node3_block_pool` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '交易ID',
  `transaction_type` VARCHAR(50) NOT NULL COMMENT '交易类型：APPROVAL_RECORD, PAPER_HASH, USER_AUTH, PAPER_CONTENT',
  `transaction_data` TEXT NOT NULL COMMENT '交易数据（JSON格式）',
  `creator_node` VARCHAR(50) NOT NULL COMMENT '创建节点ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待打包, MINED-已打包, INVALID-无效',
  `block_index` BIGINT(20) DEFAULT NULL COMMENT '所在区块高度',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点3交易池表';

-- ========================================
-- 11. P2P节点配置表（共享表）
-- ========================================
CREATE TABLE `p2p_node` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '节点ID',
  `node_id` VARCHAR(50) NOT NULL COMMENT '节点标识（node1/node2/node3）',
  `node_name` VARCHAR(100) NOT NULL COMMENT '节点名称',
  `host` VARCHAR(100) NOT NULL COMMENT '主机地址',
  `port` INT(11) NOT NULL COMMENT '端口号',
  `public_key` TEXT COMMENT '节点公钥',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ONLINE' COMMENT '状态：ONLINE-在线, OFFLINE-离线',
  `last_seen_time` DATETIME DEFAULT NULL COMMENT '最后在线时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_node_id` (`node_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='P2P节点配置表';

-- ========================================
-- 12. P2P同步记录表（共享表）
-- ========================================
CREATE TABLE `p2p_sync_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '同步记录ID',
  `source_node` VARCHAR(50) NOT NULL COMMENT '源节点ID',
  `target_node` VARCHAR(50) NOT NULL COMMENT '目标节点ID',
  `sync_type` VARCHAR(50) NOT NULL COMMENT '同步类型：BLOCK_SYNC-区块同步, TRANSACTION_SYNC-交易同步',
  `last_block_index` BIGINT(20) DEFAULT NULL COMMENT '最后同步的区块高度',
  `sync_status` VARCHAR(20) NOT NULL COMMENT '同步状态：SUCCESS-成功, FAILURE-失败, IN_PROGRESS-进行中',
  `error_message` TEXT COMMENT '错误信息',
  `sync_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
  PRIMARY KEY (`id`),
  KEY `idx_source_target` (`source_node`, `target_node`),
  KEY `idx_sync_time` (`sync_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='P2P同步记录表';

-- ========================================
-- 13. 审计日志表（共享表）
-- ========================================
CREATE TABLE `audit_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT(20) DEFAULT NULL COMMENT '用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
  `operation` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `resource_type` VARCHAR(50) DEFAULT NULL COMMENT '资源类型',
  `resource_id` BIGINT(20) DEFAULT NULL COMMENT '资源ID',
  `details` TEXT COMMENT '操作详情（AES加密）',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
  `status` VARCHAR(20) DEFAULT NULL COMMENT '操作状态：success-成功, failure-失败',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_operation` (`operation`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- ========================================
-- 初始化数据
-- ========================================

-- 插入测试用户（密码均为：123456，使用Hutool BCrypt加密）
-- BCrypt加密后的 123456: $2a$10$TJndjPP23NCJ5H5RSuS0kO7U2RAiG3IysV5EzsMFeW3Vx/IX8gbLO
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `department`, `node_id`, `email`, `phone`, `status`)
VALUES
  -- 节点1（计算机系）
  ('cs_teacher1', '$2a$10$TJndjPP23NCJ5H5RSuS0kO7U2RAiG3IysV5EzsMFeW3Vx/IX8gbLO', '张教师', 'teacher', '计算机系', 'node1', 'cs_teacher1@example.com', '13800138001', 1),
  ('cs_teacher2', '$2a$10$TJndjPP23NCJ5H5RSuS0kO7U2RAiG3IysV5EzsMFeW3Vx/IX8gbLO', '李教师', 'teacher', '计算机系', 'node1', 'cs_teacher2@example.com', '13800138002', 1),
  ('cs_dept_admin', '$2a$10$TJndjPP23NCJ5H5RSuS0kO7U2RAiG3IysV5EzsMFeW3Vx/IX8gbLO', '王主任', 'dept_admin', '计算机系', 'node1', 'cs_dept@example.com', '13800138003', 1),

  -- 节点2（软件工程系）
  ('se_teacher1', '$2a$10$TJndjPP23NCJ5H5RSuS0kO7U2RAiG3IysV5EzsMFeW3Vx/IX8gbLO', '刘教师', 'teacher', '软件工程系', 'node2', 'se_teacher1@example.com', '13800138004', 1),
  ('se_teacher2', '$2a$10$TJndjPP23NCJ5H5RSuS0kO7U2RAiG3IysV5EzsMFeW3Vx/IX8gbLO', '陈教师', 'teacher', '软件工程系', 'node2', 'se_teacher2@example.com', '13800138005', 1),
  ('se_dept_admin', '$2a$10$TJndjPP23NCJ5H5RSuS0kO7U2RAiG3IysV5EzsMFeW3Vx/IX8gbLO', '周主任', 'dept_admin', '软件工程系', 'node2', 'se_dept@example.com', '13800138006', 1),

  -- 节点3（信息学院）
  ('college_admin1', '$2a$10$TJndjPP23NCJ5H5RSuS0kO7U2RAiG3IysV5EzsMFeW3Vx/IX8gbLO', '赵院长', 'college_admin', '信息学院', 'node3', 'college1@example.com', '13800138007', 1),
  ('college_admin2', '$2a$10$TJndjPP23NCJ5H5RSuS0kO7U2RAiG3IysV5EzsMFeW3Vx/IX8gbLO', '孙院长', 'college_admin', '信息学院', 'node3', 'college2@example.com', '13800138008', 1);

-- 插入P2P节点配置
INSERT INTO `p2p_node` (`node_id`, `node_name`, `host`, `port`, `status`)
VALUES
  ('node1', '计算机系节点', 'localhost', 8081, 'ONLINE'),
  ('node2', '软件工程系节点', 'localhost', 8082, 'ONLINE'),
  ('node3', '信息学院节点', 'localhost', 8083, 'ONLINE');

-- ========================================
-- 数据库初始化完成
-- ========================================

-- 查看表结构
SHOW TABLES;

-- 查看用户数据
SELECT id, username, role, department, node_id, status FROM sys_user;

-- 查看节点配置
SELECT * FROM p2p_node;
