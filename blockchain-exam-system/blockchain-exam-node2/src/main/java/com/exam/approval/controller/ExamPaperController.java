package com.exam.approval.controller;

import com.blockchain.exam.blockchain.util.HashUtil;
import com.blockchain.exam.integration.BlockchainIntegrationService;
import com.exam.approval.common.result.Result;
import com.exam.approval.dto.PaperDecryptRequest;
import com.exam.approval.dto.PaperEncryptedRequest;
import com.exam.approval.entity.ExamPaper;
import com.exam.approval.entity.PaperDecryptRecord;
import com.exam.approval.entity.User;
import com.exam.approval.security.util.RSAUtil;
import com.exam.approval.service.ExamPaperService;
import com.exam.approval.service.PaperCryptoService;
import com.exam.approval.service.PaperDecryptRecordService;
import com.exam.approval.service.UserService;
import com.exam.approval.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 试卷管理Controller
 *
 * 功能：
 * 1. 教师创建试卷并提交审批
 * 2. 教师查看自己的试卷列表
 * 3. 管理员查看所有试卷
 * 4. 试卷详情查询
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Slf4j
@Api(tags = "试卷管理接口")
@RestController
@RequestMapping("/exam-paper")
@RequiredArgsConstructor
public class ExamPaperController {

    private final ExamPaperService examPaperService;
    private final PaperCryptoService paperCryptoService;
    private final UserService userService;
    private final PaperDecryptRecordService decryptRecordService;

    @Autowired(required = false)
    private BlockchainIntegrationService blockchainService;

    /**
     * 创建试卷（教师）- 支持加密数据
     */
    @ApiOperation("创建试卷")
    @PostMapping("/create")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Long> createPaper(@RequestBody PaperEncryptedRequest encryptedRequest) {
        try {
            // 1. 解密试卷数据
            ExamPaper paper = paperCryptoService.decryptPaperRequest(encryptedRequest);
            log.info("试卷数据解密成功 - 课程: {}", paper.getCourseName());

            // 2. 获取当前登录用户
            Long teacherId = SecurityUtil.getCurrentUserId();
            String teacherName = SecurityUtil.getCurrentUsername();

            if (teacherId == null) {
                return Result.error("未获取到用户信息");
            }

            // 3. 设置创建者信息
            paper.setCreatorId(teacherId);
            paper.setCreatorName(teacherName);
            paper.setStatus("draft");  // 初始状态：草稿

            // 获取用户部门信息并设置到试卷（用于部门隔离）
            User creator = userService.getUserById(teacherId);
            if (creator != null) {
                paper.setDepartment(creator.getDepartment());
            }

            // 4. 字段映射：前端非持久化字段 -> 数据库持久化字段
            // title = 课程名称 + 考试类型 (例如: "数据结构 期末考试")
            String title = (paper.getCourseName() != null ? paper.getCourseName() : "") +
                           (paper.getExamType() != null ? " " + paper.getExamType() : "");
            paper.setTitle(title.trim());

            // subject = 课程名称
            paper.setSubject(paper.getCourseName());

            // grade = 学期
            paper.setGrade(paper.getSemester());

            // 5. 计算试卷内容哈希（用于区块链存证）
            if (paper.getContent() != null && !paper.getContent().isEmpty()) {
                String contentHash = HashUtil.sha256(paper.getContent());
                paper.setContentHash(contentHash);
                log.info("计算试卷内容哈希: {}", contentHash);
            }

            // 6. 保存试卷（content字段会被MybatisCryptoInterceptor自动加密）
            Long paperId = examPaperService.createPaper(paper);

            // 7. 将试卷哈希上链（区块链存证）
            if (blockchainService != null && paper.getContentHash() != null) {
                try {
                    Long txId = blockchainService.submitEncryptedPaperToChain(
                            paperId, paper.getContent(), paper.getContentHash(),
                            teacherId, teacherName, paper.getTitle());
                    log.info("试卷已上链 - 交易ID: {}, 试卷ID: {}, 哈希: {}",
                            txId, paperId, paper.getContentHash());
                    // 更新试卷的区块链交易ID
                    paper.setId(paperId);
                    paper.setBlockchainTxId(txId);
                    paper.setChainTime(java.time.LocalDateTime.now());
                    examPaperService.updatePaper(paper);
                } catch (Exception e) {
                    log.warn("试卷上链失败（不影响试卷创建）: {}", e.getMessage());
                }
            }

            log.info("教师创建加密试卷成功: teacherId={}, paperId={}, title={}",
                    teacherId, paperId, paper.getTitle());

            return Result.success("试卷创建成功", paperId);

        } catch (Exception e) {
            log.error("创建试卷失败", e);
            return Result.error("创建试卷失败: " + e.getMessage());
        }
    }

