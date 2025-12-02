package com.exam.approval.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blockchain.exam.blockchain.entity.Transaction;
import com.blockchain.exam.blockchain.mapper.TransactionMapper;
import com.blockchain.exam.blockchain.service.BlockchainService;
import com.exam.approval.common.result.Result;
import com.exam.approval.entity.ExamPaper;
import com.exam.approval.entity.PaperDecryptRecord;
import com.exam.approval.entity.User;
import com.exam.approval.service.ExamPaperService;
import com.exam.approval.service.PaperCryptoService;
import com.exam.approval.service.PaperDecryptRecordService;
import com.exam.approval.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 区块链账本控制器
 *
 * 提供透明的区块链账本查询API，用于：
 * - 查看所有解密记录（审计追溯）
 * - 验证签名真实性
 * - 查看试卷完整生命周期
 * - 统计区块链操作信息
 *
 * 所有用户都可以访问此API，体现区块链的透明性原则
 *
 * @author 网络信息安全大作业
 * @date 2025-11-29
 */
@Slf4j
@RestController
@RequestMapping("/blockchain/ledger")
@RequiredArgsConstructor
public class BlockchainLedgerController {

    private final PaperDecryptRecordService decryptRecordService;
    private final PaperCryptoService cryptoService;
    private final ExamPaperService examPaperService;
    private final UserService userService;
    private final TransactionMapper transactionMapper;
    private final BlockchainService blockchainService;
    private final ObjectMapper objectMapper;

