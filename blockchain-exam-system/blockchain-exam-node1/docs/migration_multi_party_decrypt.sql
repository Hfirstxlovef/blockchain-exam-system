-- ========================================
-- 多方解密 + 解密记录上链 数据库迁移脚本
-- 符合区块链理念的权限控制
-- ========================================
--
-- 使用说明：
-- 1. 本脚本添加多方加密支持和解密审计功能
-- 2. 创建解密记录表用于区块链存证
-- 3. 添加encrypted_aes_keys字段支持多用户解密
--
-- 执行方式：
-- mysql -u root -p exam_approval < migration_multi_party_decrypt.sql
--
-- ========================================

USE blockchain_exam_system;

-- 1. 创建解密记录表（用于区块链存证）
CREATE TABLE IF NOT EXISTS paper_decrypt_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    paper_id BIGINT NOT NULL COMMENT '试卷ID',
    user_id BIGINT NOT NULL COMMENT '解密用户ID',
    user_name VARCHAR(50) COMMENT '解密用户姓名',
    user_role VARCHAR(20) COMMENT '用户角色',
    decrypt_time DATETIME NOT NULL COMMENT '解密时间',
    ip_address VARCHAR(50) COMMENT '解密IP地址',
    signature VARCHAR(1024) COMMENT '解密操作签名（RSA签名，Base64编码）',
    blockchain_tx_id BIGINT COMMENT '区块链交易ID',
    chain_time DATETIME COMMENT '上链时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_paper_id (paper_id),
    INDEX idx_user_id (user_id),
    INDEX idx_decrypt_time (decrypt_time),
    INDEX idx_blockchain_tx_id (blockchain_tx_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷解密记录表（区块链存证）';

SELECT '✓ paper_decrypt_record 表创建完成' AS status;

-- 2. exam_paper 添加多方加密密钥字段
-- JSON格式: [{"userId": 1, "encryptedKey": "xxx"}, {"userId": 2, "encryptedKey": "yyy"}]
ALTER TABLE exam_paper
ADD COLUMN IF NOT EXISTS encrypted_aes_keys JSON COMMENT '多方加密的AES密钥列表（JSON格式）';

SELECT '✓ encrypted_aes_keys 字段添加完成' AS status;

-- 3. 验证表结构
DESCRIBE paper_decrypt_record;
DESCRIBE exam_paper;

-- 4. 显示迁移完成信息
SELECT '
========================================
✓ 数据库迁移完成！

新增内容：
1. paper_decrypt_record 表 - 解密记录存证
   - paper_id: 试卷ID
   - user_id: 解密用户ID
   - decrypt_time: 解密时间
   - signature: 解密操作签名
   - blockchain_tx_id: 区块链交易ID

2. exam_paper.encrypted_aes_keys 字段
   - JSON格式存储多用户加密密钥
   - 格式: [{"userId": 1, "encryptedKey": "xxx"}, ...]

权限控制模型：
  本系教师 → 本系主任 → 本学院院长
     ↓          ↓           ↓
   创建试卷   系级审批    院级审批

每个人用自己的私钥解密，操作上链记录
========================================
' AS migration_complete;
