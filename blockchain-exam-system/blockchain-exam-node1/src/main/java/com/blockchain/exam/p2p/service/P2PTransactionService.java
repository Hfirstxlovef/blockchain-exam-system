package com.blockchain.exam.p2p.service;

import com.blockchain.exam.blockchain.entity.Transaction;
import com.blockchain.exam.blockchain.mapper.TransactionMapper;
import com.blockchain.exam.p2p.entity.P2PNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * P2P交易广播服务
 *
 * 负责交易的广播和接收：
 * - 将本地交易广播到所有邻居节点
 * - 接收来自邻居节点的交易
 * - 交易去重
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@Service
public class P2PTransactionService {

    @Autowired
    private P2PNodeService p2pNodeService;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 当前节点ID
     */
    @Value("${p2p.node.id}")
    private String currentNodeId;

    /**
     * 广播交易到所有邻居节点
     *
     * @param transaction 交易
     * @return 成功广播的节点数
     */
    public int broadcastTransaction(Transaction transaction) {
        List<P2PNode> neighbors = p2pNodeService.getNeighborNodes();

        if (neighbors.isEmpty()) {
            log.debug("没有在线的邻居节点，无法广播交易");
            return 0;
        }

        log.info("开始广播交易 - 交易ID: {}, 类型: {}, 邻居数: {}",
                transaction.getId(), transaction.getTransactionType(), neighbors.size());

        int successCount = 0;
        for (P2PNode neighbor : neighbors) {
            try {
                if (sendTransaction(neighbor, transaction)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("向节点 {} 广播交易失败", neighbor.getNodeId(), e);
            }
        }

        log.info("交易广播完成 - 成功: {}/{}", successCount, neighbors.size());
        return successCount;
    }

    /**
     * 发送交易到指定节点
     *
     * @param neighbor    邻居节点
     * @param transaction 交易
     * @return 是否成功
     */
    private boolean sendTransaction(P2PNode neighbor, Transaction transaction) {
        try {
            String url = neighbor.getNodeUrl() + "/transaction/receive";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Transaction> request = new HttpEntity<>(transaction, headers);

            log.debug("发送交易到节点 - URL: {}, 交易ID: {}", url, transaction.getId());

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("交易发送成功 - 节点: {}", neighbor.getNodeId());
                return true;
            } else {
                log.warn("交易发送失败 - 节点: {}, 状态码: {}",
                        neighbor.getNodeId(), response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("发送交易到节点 {} 失败", neighbor.getNodeId(), e);
            return false;
        }
    }

    /**
     * 接收来自其他节点的交易
     *
     * @param transaction 交易
     * @return 是否接受（true=新交易，false=重复交易）
     */
    @Transactional
    public boolean receiveTransaction(Transaction transaction) {
        try {
            // 检查交易是否已存在（去重）
            if (isTransactionExists(transaction)) {
                log.debug("交易已存在，忽略 - 类型: {}, 数据: {}",
                        transaction.getTransactionType(),
                        transaction.getTransactionData().substring(0, Math.min(50, transaction.getTransactionData().length())));
                return false;
            }

            // 保存交易到本地交易池
            // 清除原有ID，让数据库重新分配
            transaction.setId(null);
            // 确保状态为PENDING
            transaction.setStatus("PENDING");
            // 确保blockIndex为空
            transaction.setBlockIndex(null);

            transactionMapper.insert(transaction);

            log.info("接收新交易 - 来源节点: {}, 类型: {}",
                    transaction.getCreatorNode(), transaction.getTransactionType());

            return true;
        } catch (Exception e) {
            log.error("接收交易失败", e);
            return false;
        }
    }

    /**
     * 检查交易是否已存在
     * 使用交易数据和类型作为唯一性判断
     *
     * @param transaction 交易
     * @return 是否存在
     */
    private boolean isTransactionExists(Transaction transaction) {
        // 简化版本：查询相同类型和数据的交易
        // 实际项目中可能需要更复杂的去重逻辑（如使用交易哈希）
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Transaction> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("transaction_type", transaction.getTransactionType());
        queryWrapper.eq("transaction_data", transaction.getTransactionData());
        queryWrapper.eq("creator_node", transaction.getCreatorNode());

        Long count = transactionMapper.selectCount(queryWrapper);
        return count != null && count > 0;
    }

    /**
     * 批量广播交易
     *
     * @param transactions 交易列表
     * @return 成功广播的交易数
     */
    public int broadcastTransactions(List<Transaction> transactions) {
        int successCount = 0;
        for (Transaction transaction : transactions) {
            try {
                broadcastTransaction(transaction);
                successCount++;
            } catch (Exception e) {
                log.error("广播交易失败 - 交易ID: {}", transaction.getId(), e);
            }
        }
        return successCount;
    }

    /**
     * 获取广播统计信息
     *
     * @return 统计信息
     */
    public java.util.Map<String, Object> getBroadcastStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("currentNodeId", currentNodeId);
        stats.put("neighborCount", p2pNodeService.getNeighborNodes().size());

        // 统计本地创建的交易数
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Transaction> localWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        localWrapper.eq("creator_node", currentNodeId);
        Long localTransactionCount = transactionMapper.selectCount(localWrapper);

        // 统计其他节点的交易数
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Transaction> remoteWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        remoteWrapper.ne("creator_node", currentNodeId);
        Long remoteTransactionCount = transactionMapper.selectCount(remoteWrapper);

        stats.put("localTransactions", localTransactionCount);
        stats.put("remoteTransactions", remoteTransactionCount);
        stats.put("totalTransactions", localTransactionCount + remoteTransactionCount);

        return stats;
    }
}
