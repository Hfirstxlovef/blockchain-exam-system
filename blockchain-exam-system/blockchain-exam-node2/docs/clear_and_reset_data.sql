-- ========================================
-- 数据库清理与重置脚本
-- 用于修复加密字段问题
-- ========================================
--
-- 使用说明：
-- 1. 本脚本会清空试卷相关数据
-- 2. 保留用户数据和密钥数据
-- 3. 清空后请通过应用程序API创建新的测试数据
-- 4. 拦截器会自动加密content和filePath字段
--
-- 执行方式：
-- mysql -u root -p exam_approval < clear_and_reset_data.sql
--
-- ========================================

USE exam_approval;

-- 1. 备份现有数据（可选）
DROP TABLE IF EXISTS exam_paper_backup_20251117;
CREATE TABLE exam_paper_backup_20251117 AS SELECT * FROM exam_paper;

DROP TABLE IF EXISTS approval_workflow_backup_20251117;
CREATE TABLE approval_workflow_backup_20251117 AS SELECT * FROM approval_workflow;

DROP TABLE IF EXISTS approval_record_backup_20251117;
CREATE TABLE approval_record_backup_20251117 AS SELECT * FROM approval_record;

SELECT '✓ 数据备份完成' AS status;

-- 2. 清空试卷相关数据
-- 注意：使用TRUNCATE会重置自增ID，使用DELETE保留ID序列

-- 先清空审批记录（有外键依赖）
TRUNCATE TABLE approval_record;
SELECT '✓ 审批记录已清空' AS status;

-- 清空审批工作流
TRUNCATE TABLE approval_workflow;
SELECT '✓ 审批工作流已清空' AS status;

-- 清空试卷数据
TRUNCATE TABLE exam_paper;
SELECT '✓ 试卷数据已清空' AS status;

-- 3. 验证清理结果
SELECT
    '试卷表' AS table_name,
    COUNT(*) AS record_count
FROM exam_paper
UNION ALL
SELECT
    '审批工作流表' AS table_name,
    COUNT(*) AS record_count
FROM approval_workflow
UNION ALL
SELECT
    '审批记录表' AS table_name,
    COUNT(*) AS record_count
FROM approval_record;

-- 4. 显示保留的数据
SELECT '--- 保留的用户数据 ---' AS info;
SELECT
    id,
    username,
    real_name,
    role,
    department
FROM sys_user
ORDER BY id;

SELECT '--- 保留的加密密钥 ---' AS info;
SELECT
    id,
    user_id,
    key_type,
    created_at
FROM encryption_key
ORDER BY user_id;

-- 5. 下一步说明
SELECT '
========================================
✓ 数据清理完成！

下一步操作：
1. 重启后端服务（应用代码修改）
2. 通过前端页面或API创建新的测试试卷
3. 拦截器会自动加密content和filePath字段
4. 验证数据库中字段是否为Base64加密格式

备份位置：
- exam_paper_backup_20251117
- approval_workflow_backup_20251117
- approval_record_backup_20251117

如需恢复备份数据：
  DELETE FROM exam_paper;
  INSERT INTO exam_paper SELECT * FROM exam_paper_backup_20251117;
========================================
' AS next_steps;
