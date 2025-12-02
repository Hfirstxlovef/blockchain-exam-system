package com.blockchain.exam.p2p.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * P2P同步日志实体类
 *
 * 记录节点间的区块链同步和交易同步日志
 * 用于跟踪同步状态、诊断同步问题
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Data
@TableName("p2p_sync_log")
public class P2PSyncLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 同步记录ID（数据库主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 源节点ID（发起同步的节点）
     */
    private String sourceNode;

    /**
     * 目标节点ID（被同步的节点）
     */
    private String targetNode;

    /**
     * 同步类型
     * BLOCK_SYNC - 区块同步
     * TRANSACTION_SYNC - 交易同步
     */
    private String syncType;

    /**
     * 最后同步的区块高度
     */
    private Long lastBlockIndex;

    /**
     * 同步状态
     * SUCCESS - 成功
     * FAILURE - 失败
     * IN_PROGRESS - 进行中
     */
    private String syncStatus;

    /**
     * 错误信息（失败时记录）
     */
    private String errorMessage;

    /**
     * 同步时间
     */
    private LocalDateTime syncTime;

    /**
     * 判断是否同步成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(syncStatus);
    }

    /**
     * 判断是否同步失败
     */
    public boolean isFailure() {
        return "FAILURE".equals(syncStatus);
    }

    /**
     * 判断是否正在同步
     */
    public boolean isInProgress() {
        return "IN_PROGRESS".equals(syncStatus);
    }

    /**
     * 标记为成功
     */
    public void markAsSuccess(Long lastBlockIndex) {
        this.syncStatus = "SUCCESS";
        this.lastBlockIndex = lastBlockIndex;
        this.errorMessage = null;
    }

    /**
     * 标记为失败
     */
    public void markAsFailure(String errorMessage) {
        this.syncStatus = "FAILURE";
        this.errorMessage = errorMessage;
    }

    /**
     * 标记为进行中
     */
    public void markAsInProgress() {
        this.syncStatus = "IN_PROGRESS";
        this.errorMessage = null;
    }

    @Override
    public String toString() {
        return "P2PSyncLog{" +
                "id=" + id +
                ", sourceNode='" + sourceNode + '\'' +
                ", targetNode='" + targetNode + '\'' +
                ", syncType='" + syncType + '\'' +
                ", lastBlockIndex=" + lastBlockIndex +
                ", syncStatus='" + syncStatus + '\'' +
                ", syncTime=" + syncTime +
                '}';
    }
}
