package com.blockchain.exam.p2p.service;

import com.blockchain.exam.blockchain.entity.Block;
import com.blockchain.exam.blockchain.mapper.BlockMapper;
import com.blockchain.exam.blockchain.service.BlockchainService;
import com.blockchain.exam.blockchain.service.ConsensusService;
import com.blockchain.exam.p2p.entity.P2PNode;
import com.blockchain.exam.p2p.entity.P2PSyncLog;
import com.blockchain.exam.p2p.mapper.P2PSyncLogMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * P2P区块链同步服务
 *
 * 负责节点间的区块链同步：
 * - 定时从邻居节点同步区块链
 * - 解决区块链分叉（最长链原则）
 * - 记录同步日志
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@Service
public class P2PSyncService {

    @Autowired
    private P2PNodeService p2pNodeService;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private ConsensusService consensusService;

    @Autowired
    private BlockMapper blockMapper;

    @Autowired
    private P2PSyncLogMapper syncLogMapper;

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
     * 是否启用自动同步
     */
    private volatile boolean syncEnabled = true;

    /**
     * 定时同步任务
     * 每60秒执行一次
     */
    @Scheduled(fixedDelayString = "${blockchain.sync-interval:60000}", initialDelay = 30000)
    public void scheduledSync() {
        if (!syncEnabled) {
            return;
        }

        try {
            syncFromNeighbors();
        } catch (Exception e) {
            log.error("定时同步任务执行失败", e);
        }
    }

    /**
     * 从邻居节点同步区块链
     */
    public void syncFromNeighbors() {
        List<P2PNode> neighbors = p2pNodeService.getNeighborNodes();

        if (neighbors.isEmpty()) {
            log.debug("没有在线的邻居节点，跳过同步");
            return;
        }

        log.info("开始同步区块链 - 邻居节点数: {}", neighbors.size());

        for (P2PNode neighbor : neighbors) {
            try {
                syncFromNode(neighbor);
            } catch (Exception e) {
                log.error("从节点 {} 同步失败", neighbor.getNodeId(), e);
            }
        }
    }

    /**
     * 从指定节点同步区块链
     *
     * @param neighbor 邻居节点
     */
    @Transactional
    public void syncFromNode(P2PNode neighbor) {
        String targetNodeId = neighbor.getNodeId();

        // 创建同步日志
        P2PSyncLog syncLog = new P2PSyncLog();
        syncLog.setSourceNode(currentNodeId);
        syncLog.setTargetNode(targetNodeId);
        syncLog.setSyncType("BLOCK_SYNC");
        syncLog.setSyncTime(LocalDateTime.now());
        syncLog.markAsInProgress();
        syncLogMapper.insert(syncLog);

        try {
            // 获取邻居节点的区块链
            String url = neighbor.getNodeUrl() + "/blockchain/chain";
            log.debug("请求邻居节点区块链 - URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("请求失败 - 状态码: " + response.getStatusCode());
            }

            // 解析区块链数据
            List<Block> neighborChain = objectMapper.readValue(
                response.getBody(),
                new TypeReference<List<Block>>() {}
            );

            if (neighborChain == null || neighborChain.isEmpty()) {
                log.debug("邻居节点 {} 的区块链为空", targetNodeId);
                syncLog.markAsSuccess(0L);
                syncLogMapper.updateById(syncLog);
                return;
            }

            // 获取本地区块链
            List<Block> localChain = blockchainService.getAllBlocks();
            Long localHeight = blockchainService.getChainHeight();
            Long neighborHeight = (long) (neighborChain.size() - 1);

            log.info("区块链对比 - 本地高度: {}, 邻居({})高度: {}",
                    localHeight, targetNodeId, neighborHeight);

            // 如果邻居链更长，且有效，则替换本地链
            if (neighborHeight > localHeight) {
                if (isValidChain(neighborChain)) {
                    replaceChain(neighborChain);
                    log.info("区块链已更新 - 从 {} 同步，新高度: {}", targetNodeId, neighborHeight);
                    syncLog.markAsSuccess(neighborHeight);
                } else {
                    log.warn("邻居节点 {} 的区块链无效，拒绝同步", targetNodeId);
                    syncLog.markAsFailure("邻居区块链验证失败");
                }
            } else {
                log.debug("本地区块链已是最新 - 本地: {}, 邻居: {}", localHeight, neighborHeight);
                syncLog.markAsSuccess(localHeight);
            }

            syncLogMapper.updateById(syncLog);

        } catch (Exception e) {
            log.error("从节点 {} 同步失败", targetNodeId, e);
            syncLog.markAsFailure(e.getMessage());
            syncLogMapper.updateById(syncLog);
        }
    }

