package com.blockchain.exam.blockchain.controller;

import com.blockchain.exam.blockchain.entity.Transaction;
import com.blockchain.exam.blockchain.mapper.TransactionMapper;
import com.blockchain.exam.p2p.service.P2PTransactionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.exam.approval.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易控制器
 *
 * 提供交易相关的REST API
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@RestController
@RequestMapping("/transaction")
public class TransactionController {

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private P2PTransactionService p2pTransactionService;

    @Value("${p2p.node.id}")
    private String currentNodeId;

    /**
     * 创建新交易
     * 创建后会自动广播到所有邻居节点
     *
     * @param transaction 交易
     * @return 创建结果
     */
    @PostMapping("/create")
    public ResponseEntity<Result<Map<String, Object>>> createTransaction(@RequestBody Transaction transaction) {
        try {
            // 设置创建节点
            transaction.setCreatorNode(currentNodeId);
            // 设置状态为待打包
            transaction.setStatus("PENDING");
            // 清空区块高度
            transaction.setBlockIndex(null);

            // 保存到本地交易池
            transactionMapper.insert(transaction);

            log.info("创建新交易 - ID: {}, 类型: {}", transaction.getId(), transaction.getTransactionType());

            // 广播到邻居节点
            int broadcastCount = p2pTransactionService.broadcastTransaction(transaction);

            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("transactionId", transaction.getId());
            data.put("broadcastCount", broadcastCount);
            data.put("message", "交易创建成功并已广播");

            return ResponseEntity.ok(Result.success(data));
        } catch (Exception e) {
            log.error("创建交易失败", e);
            return ResponseEntity.ok(Result.error("创建交易失败: " + e.getMessage()));
        }
    }

    /**
     * 接收来自其他节点的交易
     * 用于P2P广播
     *
     * @param transaction 交易
     * @return 接收结果
     */
    @PostMapping("/receive")
    public ResponseEntity<Result<Map<String, Object>>> receiveTransaction(@RequestBody Transaction transaction) {
        try {
            boolean accepted = p2pTransactionService.receiveTransaction(transaction);

            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("accepted", accepted);
            data.put("message", accepted ? "交易已接受" : "交易已存在（重复）");

            return ResponseEntity.ok(Result.success(data));
        } catch (Exception e) {
            log.error("接收交易失败", e);
            return ResponseEntity.ok(Result.error("接收交易失败: " + e.getMessage()));
        }
    }

    /**
     * 查询待打包交易
     *
     * @return 待打包交易列表
     */
    @GetMapping("/pending")
    public ResponseEntity<Result<List<Transaction>>> getPendingTransactions() {
        try {
            QueryWrapper<Transaction> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", "PENDING");
            queryWrapper.orderByAsc("create_time");
            List<Transaction> transactions = transactionMapper.selectList(queryWrapper);
            return ResponseEntity.ok(Result.success(transactions));
        } catch (Exception e) {
            log.error("查询待打包交易失败", e);
            return ResponseEntity.ok(Result.error("查询待打包交易失败"));
        }
    }

    /**
     * 根据区块高度查询交易
     *
     * @param blockIndex 区块高度
     * @return 交易列表
     */
    @GetMapping("/block/{blockIndex}")
    public ResponseEntity<Result<List<Transaction>>> getTransactionsByBlock(@PathVariable Long blockIndex) {
        try {
            QueryWrapper<Transaction> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("block_index", blockIndex);
            queryWrapper.eq("status", "MINED");
            List<Transaction> transactions = transactionMapper.selectList(queryWrapper);
            return ResponseEntity.ok(Result.success(transactions));
        } catch (Exception e) {
            log.error("查询区块交易失败 - 区块高度: {}", blockIndex, e);
            return ResponseEntity.ok(Result.error("查询区块交易失败"));
        }
    }

    /**
     * 获取交易池统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Result<Map<String, Object>>> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            QueryWrapper<Transaction> pendingWrapper = new QueryWrapper<>();
            pendingWrapper.eq("status", "PENDING");
            Long pendingCount = transactionMapper.selectCount(pendingWrapper);

            QueryWrapper<Transaction> minedWrapper = new QueryWrapper<>();
            minedWrapper.eq("status", "MINED");
            Long minedCount = transactionMapper.selectCount(minedWrapper);

            stats.put("pending", pendingCount);
            stats.put("mined", minedCount);
            stats.put("total", pendingCount + minedCount);
            stats.put("broadcastStats", p2pTransactionService.getBroadcastStats());

            return ResponseEntity.ok(Result.success(stats));
        } catch (Exception e) {
            log.error("获取交易统计失败", e);
            return ResponseEntity.ok(Result.error("获取交易统计失败"));
        }
    }

    /**
     * 根据ID查询交易
     *
     * @param id 交易ID
     * @return 交易
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<Transaction>> getTransaction(@PathVariable Long id) {
        try {
            Transaction transaction = transactionMapper.selectById(id);
            if (transaction == null) {
                return ResponseEntity.ok(Result.error("交易不存在"));
            }
            return ResponseEntity.ok(Result.success(transaction));
        } catch (Exception e) {
            log.error("查询交易失败 - ID: {}", id, e);
            return ResponseEntity.ok(Result.error("查询交易失败"));
        }
    }
}