    /**
     * 提交审批（教师）
     */
    @ApiOperation("提交审批")
    @PostMapping("/submit/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> submitForApproval(@PathVariable Long id) {
        Long teacherId = SecurityUtil.getCurrentUserId();

        // 验证试卷所有权
        ExamPaper paper = examPaperService.getById(id);
        if (paper == null) {
            return Result.error("试卷不存在");
        }

        if (!paper.getCreatorId().equals(teacherId)) {
            return Result.error("无权操作此试卷");
        }

        if (!"draft".equals(paper.getStatus())) {
            return Result.error("只有草稿状态的试卷才能提交审批");
        }

        examPaperService.submitForApproval(id);

        log.info("教师提交试卷审批: teacherId={}, paperId={}", teacherId, id);

        return Result.success("提交成功，等待系审批");
    }

    /**
     * 查询我的试卷列表（教师）
     */
    @ApiOperation("查询我的试卷")
    @GetMapping("/my-papers")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<List<ExamPaper>> getMyPapers(
            @RequestParam(required = false) String status) {
        Long teacherId = SecurityUtil.getCurrentUserId();

        List<ExamPaper> papers = examPaperService.getByTeacherId(teacherId, status);

        log.debug("教师查询试卷列表: teacherId={}, status={}, count={}",
                teacherId, status, papers.size());

        return Result.success(papers);
    }

