package com.blockchain.exam.blockchain.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blockchain.exam.blockchain.entity.Transaction;
import com.blockchain.exam.blockchain.mapper.TransactionMapper;
import com.blockchain.exam.blockchain.service.BlockchainService;
import com.exam.approval.common.result.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 区块链账本控制器（简化版）
 *
 * 为 node2/node3 提供基本的账本查询API
 *
 * @author Claude Code
 * @since 2025-11-29
 */
@Slf4j
@RestController
@RequestMapping("/blockchain/ledger")
@RequiredArgsConstructor
public class BlockchainLedgerController {

    private final TransactionMapper transactionMapper;
    private final BlockchainService blockchainService;
    private final ObjectMapper objectMapper;

    /**
     * 获取账本统计信息（查询所有节点数据）
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // 区块链统计
            Map<String, Object> chainStats = blockchainService.getChainStats();
            stats.put("blockchainStats", chainStats);

            // 从所有节点交易池统计
            List<Transaction> allTx = transactionMapper.selectAllFromAllNodes();

            long pendingTxCount = allTx.stream().filter(tx -> "PENDING".equals(tx.getStatus())).count();
            long minedTxCount = allTx.stream().filter(tx -> "MINED".equals(tx.getStatus())).count();
            stats.put("pendingTransactions", pendingTxCount);
            stats.put("minedTransactions", minedTxCount);

            // 按类型统计交易
            Map<String, Long> txByType = new HashMap<>();
            for (Transaction tx : allTx) {
                String type = tx.getTransactionType();
                txByType.put(type, txByType.getOrDefault(type, 0L) + 1);
            }
            stats.put("transactionsByType", txByType);

            // 按操作类型统计
            Long createRecordCount = txByType.getOrDefault("PAPER_CONTENT", 0L);
            Long approvalRecordCount = txByType.getOrDefault("APPROVAL_RECORD", 0L);
            Long decryptRecordCount = txByType.getOrDefault("DECRYPT_RECORD", 0L);
            stats.put("createRecordCount", createRecordCount);
            stats.put("approvalRecordCount", approvalRecordCount);
            stats.put("decryptRecordCount", decryptRecordCount);
            stats.put("totalOperations", createRecordCount + approvalRecordCount + decryptRecordCount);

            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取账本统计失败", e);
            return Result.error("获取账本统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取统一审计日志（查询所有节点数据）
     */
    @GetMapping("/audit-logs")
    public Result<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "ALL") String operationType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<Map<String, Object>> allLogs = new ArrayList<>();

            // 从所有节点获取区块链交易记录
            List<Transaction> transactions;
            if ("CREATE".equals(operationType)) {
                transactions = transactionMapper.selectByTypeFromAllNodes("PAPER_CONTENT");
            } else if ("APPROVE".equals(operationType)) {
                transactions = transactionMapper.selectByTypeFromAllNodes("APPROVAL_RECORD");
            } else if ("DECRYPT".equals(operationType)) {
                transactions = transactionMapper.selectByTypeFromAllNodes("DECRYPT_RECORD");
            } else {
                // ALL - 获取所有记录
                transactions = transactionMapper.selectAllFromAllNodes();
            }

            // 根据 transaction_data 去重，只保留第一条（避免广播导致的重复记录）
            Set<String> seenData = new HashSet<>();
            transactions = transactions.stream()
                    .filter(tx -> seenData.add(tx.getTransactionData()))
                    .collect(Collectors.toList());

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
                    JsonNode dataSubNode = dataNode.has("data") ? dataNode.get("data") : dataNode;

                    // 获取paperId
                    Long paperId = null;
                    if (dataNode.has("referenceId") && !dataNode.get("referenceId").isNull()) {
                        paperId = dataNode.get("referenceId").asLong();
                    } else if (dataSubNode.has("paperId") && !dataSubNode.get("paperId").isNull()) {
                        paperId = dataSubNode.get("paperId").asLong();
                    }
                    logEntry.put("paperId", paperId);

                    if ("PAPER_CONTENT".equals(tx.getTransactionType())) {
                        logEntry.put("operationType", "CREATE");
                        logEntry.put("operationLabel", "创建试卷");
                        Long creatorId = dataSubNode.has("creatorId") ? dataSubNode.get("creatorId").asLong() : null;
                        logEntry.put("operatorId", creatorId);
                        String creatorName = dataSubNode.has("creatorName") ? dataSubNode.get("creatorName").asText() : null;
                        logEntry.put("operatorName", creatorName);
                        String paperTitle = dataSubNode.has("paperTitle") ? dataSubNode.get("paperTitle").asText() : null;
                        logEntry.put("paperTitle", paperTitle);
                    } else if ("APPROVAL_RECORD".equals(tx.getTransactionType())) {
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
                        logEntry.put("operatorRole", approverRole);
                        String approverName = dataSubNode.has("approverName") ? dataSubNode.get("approverName").asText() : null;
                        logEntry.put("operatorName", approverName);
                        String paperTitle = dataSubNode.has("paperTitle") ? dataSubNode.get("paperTitle").asText() : null;
                        logEntry.put("paperTitle", paperTitle);
                    } else if ("DECRYPT_RECORD".equals(tx.getTransactionType())) {
                        logEntry.put("operationType", "DECRYPT");
                        logEntry.put("operationLabel", "解密查看");
                        Long userId = dataSubNode.has("userId") ? dataSubNode.get("userId").asLong() : null;
                        logEntry.put("operatorId", userId);
                        String userName = dataSubNode.has("userName") ? dataSubNode.get("userName").asText() : null;
                        logEntry.put("operatorName", userName);
                    } else {
                        logEntry.put("operationType", tx.getTransactionType());
                        logEntry.put("operationLabel", tx.getTransactionType());
                    }
                } catch (Exception e) {
                    log.debug("解析交易数据失败: {}", e.getMessage());
                    logEntry.put("operationType", tx.getTransactionType());
                    logEntry.put("operationLabel", tx.getTransactionType());
                }

                logEntry.put("signature", null);
                logEntry.put("ipAddress", null);

                allLogs.add(logEntry);
            }

            // 分页
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
     * 搜索区块链交易
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

            Long total = transactionMapper.selectCount(wrapper);
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
}
