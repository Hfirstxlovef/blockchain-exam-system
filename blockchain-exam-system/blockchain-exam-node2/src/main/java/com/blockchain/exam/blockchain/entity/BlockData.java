package com.blockchain.exam.blockchain.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 区块数据结构
 *
 * 定义区块中存储的数据格式
 * 将业务数据（试卷、审批记录等）转换为区块链可存储的格式
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Data
public class BlockData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据类型
     * APPROVAL_RECORD - 审批记录
     * PAPER_HASH - 试卷哈希
     * USER_AUTH - 用户权限认证
     * PAPER_CONTENT - 试卷内容
     */
    private String type;

    /**
     * 关联的业务ID
     * 例如：试卷ID、审批记录ID等
     */
    private Long referenceId;

    /**
     * 具体数据（键值对）
     * 存储业务相关的数据
     */
    private Map<String, Object> data;

    /**
     * 数据创建时间戳
     */
    private Long timestamp;

    /**
     * 创建审批记录数据
     *
     * @param paperId      试卷ID
     * @param approverId   审批人ID
     * @param action       操作类型（approve/reject）
     * @param signature    数字签名
     * @param approverName 审批人姓名
     * @param approverRole 审批人角色
     * @param paperTitle   试卷标题
     * @return BlockData对象
     */
    public static BlockData createApprovalRecord(Long paperId, Long approverId,
                                                  String action, String signature,
                                                  String approverName, String approverRole,
                                                  String paperTitle) {
        BlockData blockData = new BlockData();
        blockData.setType("APPROVAL_RECORD");
        blockData.setReferenceId(paperId);
        blockData.setTimestamp(System.currentTimeMillis());

        Map<String, Object> data = new HashMap<>();
        data.put("paperId", paperId);
        data.put("approverId", approverId);
        data.put("action", action);
        data.put("signature", signature);
        data.put("approverName", approverName);
        data.put("approverRole", approverRole);
        data.put("paperTitle", paperTitle);
        data.put("timestamp", System.currentTimeMillis());

        blockData.setData(data);
        return blockData;
    }

    /**
     * 创建试卷哈希数据
     *
     * @param paperId    试卷ID
     * @param paperHash  试卷内容哈希
     * @return BlockData对象
     */
    public static BlockData createPaperHash(Long paperId, String paperHash) {
        BlockData blockData = new BlockData();
        blockData.setType("PAPER_HASH");
        blockData.setReferenceId(paperId);
        blockData.setTimestamp(System.currentTimeMillis());

        Map<String, Object> data = new HashMap<>();
        data.put("paperId", paperId);
        data.put("paperHash", paperHash);
        data.put("timestamp", System.currentTimeMillis());

        blockData.setData(data);
        return blockData;
    }

    /**
     * 创建用户权限认证数据
     *
     * @param userId   用户ID
     * @param role     角色
     * @param action   操作类型
     * @return BlockData对象
     */
    public static BlockData createUserAuth(Long userId, String role, String action) {
        BlockData blockData = new BlockData();
        blockData.setType("USER_AUTH");
        blockData.setReferenceId(userId);
        blockData.setTimestamp(System.currentTimeMillis());

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("role", role);
        data.put("action", action);
        data.put("timestamp", System.currentTimeMillis());

        blockData.setData(data);
        return blockData;
    }

    /**
     * 创建试卷内容数据（加密）
     *
     * @param paperId          试卷ID
     * @param encryptedContent 加密后的试卷内容
     * @param contentHash      试卷内容哈希
     * @param creatorId        创建者ID
     * @param creatorName      创建者姓名
     * @param paperTitle       试卷标题
     * @return BlockData对象
     */
    public static BlockData createPaperContent(Long paperId, String encryptedContent,
                                               String contentHash, Long creatorId,
                                               String creatorName, String paperTitle) {
        BlockData blockData = new BlockData();
        blockData.setType("PAPER_CONTENT");
        blockData.setReferenceId(paperId);
        blockData.setTimestamp(System.currentTimeMillis());

        Map<String, Object> data = new HashMap<>();
        data.put("paperId", paperId);
        data.put("encryptedContent", encryptedContent);
        data.put("contentHash", contentHash);
        data.put("creatorId", creatorId);
        data.put("creatorName", creatorName);
        data.put("paperTitle", paperTitle);
        data.put("timestamp", System.currentTimeMillis());

        blockData.setData(data);
        return blockData;
    }

    /**
     * 创建解密记录数据
     *
     * @param paperId     试卷ID
     * @param userId      解密用户ID
     * @param userName    解密用户名
     * @param userRole    用户角色
     * @param contentHash 试卷内容哈希
     * @param signature   用户签名
     * @return BlockData对象
     */
    public static BlockData createDecryptRecord(Long paperId, Long userId, String userName,
                                                 String userRole, String contentHash, String signature) {
        BlockData blockData = new BlockData();
        blockData.setType("DECRYPT_RECORD");
        blockData.setReferenceId(paperId);
        blockData.setTimestamp(System.currentTimeMillis());

        Map<String, Object> data = new HashMap<>();
        data.put("paperId", paperId);
        data.put("userId", userId);
        data.put("userName", userName);
        data.put("userRole", userRole);
        data.put("contentHash", contentHash);
        data.put("signature", signature);
        data.put("timestamp", System.currentTimeMillis());

        blockData.setData(data);
        return blockData;
    }

    /**
     * 创建交易批次数据（用于矿工打包多笔交易）
     *
     * @param transactions 交易列表
     * @return BlockData对象
     */
    public static BlockData createTransactionBatch(Object transactions) {
        BlockData blockData = new BlockData();
        blockData.setType("TRANSACTION_BATCH");
        blockData.setTimestamp(System.currentTimeMillis());

        Map<String, Object> data = new HashMap<>();
        data.put("transactions", transactions);
        data.put("count", transactions instanceof java.util.List ?
                ((java.util.List<?>) transactions).size() : 0);
        data.put("timestamp", System.currentTimeMillis());

        blockData.setData(data);
        return blockData;
    }

    @Override
    public String toString() {
        return "BlockData{" +
                "type='" + type + '\'' +
                ", referenceId=" + referenceId +
                ", timestamp=" + timestamp +
                ", dataSize=" + (data != null ? data.size() : 0) +
                '}';
    }
}
