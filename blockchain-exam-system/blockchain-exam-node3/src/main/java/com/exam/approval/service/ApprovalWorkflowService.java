package com.exam.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.approval.entity.ApprovalWorkflow;
import com.exam.approval.mapper.ApprovalWorkflowMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 审批工作流Service
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Slf4j
@Service
public class ApprovalWorkflowService extends ServiceImpl<ApprovalWorkflowMapper, ApprovalWorkflow> {

    /**
     * 根据试卷ID查询工作流
     *
     * @param paperId 试卷ID
     * @return 工作流对象
     */
    public ApprovalWorkflow getByPaperId(Long paperId) {
        return this.getOne(new LambdaQueryWrapper<ApprovalWorkflow>()
                .eq(ApprovalWorkflow::getPaperId, paperId));
    }

    /**
     * 更新工作流步骤
     *
     * @param workflowId  工作流ID
     * @param currentStep 当前步骤
     * @param status      状态
     */
    public void updateStep(Long workflowId, Integer currentStep, String status) {
        ApprovalWorkflow workflow = this.getById(workflowId);
        if (workflow != null) {
            workflow.setCurrentStep(currentStep);
            workflow.setStatus(status);
            this.updateById(workflow);
            log.info("更新工作流，ID: {}, 步骤: {}, 状态: {}", workflowId, currentStep, status);
        }
    }
}
