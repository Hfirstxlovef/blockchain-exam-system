package com.exam.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blockchain.exam.integration.BlockchainIntegrationService;
import com.exam.approval.common.exception.BusinessException;
import com.exam.approval.entity.ApprovalRecord;
import com.exam.approval.entity.ApprovalWorkflow;
import com.exam.approval.entity.ExamPaper;
import com.exam.approval.entity.User;
import com.exam.approval.mapper.ApprovalRecordMapper;
import com.exam.approval.security.util.RSAUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 审批Service - 核心业务逻辑
 *
 * 功能：
 * 1. 三级审批流程（教师提交 → 系审批 → 院审批）
 * 2. 数字签名（RSA-2048保证不可抵赖）
 * 3. 审批记录管理
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ExamPaperService paperService;
    private final ApprovalWorkflowService workflowService;
    private final ApprovalRecordMapper recordMapper;
    private final UserService userService;

    @Autowired(required = false)
    private BlockchainIntegrationService blockchainService;

    /**
     * 获取待审批试卷列表
     *
     * @param userRole       用户角色
     * @param userDepartment 用户部门
     * @return 试卷列表
     */
    public List<ExamPaper> getPendingPapers(String userRole, String userDepartment) {
        String status;
        if ("dept_admin".equals(userRole)) {
            // 系管理员：查看待系审批的试卷（只能看本部门）
            status = "pending";
            return paperService.getByStatusAndDepartment(status, userDepartment);
        } else if ("college_admin".equals(userRole)) {
            // 院管理员：查看系已审批的试卷（可以看所有部门）
            status = "dept_approved";
            return paperService.getByStatus(status);
        } else {
            throw new BusinessException("无审批权限");
        }
    }

    /**
     * 审批通过
     *
     * @param paperId    试卷ID
     * @param approverId 审批人ID
     * @param comment    审批意见
     * @param password   审批密码（用于生成数字签名）
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long paperId, Long approverId, String comment, String password) {
        // 1. 获取审批人信息
        User approver = userService.getUserById(approverId);
        if (approver == null) {
            throw new BusinessException("审批人不存在");
        }

        // 2. 获取试卷和工作流
        ExamPaper paper = paperService.getById(paperId);
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }

        ApprovalWorkflow workflow = workflowService.getByPaperId(paperId);
        if (workflow == null) {
            throw new BusinessException("工作流不存在");
        }

        // 3. 验证审批权限
        validateApprovalPermission(approver.getRole(), workflow.getCurrentStep());

        // 4. 生成数字签名
        long timestamp = System.currentTimeMillis();
        String signData = RSAUtil.buildApprovalSignData(
                paperId, approverId, "approve", comment, timestamp
        );

        // TODO: 实际应用中，每个用户应有自己的RSA密钥对
        // 这里为了演示，使用统一的密钥
        String privateKey = getPrivateKeyForUser(approverId, password);
        String signature = RSAUtil.sign(signData, privateKey);

        // 5. 保存审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setWorkflowId(workflow.getId());
        record.setPaperId(paperId);
        record.setStep(workflow.getCurrentStep());
        record.setApproverId(approverId);
        record.setApproverName(approver.getRealName());
        record.setAction("approve");
        record.setComment(comment);
        record.setSignature(signature);
        recordMapper.insert(record);

        // 5.1 将审批记录上链（区块链存证）
        if (blockchainService != null) {
            try {
                Long txId = blockchainService.submitApprovalToChain(
                        paperId, approverId, "approve", signature,
                        approver.getRealName(), approver.getRole(), paper.getTitle());
                log.info("审批记录已上链 - 交易ID: {}, 试卷ID: {}", txId, paperId);
            } catch (Exception e) {
                log.warn("审批记录上链失败（不影响审批流程）: {}", e.getMessage());
            }
        }

        // 6. 更新工作流和试卷状态
        int nextStep = workflow.getCurrentStep() + 1;
        String newStatus;

        if (nextStep == 2) {
            // 系审批通过，进入院审批
            newStatus = "dept_approved";
            workflowService.updateStep(workflow.getId(), nextStep, "pending");
        } else if (nextStep > 2) {
            // 院审批通过，流程完成
            newStatus = "college_approved";
            workflowService.updateStep(workflow.getId(), 3, "completed");
        } else {
            newStatus = paper.getStatus();
        }

        paperService.updateStatus(paperId, newStatus);

        log.info("审批通过，试卷ID: {}, 审批人: {}, 步骤: {} → {}",
                paperId, approver.getUsername(), workflow.getCurrentStep(), nextStep);
    }

    /**
     * 审批驳回
     *
     * @param paperId    试卷ID
     * @param approverId 审批人ID
     * @param comment    驳回原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long paperId, Long approverId, String comment) {
        // 1. 获取审批人信息
        User approver = userService.getUserById(approverId);
        if (approver == null) {
            throw new BusinessException("审批人不存在");
        }

        // 2. 获取试卷和工作流
        ExamPaper paper = paperService.getById(paperId);
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }

        ApprovalWorkflow workflow = workflowService.getByPaperId(paperId);
        if (workflow == null) {
            throw new BusinessException("工作流不存在");
        }

        // 3. 验证审批权限
        validateApprovalPermission(approver.getRole(), workflow.getCurrentStep());

        // 4. 生成数字签名
        long timestamp = System.currentTimeMillis();
        String signData = RSAUtil.buildApprovalSignData(
                paperId, approverId, "reject", comment, timestamp
        );
        String privateKey = getPrivateKeyForUser(approverId, null);
        String signature = RSAUtil.sign(signData, privateKey);

        // 5. 保存审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setWorkflowId(workflow.getId());
        record.setPaperId(paperId);
        record.setStep(workflow.getCurrentStep());
        record.setApproverId(approverId);
        record.setApproverName(approver.getRealName());
        record.setAction("reject");
        record.setComment(comment);
        record.setSignature(signature);
        recordMapper.insert(record);

        // 5.1 将驳回记录上链（区块链存证）
        if (blockchainService != null) {
            try {
                Long txId = blockchainService.submitApprovalToChain(
                        paperId, approverId, "reject", signature,
                        approver.getRealName(), approver.getRole(), paper.getTitle());
                log.info("驳回记录已上链 - 交易ID: {}, 试卷ID: {}", txId, paperId);
            } catch (Exception e) {
                log.warn("驳回记录上链失败（不影响审批流程）: {}", e.getMessage());
            }
        }

        // 6. 更新工作流和试卷状态
        workflowService.updateStep(workflow.getId(), workflow.getCurrentStep(), "rejected");
        paperService.updateStatus(paperId, "rejected");

        log.info("审批驳回，试卷ID: {}, 审批人: {}", paperId, approver.getUsername());
    }

    /**
     * 获取审批记录
     *
     * @param paperId 试卷ID
     * @return 审批记录列表
     */
    public List<ApprovalRecord> getRecordsByPaperId(Long paperId) {
        return recordMapper.selectList(new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getPaperId, paperId)
                .orderByAsc(ApprovalRecord::getCreateTime));
    }

    /**
     * 验证签名
     *
     * @param recordId 审批记录ID
     * @return 验证结果
     */
    public boolean verifySignature(Long recordId) {
        ApprovalRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            return false;
        }

        // 重建签名数据
        String signData = RSAUtil.buildApprovalSignData(
                record.getPaperId(),
                record.getApproverId(),
                record.getAction(),
                record.getComment(),
                record.getCreateTime().toInstant(java.time.ZoneOffset.ofHours(8)).toEpochMilli()
        );

        // 获取公钥验证
        String publicKey = getPublicKeyForUser(record.getApproverId());
        return RSAUtil.verify(signData, record.getSignature(), publicKey);
    }

    /**
     * 验证审批权限
     */
    private void validateApprovalPermission(String userRole, Integer currentStep) {
        if (currentStep == 1 && !"dept_admin".equals(userRole)) {
            throw new BusinessException("只有系管理员可以进行系审批");
        }
        if (currentStep == 2 && !"college_admin".equals(userRole)) {
            throw new BusinessException("只有院管理员可以进行院审批");
        }
    }

    /**
     * 获取用户私钥（实际应用中应从密钥管理系统获取）
     * TODO: 实现真实的密钥管理
     */
    private String getPrivateKeyForUser(Long userId, String password) {
        // 演示用：生成临时密钥对
        // 实际应用中应：
        // 1. 从数据库encryption_key表查询用户的加密私钥
        // 2. 使用用户密码解密私钥
        // 3. 返回解密后的私钥
        return RSAUtil.generateKeyPair().get(RSAUtil.PRIVATE_KEY);
    }

    /**
     * 获取用户公钥
     */
    private String getPublicKeyForUser(Long userId) {
        // 演示用：生成临时密钥对
        // 实际应用中应从数据库查询
        return RSAUtil.generateKeyPair().get(RSAUtil.PUBLIC_KEY);
    }
}