    /**
     * 验证区块链是否有效
     *
     * @param chain 区块链
     * @return 是否有效
     */
    private boolean isValidChain(List<Block> chain) {
        if (chain == null || chain.isEmpty()) {
            return false;
        }

        // 验证创世区块
        Block genesisBlock = chain.get(0);
        if (!genesisBlock.isGenesisBlock()) {
            log.warn("第一个区块不是创世区块");
            return false;
        }

        if (!consensusService.validateProofOfWork(genesisBlock)) {
            log.warn("创世区块PoW验证失败");
            return false;
        }

        // 验证后续区块
        for (int i = 1; i < chain.size(); i++) {
            Block currentBlock = chain.get(i);
            Block previousBlock = chain.get(i - 1);

            if (!consensusService.validateBlock(currentBlock, previousBlock)) {
                log.warn("区块验证失败 - 区块高度: {}", currentBlock.getBlockIndex());
                return false;
            }
        }

        return true;
    }

    /**
     * 替换本地区块链
     *
     * @param newChain 新的区块链
     */
    @Transactional
    public void replaceChain(List<Block> newChain) {
        log.info("开始替换区块链 - 新链长度: {}", newChain.size());

        // 删除本地所有区块
        blockMapper.delete(null);

        // 插入新的区块链
        for (Block block : newChain) {
            // 清除自增ID，让数据库重新分配
            block.setId(null);
            blockMapper.insert(block);
        }

        log.info("区块链替换完成 - 新高度: {}", newChain.size() - 1);
    }

    /**
     * 手动触发同步（用于测试）
     *
     * @return 是否成功
     */
    public boolean manualSync() {
        try {
            syncFromNeighbors();
            return true;
        } catch (Exception e) {
            log.error("手动同步失败", e);
            return false;
        }
    }

    /**
     * 启用自动同步
     */
    public void enableSync() {
        syncEnabled = true;
        log.info("自动同步已启用 - 节点: {}", currentNodeId);
    }

    /**
     * 禁用自动同步
     */
    public void disableSync() {
        syncEnabled = false;
        log.info("自动同步已禁用 - 节点: {}", currentNodeId);
    }

    /**
     * 获取同步状态
     *
     * @return 是否启用同步
     */
    public boolean isSyncEnabled() {
        return syncEnabled;
    }

    /**
     * 获取同步统计信息
     *
     * @return 统计信息
     */
    public java.util.Map<String, Object> getSyncStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("currentNodeId", currentNodeId);
        stats.put("syncEnabled", syncEnabled);

        // 统计最近的同步日志
        List<P2PSyncLog> recentLogs = syncLogMapper.selectByNode(currentNodeId);
        if (recentLogs.size() > 10) {
            recentLogs = recentLogs.subList(0, 10);
        }

        long successCount = recentLogs.stream().filter(P2PSyncLog::isSuccess).count();
        long failureCount = recentLogs.stream().filter(P2PSyncLog::isFailure).count();

        stats.put("recentSyncCount", recentLogs.size());
        stats.put("successCount", successCount);
        stats.put("failureCount", failureCount);
        stats.put("recentLogs", recentLogs);

        return stats;
    }
}
