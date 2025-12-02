package com.blockchain.exam.blockchain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 交易实体类
 *
 * 表示待打包到区块链的交易
 * 存储在交易池中，等待矿工打包
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Data
@TableName(value = "node1_block_pool", autoResultMap = true)
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 交易ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 交易类型
     * APPROVAL_RECORD - 审批记录
     * PAPER_HASH - 试卷哈希
     * USER_AUTH - 用户权限认证
     * PAPER_CONTENT - 试卷内容
     */
    private String transactionType;

    /**
     * 交易数据（JSON格式）
     * 存储BlockData对象序列化后的JSON字符串
     */
    private String transactionData;

    /**
     * 创建节点ID
     * 记录是哪个节点创建的交易
     */
    private String creatorNode;

    /**
     * 交易状态
     * PENDING - 待打包
     * MINED - 已打包
     * INVALID - 无效
     */
    private String status;

    /**
     * 所在区块高度
     * 交易被打包后，记录所在区块的高度
     */
    private Long blockIndex;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 判断交易是否待打包
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    /**
     * 判断交易是否已打包
     */
    public boolean isMined() {
        return "MINED".equals(status);
    }

    /**
     * 标记交易为已打包
     */
    public void markAsMined(Long blockIndex) {
        this.status = "MINED";
        this.blockIndex = blockIndex;
    }

    /**
     * 标记交易为无效
     */
    public void markAsInvalid() {
        this.status = "INVALID";
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", transactionType='" + transactionType + '\'' +
                ", creatorNode='" + creatorNode + '\'' +
                ", status='" + status + '\'' +
                ", blockIndex=" + blockIndex +
                ", createTime=" + createTime +
                '}';
    }
}
