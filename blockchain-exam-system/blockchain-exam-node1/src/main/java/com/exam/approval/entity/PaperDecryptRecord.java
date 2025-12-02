package com.exam.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 试卷解密记录实体（区块链存证）
 *
 * 用于记录每次解密操作，并上链存证，实现：
 * - 可追溯性：追踪谁在什么时候解密了哪份试卷
 * - 不可抵赖性：解密操作带数字签名
 * - 透明性：所有人可查看解密记录
 *
 * @author 网络信息安全大作业
 * @date 2025-11-29
 */
@Data
@TableName("paper_decrypt_record")
public class PaperDecryptRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 试卷ID
     */
    private Long paperId;

    /**
     * 解密用户ID
     */
    private Long userId;

    /**
     * 解密用户姓名
     */
    private String userName;

    /**
     * 用户角色（teacher/dept_admin/college_admin）
     */
    private String userRole;

    /**
     * 解密时间
     */
    private LocalDateTime decryptTime;

    /**
     * 解密时间毫秒时间戳（用于签名验证）
     * 签名时使用精确毫秒时间戳，避免LocalDateTime转换时的精度损失
     */
    private Long decryptTimeMs;

    /**
     * 解密IP地址
     */
    private String ipAddress;

    /**
     * 解密操作签名（RSA签名，Base64编码）
     * 用于验证是本人操作，确保不可抵赖
     */
    private String signature;

    /**
     * 区块链交易ID
     */
    private Long blockchainTxId;

    /**
     * 上链时间
     */
    private LocalDateTime chainTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
