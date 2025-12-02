package com.exam.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.exam.approval.common.annotation.Encrypted;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批记录实体
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Data
@TableName("approval_record")
public class ApprovalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工作流ID
     */
    private Long workflowId;

    /**
     * 试卷ID
     */
    private Long paperId;

    /**
     * 审批步骤：1-系审批, 2-院审批
     */
    private Integer step;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 操作：approve-通过, reject-驳回
     */
    private String action;

    /**
     * 审批意见（AES加密）
     */
    @Encrypted
    private String comment;

    /**
     * 数字签名（RSA签名）
     */
    private String signature;

    /**
     * 审批时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
