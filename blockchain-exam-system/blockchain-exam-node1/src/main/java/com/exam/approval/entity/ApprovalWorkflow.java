package com.exam.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批工作流实体
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Data
@TableName("approval_workflow")
public class ApprovalWorkflow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工作流ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 试卷ID
     */
    private Long paperId;

    /**
     * 当前步骤：1-系审批, 2-院审批, 3-完成
     */
    private Integer currentStep;

    /**
     * 工作流数据（JSON格式）
     */
    private String workflowData;

    /**
     * 状态：pending-进行中, completed-已完成, rejected-已驳回
     */
    private String status;

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
}
