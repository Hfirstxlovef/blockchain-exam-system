package com.blockchain.exam.blockchain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 区块实体类
 *
 * 区块是区块链的基本单元，包含区块高度、哈希、数据等信息
 * 通过previousHash形成链式结构
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Data
@TableName(value = "node1_blockchain", autoResultMap = true)
public class Block implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 区块ID（数据库主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 区块高度（区块在链中的位置，从0开始）
     */
    private Long blockIndex;

    /**
     * 前一区块哈希值（SHA-256，64字符）
     * 创世区块的previousHash为"0"
     */
    private String previousHash;

    /**
     * 当前区块哈希值（SHA-256，64字符）
     * 通过PoW计算得出，必须满足难度要求
     */
    private String currentHash;

    /**
     * 时间戳（Unix毫秒时间戳）
     */
    private Long timestamp;

    /**
     * 区块数据（JSON格式）
     * 存储BlockData对象序列化后的JSON字符串
     * 包含交易类型、交易数据等信息
     */
    private String data;

    /**
     * PoW工作量证明的随机数
     * 通过不断尝试不同的nonce值，使区块哈希满足难度要求
     */
    private Integer nonce;

    /**
     * Merkle树根哈希（可选）
     * 用于快速验证区块中的交易数据
     */
    private String merkleRoot;

    /**
     * 挖矿难度
     * 表示哈希值前N位必须为0，例如difficulty=4表示哈希前4位为0
     */
    private Integer difficulty;

    /**
     * 矿工节点地址
     * 记录是哪个节点挖出了这个区块
     */
    private String minerAddress;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 判断是否为创世区块
     */
    public boolean isGenesisBlock() {
        return blockIndex != null && blockIndex == 0L;
    }

    /**
     * 构建区块信息字符串（用于计算哈希）
     */
    public String buildHashData() {
        return blockIndex + previousHash + timestamp + data + nonce;
    }

    @Override
    public String toString() {
        return "Block{" +
                "id=" + id +
                ", blockIndex=" + blockIndex +
                ", previousHash='" + previousHash + '\'' +
                ", currentHash='" + currentHash + '\'' +
                ", timestamp=" + timestamp +
                ", nonce=" + nonce +
                ", difficulty=" + difficulty +
                ", minerAddress='" + minerAddress + '\'' +
                '}';
    }
}