    /**
     * 查询所有试卷（管理员）
     * 系管理员只能看本部门试卷，院管理员可以看所有部门
     */
    @ApiOperation("查询所有试卷")
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('DEPT_ADMIN', 'COLLEGE_ADMIN')")
    public Result<List<ExamPaper>> getAllPapers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department) {

        String userRole = SecurityUtil.getCurrentUserRole();
        Long userId = SecurityUtil.getCurrentUserId();

        // 系管理员：强制只能看本部门试卷
        if ("dept_admin".equals(userRole)) {
            User currentUser = userService.getUserById(userId);
            String userDepartment = currentUser != null ? currentUser.getDepartment() : null;

            List<ExamPaper> papers = examPaperService.getAll(status, userDepartment);
            log.debug("系管理员查询试卷列表: userId={}, department={}, status={}, count={}",
                    userId, userDepartment, status, papers.size());
            return Result.success(papers);
        }

        // 院管理员：可以看所有部门
        List<ExamPaper> papers = examPaperService.getAll(status, department);
        log.debug("院管理员查询试卷列表: status={}, department={}, count={}",
                status, department, papers.size());

        return Result.success(papers);
    }

    /**
     * 查询试卷详情
     */
    @ApiOperation("查询试卷详情")
    @GetMapping("/{id}")
    public Result<ExamPaper> getPaperDetail(@PathVariable Long id) {
        ExamPaper paper = examPaperService.getById(id);

        if (paper == null) {
            return Result.error("试卷不存在");
        }

        // 权限验证：只有试卷创建者或管理员可以查看
        Long currentUserId = SecurityUtil.getCurrentUserId();
        String currentRole = SecurityUtil.getCurrentUserRole();

        log.info("权限检查 - paperId: {}, currentUserId: {}, currentRole: {}, creatorId: {}",
                 id, currentUserId, currentRole, paper.getCreatorId());

        boolean isOwner = paper.getCreatorId().equals(currentUserId);
        boolean isAdmin = "dept_admin".equals(currentRole) || "college_admin".equals(currentRole);

        log.info("权限检查结果 - isOwner: {}, isAdmin: {}", isOwner, isAdmin);

        if (!isOwner && !isAdmin) {
            log.warn("权限不足 - userId: {}, role: {}", currentUserId, currentRole);
            return Result.error("无权查看此试卷");
        }

        // 字段映射：将持久化字段映射到前端期望的非持久化字段
        paper.setCourseName(paper.getSubject());
        if (paper.getTitle() != null && paper.getTitle().contains(" ")) {
            String[] parts = paper.getTitle().split(" ", 2);
            if (parts.length > 1) {
                paper.setExamType(parts[1]);
            }
        }
        paper.setSemester(paper.getGrade());
        paper.setTeacherName(paper.getCreatorName());
        paper.setTeacherId(paper.getCreatorId());

        log.debug("查询试卷详情: paperId={}, userId={}", id, currentUserId);

        return Result.success(paper);
    }

    /**
     * 更新试卷（仅草稿状态可编辑）
     */
    @ApiOperation("更新试卷")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> updatePaper(@PathVariable Long id, @RequestBody ExamPaper paper) {
        Long teacherId = SecurityUtil.getCurrentUserId();

        // 验证试卷所有权
        ExamPaper existingPaper = examPaperService.getById(id);
        if (existingPaper == null) {
            return Result.error("试卷不存在");
        }

        if (!existingPaper.getCreatorId().equals(teacherId)) {
            return Result.error("无权操作此试卷");
        }

        if (!"draft".equals(existingPaper.getStatus())) {
            return Result.error("只有草稿状态的试卷才能编辑");
        }

        paper.setId(id);

        // 字段映射：前端非持久化字段 -> 数据库持久化字段
        String title = (paper.getCourseName() != null ? paper.getCourseName() : "") +
                       (paper.getExamType() != null ? " " + paper.getExamType() : "");
        paper.setTitle(title.trim());
        paper.setSubject(paper.getCourseName());
        paper.setGrade(paper.getSemester());

        examPaperService.updatePaper(paper);

        log.info("教师更新试卷: teacherId={}, paperId={}", teacherId, id);

        return Result.success("更新成功");
    }

    /**
     * 删除试卷（仅草稿或驳回状态可删除）
     */
    @ApiOperation("删除试卷")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> deletePaper(@PathVariable Long id) {
        Long teacherId = SecurityUtil.getCurrentUserId();

        // 验证试卷所有权和状态
        ExamPaper paper = examPaperService.getById(id);
        if (paper == null) {
            return Result.error("试卷不存在");
        }

        if (!paper.getCreatorId().equals(teacherId)) {
            return Result.error("无权操作此试卷");
        }

        String status = paper.getStatus();
        if (!"draft".equals(status) && !"rejected".equals(status)) {
            return Result.error("只有草稿或已驳回的试卷才能删除");
        }

        examPaperService.deletePaper(id);

        log.info("教师删除试卷: teacherId={}, paperId={}", teacherId, id);

        return Result.success("删除成功");
    }

    /**
     * 统计信息（所有角色）
     * 教师：统计自己的试卷
     * 管理员：统计全局试卷
     */
    @ApiOperation("统计试卷数量")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('TEACHER', 'DEPT_ADMIN', 'COLLEGE_ADMIN')")
    public Result<Map<String, Long>> getStatistics() {
        String role = SecurityUtil.getCurrentUserRole();
        Long userId = SecurityUtil.getCurrentUserId();

        Map<String, Long> statistics = examPaperService.getStatistics(role, userId);

        return Result.success(statistics);
    }

    /**
     * 获取试卷加密信息（不含内容）
     * 用于前端判断试卷是否需要解密
     */
    @ApiOperation("获取试卷加密信息")
    @GetMapping("/{id}/crypto-info")
    @PreAuthorize("hasAnyRole('TEACHER', 'DEPT_ADMIN', 'COLLEGE_ADMIN')")
    public Result<Map<String, Object>> getPaperCryptoInfo(@PathVariable Long id) {
        ExamPaper paper = examPaperService.getById(id);
        if (paper == null) {
            return Result.error("试卷不存在");
        }

        // 权限验证
        Long currentUserId = SecurityUtil.getCurrentUserId();
        String currentRole = SecurityUtil.getCurrentUserRole();
        boolean isOwner = paper.getCreatorId().equals(currentUserId);
        boolean isAdmin = "dept_admin".equals(currentRole) || "college_admin".equals(currentRole);

        if (!isOwner && !isAdmin) {
            return Result.error("无权查看此试卷");
        }

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("paperId", id);
        data.put("title", paper.getTitle());
        data.put("isPkiEncrypted", paper.getEncryptedAesKey() != null && !paper.getEncryptedAesKey().isEmpty());
        data.put("contentHash", paper.getContentHash());
        data.put("blockchainTxId", paper.getBlockchainTxId());
        data.put("chainTime", paper.getChainTime());
        data.put("creatorId", paper.getCreatorId());
        data.put("creatorName", paper.getCreatorName());

        return Result.success(data);
    }

    /**
     * 查询试卷的解密记录
     */
    @ApiOperation("查询试卷解密记录")
    @GetMapping("/{paperId}/decrypt-records")
    @PreAuthorize("hasAnyRole('TEACHER', 'DEPT_ADMIN', 'COLLEGE_ADMIN')")
    public Result<List<PaperDecryptRecord>> getDecryptRecords(@PathVariable Long paperId) {
        // 查询试卷
        ExamPaper paper = examPaperService.getById(paperId);
        if (paper == null) {
            return Result.error("试卷不存在");
        }

        // 权限验证：只有试卷创建者或管理员可以查看解密记录
        Long currentUserId = SecurityUtil.getCurrentUserId();
        String currentRole = SecurityUtil.getCurrentUserRole();
        boolean isOwner = paper.getCreatorId().equals(currentUserId);
        boolean isAdmin = "dept_admin".equals(currentRole) || "college_admin".equals(currentRole);

        if (!isOwner && !isAdmin) {
            return Result.error("无权查看此试卷的解密记录");
        }

        List<PaperDecryptRecord> records = decryptRecordService.findByPaperId(paperId);
        return Result.success(records);
    }

    /**
     * 解密试卷（带审计，多方加密版本）
     * 使用用户私钥解密，并记录解密操作到区块链
     */
    @ApiOperation("解密试卷（带审计上链）")
    @PostMapping("/decrypt-with-audit")
    @PreAuthorize("hasAnyRole('TEACHER', 'DEPT_ADMIN', 'COLLEGE_ADMIN')")
    public Result<Map<String, Object>> decryptWithAudit(
            @RequestBody PaperDecryptRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long paperId = request.getPaperId();
            String privateKey = request.getPrivateKey();

            if (paperId == null || privateKey == null || privateKey.isEmpty()) {
                return Result.error("试卷ID和私钥不能为空");
            }

            // 查询试卷
            ExamPaper paper = examPaperService.getById(paperId);
            if (paper == null) {
                return Result.error("试卷不存在");
            }

            // 获取当前用户信息
            Long currentUserId = SecurityUtil.getCurrentUserId();
            String currentRole = SecurityUtil.getCurrentUserRole();
            User currentUser = userService.getUserById(currentUserId);

            if (currentUser == null) {
                return Result.error("用户信息不存在");
            }

            // 检查用户是否有权解密（在多方加密密钥列表中）
            String userEncryptedKey = paperCryptoService.findUserEncryptedKey(
                    paper.getEncryptedAesKeys(), currentUserId);
            boolean isOwner = paper.getCreatorId().equals(currentUserId);

            if (userEncryptedKey == null && !isOwner) {
                return Result.error("您没有权限解密此试卷");
            }

            // 使用多方加密解密
            String decryptedContent = paperCryptoService.decryptPaperWithMultiPartyKey(
                    paper, privateKey, currentUserId);

            // 生成解密签名
            long timestamp = System.currentTimeMillis();
            String signData = paperCryptoService.buildDecryptSignData(paperId, currentUserId, timestamp);
            String signature = RSAUtil.sign(signData, privateKey);

            // 获取客户端IP
            String ipAddress = getClientIp(httpRequest);

            // 创建解密记录
            PaperDecryptRecord record = new PaperDecryptRecord();
            record.setPaperId(paperId);
            record.setUserId(currentUserId);
            record.setUserName(currentUser.getRealName());
            record.setUserRole(currentRole);
            record.setDecryptTime(LocalDateTime.now());
            record.setDecryptTimeMs(timestamp);  // 保存原始毫秒时间戳用于签名验证
            record.setIpAddress(ipAddress);
            record.setSignature(signature);
            decryptRecordService.saveRecord(record);

            // 解密记录上链
            if (blockchainService != null) {
                try {
                    Long txId = blockchainService.recordDecryptAction(
                            paperId, currentUserId, currentUser.getRealName(),
                            currentRole, paper.getContentHash(), signature);
                    if (txId != null) {
                        decryptRecordService.updateBlockchainTxId(record.getId(), txId);
                        log.info("解密记录上链成功 - 交易ID: {}", txId);
                    }
                } catch (Exception e) {
                    log.warn("解密记录上链失败（不影响解密）: {}", e.getMessage());
                }
            }

            Map<String, Object> data = new java.util.HashMap<>();
            data.put("paperId", paperId);
            data.put("content", decryptedContent);
            data.put("contentHash", paper.getContentHash());
            data.put("verified", true);
            data.put("recordId", record.getId());

            log.info("试卷解密成功（带审计）- 用户: {} ({}), 试卷ID: {}",
                    currentUser.getRealName(), currentRole, paperId);
            return Result.success("解密成功，操作已记录到区块链", data);

        } catch (Exception e) {
            log.error("解密试卷失败", e);
            return Result.error("解密失败: " + e.getMessage());
        }
    }

    /**
     * 验证解密签名
     */
    @ApiOperation("验证解密签名")
    @PostMapping("/verify-decrypt-signature")
    public Result<Map<String, Object>> verifyDecryptSignature(@RequestBody Map<String, Long> request) {
        Long recordId = request.get("recordId");
        if (recordId == null) {
            return Result.error("记录ID不能为空");
        }

        PaperDecryptRecord record = decryptRecordService.getById(recordId);
        if (record == null) {
            return Result.error("解密记录不存在");
        }

        User user = userService.getUserById(record.getUserId());
        if (user == null || user.getRsaPublicKey() == null) {
            return Result.error("用户公钥不存在，无法验证签名");
        }

        boolean valid = paperCryptoService.verifyDecryptSignature(record, user.getRsaPublicKey());

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("recordId", recordId);
        data.put("valid", valid);
        data.put("message", valid ? "签名验证通过，确认为本人操作" : "签名验证失败，可能被篡改");
        data.put("userName", record.getUserName());
        data.put("decryptTime", record.getDecryptTime());

        return Result.success(data);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多个代理，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
