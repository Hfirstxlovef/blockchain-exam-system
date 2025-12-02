package com.exam.approval.controller;

import com.exam.approval.common.result.Result;
import com.exam.approval.entity.ApprovalRecord;
import com.exam.approval.entity.ExamPaper;
import com.exam.approval.entity.User;
import com.exam.approval.service.ApprovalService;
import com.exam.approval.service.UserService;
import com.exam.approval.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批管理Controller
 *
 * 功能：
 * 1. 获取待审批试卷列表
 * 2. 审批通过（生成数字签名）
 * 3. 审批驳回
 * 4. 查看审批记录
 * 5. 验证数字签名
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Slf4j
@Api(tags = "审批管理接口")
@RestController
@RequestMapping("/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;
    private final UserService userService;

    /**
     * 获取待审批试卷列表
     */
    @ApiOperation("获取待审批试卷")
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('DEPT_ADMIN', 'COLLEGE_ADMIN')")
    public Result<List<ExamPaper>> getPendingPapers() {
        String userRole = SecurityUtil.getCurrentUserRole();
        Long userId = SecurityUtil.getCurrentUserId();

        if (userRole == null) {
            return Result.error("未获取到用户角色");
        }

        // 获取用户部门信息
        User currentUser = userService.getUserById(userId);
        String userDepartment = currentUser != null ? currentUser.getDepartment() : null;

        List<ExamPaper> papers = approvalService.getPendingPapers(userRole, userDepartment);

        log.debug("查询待审批试卷: role={}, department={}, count={}", userRole, userDepartment, papers.size());

        return Result.success(papers);
    }

    /**
     * 审批通过
     */
    @ApiOperation("审批通过")
    @PostMapping("/approve")
    @PreAuthorize("hasAnyRole('DEPT_ADMIN', 'COLLEGE_ADMIN')")
    public Result<?> approve(@RequestBody ApprovalRequest request) {
        Long approverId = SecurityUtil.getCurrentUserId();

        if (approverId == null) {
            return Result.error("未获取到用户信息");
        }

        // 验证必填字段
        if (request.getPaperId() == null) {
            return Result.error("试卷ID不能为空");
        }

        if (request.getPrivateKey() == null || request.getPrivateKey().trim().isEmpty()) {
            return Result.error("私钥不能为空，请上传您的PEM私钥文件");
        }

        try {
            approvalService.approve(
                    request.getPaperId(),
                    approverId,
                    request.getComment(),
                    request.getPrivateKey()
            );

            log.info("审批通过: approverId={}, paperId={}", approverId, request.getPaperId());

            return Result.success("审批通过");

        } catch (Exception e) {
            log.error("审批失败: {}", e.getMessage(), e);
            return Result.error("审批失败: " + e.getMessage());
        }
    }

    /**
     * 审批驳回
     */
    @ApiOperation("审批驳回")
    @PostMapping("/reject")
    @PreAuthorize("hasAnyRole('DEPT_ADMIN', 'COLLEGE_ADMIN')")
    public Result<?> reject(@RequestBody ApprovalRequest request) {
        Long approverId = SecurityUtil.getCurrentUserId();

        if (approverId == null) {
            return Result.error("未获取到用户信息");
        }

        // 验证必填字段
        if (request.getPaperId() == null) {
            return Result.error("试卷ID不能为空");
        }

        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            return Result.error("驳回原因不能为空");
        }

        if (request.getPrivateKey() == null || request.getPrivateKey().trim().isEmpty()) {
            return Result.error("私钥不能为空，请上传您的PEM私钥文件");
        }

        try {
            approvalService.reject(
                    request.getPaperId(),
                    approverId,
                    request.getComment(),
                    request.getPrivateKey()
            );

            log.info("审批驳回: approverId={}, paperId={}", approverId, request.getPaperId());

            return Result.success("已驳回");

        } catch (Exception e) {
            log.error("驳回失败: {}", e.getMessage(), e);
            return Result.error("驳回失败: " + e.getMessage());
        }
    }

    /**
     * 查询审批记录
     */
    @ApiOperation("查询审批记录")
    @GetMapping("/records/{paperId}")
    public Result<List<ApprovalRecord>> getRecords(@PathVariable Long paperId) {
        List<ApprovalRecord> records = approvalService.getRecordsByPaperId(paperId);

        log.debug("查询审批记录: paperId={}, count={}", paperId, records.size());

        return Result.success(records);
    }

    /**
     * 验证数字签名
     */
    @ApiOperation("验证数字签名")
    @PostMapping("/verify/{recordId}")
    public Result<SignatureVerifyResult> verifySignature(@PathVariable Long recordId) {
        boolean valid = approvalService.verifySignature(recordId);

        SignatureVerifyResult result = new SignatureVerifyResult();
        result.setRecordId(recordId);
        result.setValid(valid);
        result.setMessage(valid ? "签名验证通过，审批记录真实有效" : "签名验证失败，审批记录可能被篡改");

        log.info("验证数字签名: recordId={}, valid={}", recordId, valid);

        return Result.success(result);
    }

    /**
     * 审批请求DTO
     */
    @Data
    public static class ApprovalRequest {
        private Long paperId;       // 试卷ID
        private String comment;     // 审批意见/驳回原因
        private String privateKey;  // 用户RSA私钥（Base64编码，用于生成数字签名）
    }

    /**
     * 签名验证结果DTO
     */
    @Data
    public static class SignatureVerifyResult {
        private Long recordId;      // 审批记录ID
        private Boolean valid;      // 验证结果
        private String message;     // 验证消息
    }
}