    /**
     * 获取账本统计信息
     *
     * 返回解密记录总数、区块链交易统计等
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // 解密记录统计
            Long decryptRecordCount = decryptRecordService.countAll();
            stats.put("decryptRecordCount", decryptRecordCount);

            // 已上链的解密记录数
            long chainedCount = decryptRecordService.list(
                    new LambdaQueryWrapper<PaperDecryptRecord>()
                            .isNotNull(PaperDecryptRecord::getBlockchainTxId)
            ).size();
            stats.put("chainedDecryptCount", chainedCount);

            // 区块链统计
            Map<String, Object> chainStats = blockchainService.getChainStats();
            stats.put("blockchainStats", chainStats);

            // 交易池统计
            Long pendingTxCount = transactionMapper.selectCount(
                    new LambdaQueryWrapper<Transaction>()
                            .eq(Transaction::getStatus, "PENDING")
            );
            Long minedTxCount = transactionMapper.selectCount(
                    new LambdaQueryWrapper<Transaction>()
                            .eq(Transaction::getStatus, "MINED")
            );
            stats.put("pendingTransactions", pendingTxCount);
            stats.put("minedTransactions", minedTxCount);

            // 按类型统计交易
            Map<String, Long> txByType = new HashMap<>();
            List<Transaction> allTx = transactionMapper.selectList(null);
            for (Transaction tx : allTx) {
                String type = tx.getTransactionType();
                txByType.put(type, txByType.getOrDefault(type, 0L) + 1);
            }
            stats.put("transactionsByType", txByType);

            // 按操作类型统计（用于审计账本统计卡片）
            Long createRecordCount = txByType.getOrDefault("PAPER_CONTENT", 0L);
            Long approvalRecordCount = txByType.getOrDefault("APPROVAL_RECORD", 0L);
            stats.put("createRecordCount", createRecordCount);
            stats.put("approvalRecordCount", approvalRecordCount);
            stats.put("totalOperations", createRecordCount + approvalRecordCount + decryptRecordCount);

            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取账本统计失败", e);
            return Result.error("获取账本统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取统一审计日志
     *
     * 返回所有操作类型（创建/审批/解密）的统一格式日志
     * 所有用户都可以查看，体现区块链透明性原则
     *
     * @param operationType 操作类型过滤（CREATE/APPROVE/DECRYPT/ALL，默认ALL）
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 统一格式的审计日志列表
     */
    @GetMapping("/audit-logs")
    public Result<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "ALL") String operationType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<Map<String, Object>> allLogs = new ArrayList<>();

            // 1. 获取区块链交易记录（CREATE和APPROVE）
            if ("ALL".equals(operationType) || "CREATE".equals(operationType) || "APPROVE".equals(operationType)) {
                LambdaQueryWrapper<Transaction> txWrapper = new LambdaQueryWrapper<>();

                if ("CREATE".equals(operationType)) {
                    txWrapper.eq(Transaction::getTransactionType, "PAPER_CONTENT");
                } else if ("APPROVE".equals(operationType)) {
                    txWrapper.eq(Transaction::getTransactionType, "APPROVAL_RECORD");
                } else {
                    // ALL - 获取创建和审批记录
                    txWrapper.in(Transaction::getTransactionType, "PAPER_CONTENT", "APPROVAL_RECORD");
                }

                List<Transaction> transactions = transactionMapper.selectList(txWrapper);

                for (Transaction tx : transactions) {
                    Map<String, Object> logEntry = new HashMap<>();
                    logEntry.put("id", "TX_" + tx.getId());
                    logEntry.put("blockchainTxId", tx.getId());
                    logEntry.put("isChained", tx.getBlockIndex() != null);
                    logEntry.put("operationTime", tx.getCreateTime());
                    logEntry.put("transactionType", tx.getTransactionType());

                    // 解析交易数据
                    try {
                        JsonNode dataNode = objectMapper.readTree(tx.getTransactionData());
                        // 获取嵌套的 data 子对象（BlockData 结构中业务数据存储在 data 字段）
                        JsonNode dataSubNode = dataNode.has("data") ? dataNode.get("data") : dataNode;

                        // 获取paperId - 优先从 referenceId 获取，其次从 data.paperId 获取
                        Long paperId = null;
                        if (dataNode.has("referenceId") && !dataNode.get("referenceId").isNull()) {
                            paperId = dataNode.get("referenceId").asLong();
                        } else if (dataSubNode.has("paperId") && !dataSubNode.get("paperId").isNull()) {
                            paperId = dataSubNode.get("paperId").asLong();
                        }
                        logEntry.put("paperId", paperId);

                        // 获取试卷标题
                        if (paperId != null) {
                            ExamPaper paper = examPaperService.getById(paperId);
                            if (paper != null) {
                                logEntry.put("paperTitle", paper.getTitle());
                            }
                        }

                        if ("PAPER_CONTENT".equals(tx.getTransactionType())) {
                            // 创建记录
                            logEntry.put("operationType", "CREATE");
                            logEntry.put("operationLabel", "创建试卷");

                            // creatorId 在 data 子对象中
                            Long creatorId = dataSubNode.has("creatorId") ? dataSubNode.get("creatorId").asLong() : null;
                            logEntry.put("operatorId", creatorId);

                            if (creatorId != null) {
                                User creator = userService.getById(creatorId);
                                if (creator != null) {
                                    logEntry.put("operatorName", creator.getRealName());
                                    logEntry.put("operatorRole", creator.getRole());
                                }
                            }
                        } else if ("APPROVAL_RECORD".equals(tx.getTransactionType())) {
                            // 审批记录 - 数据在 data 子对象中
                            String actionType = dataSubNode.has("actionType") ? dataSubNode.get("actionType").asText() :
                                               (dataSubNode.has("action") ? dataSubNode.get("action").asText() : "APPROVE");
                            logEntry.put("operationType", "APPROVE");

                            if ("APPROVE".equalsIgnoreCase(actionType) || "approve".equals(actionType)) {
                                logEntry.put("operationLabel", "审批通过");
                            } else if ("REJECT".equalsIgnoreCase(actionType) || "reject".equals(actionType)) {
                                logEntry.put("operationLabel", "审批拒绝");
                                logEntry.put("operationType", "REJECT");
                            } else {
                                logEntry.put("operationLabel", "审批操作");
                            }

                            Long approverId = dataSubNode.has("approverId") ? dataSubNode.get("approverId").asLong() : null;
                            logEntry.put("operatorId", approverId);

                            String approverRole = dataSubNode.has("approverRole") ? dataSubNode.get("approverRole").asText() : null;

                            if (approverId != null) {
                                User approver = userService.getById(approverId);
                                if (approver != null) {
                                    logEntry.put("operatorName", approver.getRealName());
                                    // 如果链上没有角色信息，从用户表获取
                                    if (approverRole == null || approverRole.isEmpty() || "null".equals(approverRole)) {
                                        approverRole = approver.getRole();
                                    }
                                }
                            }
                            logEntry.put("operatorRole", approverRole);
                        }
                    } catch (Exception e) {
                        log.debug("解析交易数据失败: {}", e.getMessage());
                        logEntry.put("operationType", tx.getTransactionType());
                        logEntry.put("operationLabel", tx.getTransactionType());
                    }

                    // 创建和审批记录没有签名和IP
                    logEntry.put("signature", null);
                    logEntry.put("ipAddress", null);

                    allLogs.add(logEntry);
                }
            }

            // 2. 获取解密记录
            if ("ALL".equals(operationType) || "DECRYPT".equals(operationType)) {
                List<PaperDecryptRecord> decryptRecords = decryptRecordService.list();

                for (PaperDecryptRecord record : decryptRecords) {
                    Map<String, Object> logEntry = new HashMap<>();
                    logEntry.put("id", "DECRYPT_" + record.getId());
                    logEntry.put("operationType", "DECRYPT");
                    logEntry.put("operationLabel", "解密查看");
                    logEntry.put("paperId", record.getPaperId());
                    logEntry.put("operatorId", record.getUserId());
                    logEntry.put("operatorName", record.getUserName());
                    logEntry.put("operatorRole", record.getUserRole());
                    logEntry.put("operationTime", record.getDecryptTime());
                    logEntry.put("ipAddress", record.getIpAddress());
                    logEntry.put("signature", record.getSignature());
                    logEntry.put("blockchainTxId", record.getBlockchainTxId());
                    logEntry.put("isChained", record.getBlockchainTxId() != null);
                    logEntry.put("transactionType", "DECRYPT_RECORD");
                    logEntry.put("decryptRecordId", record.getId());

                    // 获取试卷标题
                    ExamPaper paper = examPaperService.getById(record.getPaperId());
                    if (paper != null) {
                        logEntry.put("paperTitle", paper.getTitle());
                    }

                    allLogs.add(logEntry);
                }
            }

            // 3. 按操作时间倒序排序
            allLogs.sort((a, b) -> {
                Object timeA = a.get("operationTime");
                Object timeB = b.get("operationTime");
                if (timeA == null && timeB == null) return 0;
                if (timeA == null) return 1;
                if (timeB == null) return -1;
                return timeB.toString().compareTo(timeA.toString());
            });

            // 4. 分页
            int total = allLogs.size();
            int start = (page - 1) * size;
            int end = Math.min(start + size, total);
            List<Map<String, Object>> pagedLogs = start < total ? allLogs.subList(start, end) : new ArrayList<>();

            Map<String, Object> result = new HashMap<>();
            result.put("logs", pagedLogs);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (total + size - 1) / size);

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取审计日志失败", e);
            return Result.error("获取审计日志失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询解密记录
     *
     * 所有用户都可以查看所有解密记录，体现区块链透明性
     *
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 解密记录列表
     */
    @GetMapping("/records")
    public Result<Map<String, Object>> getRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<PaperDecryptRecord> records = decryptRecordService.findAllPaged(page, size);
            Long total = decryptRecordService.countAll();

            // 丰富记录信息（添加试卷标题等）
            List<Map<String, Object>> enrichedRecords = new ArrayList<>();
            for (PaperDecryptRecord record : records) {
                Map<String, Object> enriched = new HashMap<>();
                enriched.put("id", record.getId());
                enriched.put("paperId", record.getPaperId());
                enriched.put("userId", record.getUserId());
                enriched.put("userName", record.getUserName());
                enriched.put("userRole", record.getUserRole());
                enriched.put("decryptTime", record.getDecryptTime());
                enriched.put("ipAddress", record.getIpAddress());
                enriched.put("signature", record.getSignature());
                enriched.put("blockchainTxId", record.getBlockchainTxId());
                enriched.put("chainTime", record.getChainTime());

                // 获取试卷标题
                ExamPaper paper = examPaperService.getById(record.getPaperId());
                if (paper != null) {
                    enriched.put("paperTitle", paper.getTitle());
                    enriched.put("paperDepartment", paper.getDepartment());
                }

                // 验证状态
                enriched.put("isChained", record.getBlockchainTxId() != null);

                enrichedRecords.add(enriched);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("records", enrichedRecords);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (total + size - 1) / size);

            return Result.success(result);
        } catch (Exception e) {
            log.error("查询解密记录失败", e);
            return Result.error("查询解密记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取解密记录详情
     *
     * @param recordId 解密记录ID
     * @return 解密记录详情
     */
    @GetMapping("/records/{recordId}")
    public Result<Map<String, Object>> getRecordDetail(@PathVariable Long recordId) {
        try {
            PaperDecryptRecord record = decryptRecordService.getById(recordId);
            if (record == null) {
                return Result.notFound("解密记录不存在");
            }

            Map<String, Object> detail = new HashMap<>();
            detail.put("record", record);

            // 获取试卷信息
            ExamPaper paper = examPaperService.getById(record.getPaperId());
            if (paper != null) {
                Map<String, Object> paperInfo = new HashMap<>();
                paperInfo.put("id", paper.getId());
                paperInfo.put("title", paper.getTitle());
                paperInfo.put("department", paper.getDepartment());
                paperInfo.put("courseName", paper.getCourseName());
                paperInfo.put("createTime", paper.getCreateTime());
                detail.put("paper", paperInfo);
            }

            // 获取用户信息
            User user = userService.getById(record.getUserId());
            if (user != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("realName", user.getRealName());
                userInfo.put("role", user.getRole());
                userInfo.put("department", user.getDepartment());
                detail.put("user", userInfo);
            }

            // 获取区块链交易信息
            if (record.getBlockchainTxId() != null) {
                Transaction tx = transactionMapper.selectById(record.getBlockchainTxId());
                if (tx != null) {
                    Map<String, Object> txInfo = new HashMap<>();
                    txInfo.put("id", tx.getId());
                    txInfo.put("transactionType", tx.getTransactionType());
                    txInfo.put("status", tx.getStatus());
                    txInfo.put("blockIndex", tx.getBlockIndex());
                    txInfo.put("creatorNode", tx.getCreatorNode());
                    txInfo.put("createTime", tx.getCreateTime());
                    detail.put("blockchainTransaction", txInfo);
                }
            }

            // 签名验证状态
            if (record.getSignature() != null && user != null && user.getRsaPublicKey() != null) {
                boolean signatureValid = cryptoService.verifyDecryptSignature(record, user.getRsaPublicKey());
                detail.put("signatureValid", signatureValid);
            } else {
                detail.put("signatureValid", null);
                detail.put("signatureValidMessage", "无法验证（缺少签名或公钥）");
            }

            return Result.success(detail);
        } catch (Exception e) {
            log.error("获取解密记录详情失败 - recordId: {}", recordId, e);
            return Result.error("获取解密记录详情失败: " + e.getMessage());
        }
    }

    /**
     * 验证解密签名
     *
     * 任何用户都可以验证签名，体现区块链的可验证性
     *
     * @param request 包含recordId的请求
     * @return 验证结果
     */
    @PostMapping("/verify-signature")
    public Result<Map<String, Object>> verifySignature(@RequestBody Map<String, Long> request) {
        try {
            Long recordId = request.get("recordId");
            if (recordId == null) {
                return Result.error("请提供记录ID");
            }

            PaperDecryptRecord record = decryptRecordService.getById(recordId);
            if (record == null) {
                return Result.notFound("解密记录不存在");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("recordId", recordId);
            result.put("paperId", record.getPaperId());
            result.put("userId", record.getUserId());
            result.put("userName", record.getUserName());
            result.put("decryptTime", record.getDecryptTime());

            if (record.getSignature() == null) {
                result.put("valid", false);
                result.put("message", "该记录没有签名");
                return Result.success(result);
            }

            User user = userService.getById(record.getUserId());
            if (user == null || user.getRsaPublicKey() == null) {
                result.put("valid", false);
                result.put("message", "无法获取用户公钥");
                return Result.success(result);
            }

            boolean isValid = cryptoService.verifyDecryptSignature(record, user.getRsaPublicKey());
            result.put("valid", isValid);
            result.put("message", isValid ? "签名验证通过" : "签名验证失败");
            result.put("signature", record.getSignature().substring(0, Math.min(64, record.getSignature().length())) + "...");
            result.put("publicKeyFingerprint", user.getRsaPublicKey().substring(0, 50) + "...");

            return Result.success(result);
        } catch (Exception e) {
            log.error("验证签名失败", e);
            return Result.error("验证签名失败: " + e.getMessage());
        }
    }

    /**
     * 获取试卷完整生命周期
     *
     * 返回试卷从创建到审批到解密的完整记录
     *
     * @param paperId 试卷ID
     * @return 生命周期记录
     */
    @GetMapping("/paper-lifecycle/{paperId}")
    public Result<Map<String, Object>> getPaperLifecycle(@PathVariable Long paperId) {
        try {
            ExamPaper paper = examPaperService.getById(paperId);
            if (paper == null) {
                return Result.notFound("试卷不存在");
            }

            Map<String, Object> lifecycle = new HashMap<>();

            // 1. 试卷基本信息
            Map<String, Object> paperInfo = new HashMap<>();
            paperInfo.put("id", paper.getId());
            paperInfo.put("title", paper.getTitle());
            paperInfo.put("department", paper.getDepartment());
            paperInfo.put("courseName", paper.getCourseName());
            paperInfo.put("status", paper.getStatus());
            paperInfo.put("createTime", paper.getCreateTime());
            paperInfo.put("creatorId", paper.getCreatorId());
            paperInfo.put("contentHash", paper.getContentHash());
            lifecycle.put("paper", paperInfo);

            // 2. 获取创建者信息
            User creator = userService.getById(paper.getCreatorId());
            if (creator != null) {
                Map<String, Object> creatorInfo = new HashMap<>();
                creatorInfo.put("id", creator.getId());
                creatorInfo.put("realName", creator.getRealName());
                creatorInfo.put("role", creator.getRole());
                creatorInfo.put("department", creator.getDepartment());
                lifecycle.put("creator", creatorInfo);
            }

            // 3. 查询相关的区块链交易（审批记录、试卷哈希等）
            List<Map<String, Object>> blockchainEvents = new ArrayList<>();

            List<Transaction> relatedTx = transactionMapper.selectList(
                    new LambdaQueryWrapper<Transaction>()
                            .like(Transaction::getTransactionData, "\"paperId\":" + paperId)
                            .or()
                            .like(Transaction::getTransactionData, "\"paperId\": " + paperId)
                            .orderByAsc(Transaction::getCreateTime)
            );

            for (Transaction tx : relatedTx) {
                Map<String, Object> event = new HashMap<>();
                event.put("txId", tx.getId());
                event.put("type", tx.getTransactionType());
                event.put("status", tx.getStatus());
                event.put("blockIndex", tx.getBlockIndex());
                event.put("createTime", tx.getCreateTime());
                event.put("creatorNode", tx.getCreatorNode());

                // 解析交易数据获取更多信息
                try {
                    JsonNode dataNode = objectMapper.readTree(tx.getTransactionData());
                    if (dataNode.has("actionType")) {
                        event.put("actionType", dataNode.get("actionType").asText());
                    }
                    if (dataNode.has("operatorId")) {
                        event.put("operatorId", dataNode.get("operatorId").asLong());
                    }
                    if (dataNode.has("approverRole")) {
                        event.put("approverRole", dataNode.get("approverRole").asText());
                    }
                } catch (Exception e) {
                    log.debug("解析交易数据失败: {}", e.getMessage());
                }

                blockchainEvents.add(event);
            }
            lifecycle.put("blockchainEvents", blockchainEvents);

            // 4. 获取解密记录
            List<PaperDecryptRecord> decryptRecords = decryptRecordService.findByPaperId(paperId);
            List<Map<String, Object>> decryptEvents = new ArrayList<>();
            for (PaperDecryptRecord record : decryptRecords) {
                Map<String, Object> event = new HashMap<>();
                event.put("recordId", record.getId());
                event.put("userId", record.getUserId());
                event.put("userName", record.getUserName());
                event.put("userRole", record.getUserRole());
                event.put("decryptTime", record.getDecryptTime());
                event.put("ipAddress", record.getIpAddress());
                event.put("isChained", record.getBlockchainTxId() != null);
                event.put("blockchainTxId", record.getBlockchainTxId());
                decryptEvents.add(event);
            }
            lifecycle.put("decryptRecords", decryptEvents);

            // 5. 统计摘要
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalBlockchainEvents", blockchainEvents.size());
            summary.put("totalDecryptRecords", decryptRecords.size());
            summary.put("chainedDecryptRecords", decryptRecords.stream()
                    .filter(r -> r.getBlockchainTxId() != null).count());
            lifecycle.put("summary", summary);

            return Result.success(lifecycle);
        } catch (Exception e) {
            log.error("获取试卷生命周期失败 - paperId: {}", paperId, e);
            return Result.error("获取试卷生命周期失败: " + e.getMessage());
        }
    }

    /**
     * 搜索区块链交易
     *
     * 支持按类型、状态搜索
     *
     * @param type 交易类型（可选）
     * @param status 交易状态（可选）
     * @param page 页码
     * @param size 每页数量
     * @return 交易列表
     */
    @GetMapping("/transactions")
    public Result<Map<String, Object>> searchTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            LambdaQueryWrapper<Transaction> wrapper = new LambdaQueryWrapper<>();

            if (type != null && !type.isEmpty()) {
                wrapper.eq(Transaction::getTransactionType, type);
            }
            if (status != null && !status.isEmpty()) {
                wrapper.eq(Transaction::getStatus, status);
            }

            wrapper.orderByDesc(Transaction::getCreateTime);

            // 获取总数
            Long total = transactionMapper.selectCount(wrapper);

            // 分页
            wrapper.last("LIMIT " + ((page - 1) * size) + ", " + size);
            List<Transaction> transactions = transactionMapper.selectList(wrapper);

            Map<String, Object> result = new HashMap<>();
            result.put("transactions", transactions);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (total + size - 1) / size);

            return Result.success(result);
        } catch (Exception e) {
            log.error("搜索交易失败", e);
            return Result.error("搜索交易失败: " + e.getMessage());
        }
    }

    /**
     * 获取交易详情
     *
     * @param txId 交易ID
     * @return 交易详情
     */
    @GetMapping("/transactions/{txId}")
    public Result<Map<String, Object>> getTransactionDetail(@PathVariable Long txId) {
        try {
            Transaction tx = transactionMapper.selectById(txId);
            if (tx == null) {
                return Result.notFound("交易不存在");
            }

            Map<String, Object> detail = new HashMap<>();
            detail.put("id", tx.getId());
            detail.put("transactionType", tx.getTransactionType());
            detail.put("status", tx.getStatus());
            detail.put("blockIndex", tx.getBlockIndex());
            detail.put("creatorNode", tx.getCreatorNode());
            detail.put("createTime", tx.getCreateTime());
            detail.put("updateTime", tx.getUpdateTime());

            // 解析并格式化交易数据
            try {
                JsonNode dataNode = objectMapper.readTree(tx.getTransactionData());
                detail.put("transactionData", dataNode);
            } catch (Exception e) {
                detail.put("transactionDataRaw", tx.getTransactionData());
            }

            return Result.success(detail);
        } catch (Exception e) {
            log.error("获取交易详情失败 - txId: {}", txId, e);
            return Result.error("获取交易详情失败: " + e.getMessage());
        }
    }
}
