package com.blockchain.exam.blockchain.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blockchain.exam.blockchain.entity.BlockData;
import com.blockchain.exam.blockchain.entity.Transaction;
import com.blockchain.exam.blockchain.mapper.TransactionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 矿工服务
 *
 * 定时检查交易池，打包交易并挖矿
 * - 每隔30秒检查一次交易池
 * - 每次最多打包3笔交易
 * - 挖矿成功后更新交易状态
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@Service
public class MinerService {

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 交易池表名
     */
    @Value("${blockchain.pool-table}")
    private String poolTableName;

    /**
     * 当前节点ID
     */
    @Value("${p2p.node.id}")
    private String nodeId;

    /**
     * 每次打包的交易数量
     */
    private static final int BATCH_SIZE = 3;

    /**
     * 是否启用自动挖矿
     */
    private volatile boolean miningEnabled = true;

    /**
     * 定时挖矿任务
     * 每隔30秒执行一次
     */
    @Scheduled(fixedDelayString = "${blockchain.mining-interval:30000}", initialDelay = 10000)
    public void scheduledMining() {
        if (!miningEnabled) {
            return;
        }

        try {
            mineBlock();
        } catch (Exception e) {
            log.error("定时挖矿任务执行失败", e);
        }
    }

    /**
     * 执行挖矿
     * 从交易池获取待打包交易，打包并挖矿
     */
    @Transactional
    public void mineBlock() {
        // 查询待打包的交易
        List<Transaction> pendingTransactions = getPendingTransactions(BATCH_SIZE);

        if (pendingTransactions.isEmpty()) {
            log.debug("交易池为空，暂无交易需要打包");
            return;
        }

        log.info("开始挖矿 - 待打包交易数: {}", pendingTransactions.size());

        try {
            // 创建区块数据
            BlockData blockData = BlockData.createTransactionBatch(pendingTransactions);

            // 添加区块（包含PoW挖矿）
            com.blockchain.exam.blockchain.entity.Block newBlock = blockchainService.addBlock(blockData);

            // 更新交易状态为已打包
            List<Long> transactionIds = pendingTransactions.stream()
                    .map(Transaction::getId)
                    .collect(Collectors.toList());

            int updatedCount = updateTransactionsToMined(transactionIds, newBlock.getBlockIndex());

            log.info("挖矿成功 - 区块高度: {}, 打包交易数: {}, 更新交易状态: {}",
                    newBlock.getBlockIndex(), pendingTransactions.size(), updatedCount);

        } catch (Exception e) {
            log.error("挖矿失败", e);
            // 可以考虑将交易标记为无效或重试
        }
    }

    /**
     * 获取待打包的交易
     *
     * @param limit 最多获取的交易数量
     * @return 待打包的交易列表
     */
    private List<Transaction> getPendingTransactions(int limit) {
        QueryWrapper<Transaction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "PENDING");
        queryWrapper.orderByAsc("create_time");
        queryWrapper.last("LIMIT " + limit);
        return transactionMapper.selectList(queryWrapper);
    }

    /**
     * 批量更新交易状态为已打包
     *
     * @param transactionIds 交易ID列表
     * @param blockIndex     区块高度
     * @return 更新的记录数
     */
    private int updateTransactionsToMined(List<Long> transactionIds, Long blockIndex) {
        int count = 0;
        for (Long id : transactionIds) {
            Transaction transaction = transactionMapper.selectById(id);
            if (transaction != null) {
                transaction.markAsMined(blockIndex);
                transactionMapper.updateById(transaction);
                count++;
            }
        }
        return count;
    }

    /**
     * 手动触发挖矿（用于测试）
     *
     * @return 是否成功
     */
    public boolean manualMine() {
        try {
            mineBlock();
            return true;
        } catch (Exception e) {
            log.error("手动挖矿失败", e);
            return false;
        }
    }

    /**
     * 启用自动挖矿
     */
    public void enableMining() {
        miningEnabled = true;
        log.info("自动挖矿已启用 - 节点: {}", nodeId);
    }

    /**
     * 禁用自动挖矿
     */
    public void disableMining() {
        miningEnabled = false;
        log.info("自动挖矿已禁用 - 节点: {}", nodeId);
    }

    /**
     * 获取挖矿状态
     *
     * @return 是否启用挖矿
     */
    public boolean isMiningEnabled() {
        return miningEnabled;
    }

    /**
     * 获取交易池统计信息
     *
     * @return 统计信息
     */
    public java.util.Map<String, Object> getPoolStats() {
        QueryWrapper<Transaction> pendingWrapper = new QueryWrapper<>();
        pendingWrapper.eq("status", "PENDING");
        Long pendingCount = transactionMapper.selectCount(pendingWrapper);

        QueryWrapper<Transaction> minedWrapper = new QueryWrapper<>();
        minedWrapper.eq("status", "MINED");
        Long minedCount = transactionMapper.selectCount(minedWrapper);

        QueryWrapper<Transaction> invalidWrapper = new QueryWrapper<>();
        invalidWrapper.eq("status", "INVALID");
        Long invalidCount = transactionMapper.selectCount(invalidWrapper);

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("nodeId", nodeId);
        stats.put("pendingTransactions", pendingCount);
        stats.put("minedTransactions", minedCount);
        stats.put("invalidTransactions", invalidCount);
        stats.put("totalTransactions", pendingCount + minedCount + invalidCount);
        stats.put("miningEnabled", miningEnabled);
        stats.put("batchSize", BATCH_SIZE);

        return stats;
    }

    /**
     * 清理已打包的旧交易（可选）
     * 删除超过N天的已打包交易，减少数据库压力
     *
     * @param days 保留天数
     * @return 删除的记录数
     */
    @Transactional
    public int cleanOldMinedTransactions(int days) {
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);

        QueryWrapper<Transaction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "MINED");
        queryWrapper.lt("create_time", new java.util.Date(cutoffTime));

        int count = transactionMapper.delete(queryWrapper);
        log.info("清理旧交易记录 - 删除数量: {}, 保留天数: {}", count, days);

        return count;
    }

    /**
     * 重新提交无效交易（可选）
     * 将状态为INVALID的交易重新标记为PENDING
     *
     * @return 重新提交的记录数
     */
    @Transactional
    public int resubmitInvalidTransactions() {
        QueryWrapper<Transaction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "INVALID");
        List<Transaction> invalidTransactions = transactionMapper.selectList(queryWrapper);

        int count = 0;
        for (Transaction transaction : invalidTransactions) {
            transaction.setStatus("PENDING");
            transaction.setBlockIndex(null);
            transactionMapper.updateById(transaction);
            count++;
        }

        log.info("重新提交无效交易 - 数量: {}", count);
        return count;
    }
}
