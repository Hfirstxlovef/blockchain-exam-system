package com.blockchain.exam.blockchain.service;

import com.blockchain.exam.blockchain.entity.Block;
import com.blockchain.exam.blockchain.util.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 共识服务
 *
 * 实现PoW（工作量证明）共识算法
 * - 通过不断尝试nonce值，寻找满足难度要求的哈希
 * - 难度由配置文件指定（默认4，表示哈希前4位为0）
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@Service
public class ConsensusService {

    /**
     * 挖矿难度（哈希前N位必须为0）
     */
    @Value("${blockchain.difficulty:4}")
    private int difficulty;

    /**
     * 当前节点ID
     */
    @Value("${p2p.node.id}")
    private String nodeId;

    /**
     * 执行工作量证明（PoW）
     * 不断尝试nonce值，直到找到满足难度要求的哈希
     *
     * @param block 待挖矿的区块
     * @return 挖矿成功后的区块（包含有效的nonce和hash）
     */
    public Block proofOfWork(Block block) {
        long startTime = System.currentTimeMillis();
        log.info("开始PoW挖矿 - 区块高度: {}, 难度: {}", block.getBlockIndex(), difficulty);

        // 设置难度和矿工地址
        block.setDifficulty(difficulty);
        block.setMinerAddress(nodeId);

        // 从0开始尝试nonce
        int nonce = 0;
        String hash;
        String target = HashUtil.getDifficultyTarget(difficulty);

        while (true) {
            // 设置nonce
            block.setNonce(nonce);

            // 计算哈希
            hash = calculateBlockHash(block);

            // 检查是否满足难度要求
            if (hash.startsWith(target)) {
                // 找到有效哈希
                block.setCurrentHash(hash);
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                log.info("PoW挖矿成功! 区块高度: {}, Nonce: {}, 哈希: {}, 耗时: {}ms",
                        block.getBlockIndex(), nonce, hash, duration);

                return block;
            }

            // 尝试下一个nonce
            nonce++;

            // 每10000次尝试输出一次进度
            if (nonce % 10000 == 0) {
                log.debug("PoW挖矿中... 已尝试: {}, 当前哈希: {}", nonce, hash);
            }

            // 防止无限循环（理论上不会发生，但作为保护机制）
            if (nonce == Integer.MAX_VALUE) {
                log.error("PoW挖矿失败 - 达到最大nonce值");
                throw new RuntimeException("PoW挖矿失败 - 无法找到有效哈希");
            }
        }
    }

    /**
     * 计算区块哈希
     *
     * @param block 区块
     * @return SHA-256哈希值
     */
    public String calculateBlockHash(Block block) {
        String hashData = block.buildHashData();
        return HashUtil.sha256(hashData);
    }

    /**
     * 验证区块的PoW
     * 检查区块哈希是否满足难度要求
     *
     * @param block 区块
     * @return 是否有效
     */
    public boolean validateProofOfWork(Block block) {
        // 重新计算哈希
        String calculatedHash = calculateBlockHash(block);

        // 检查计算的哈希是否与区块中保存的哈希一致
        if (!calculatedHash.equals(block.getCurrentHash())) {
            log.warn("区块哈希不匹配 - 区块: {}, 计算: {}, 保存: {}",
                    block.getBlockIndex(), calculatedHash, block.getCurrentHash());
            return false;
        }

        // 检查哈希是否满足难度要求
        boolean isValid = HashUtil.validateProofOfWork(calculatedHash, block.getDifficulty());

        if (!isValid) {
            log.warn("区块PoW验证失败 - 区块: {}, 哈希: {}, 难度: {}",
                    block.getBlockIndex(), calculatedHash, block.getDifficulty());
        }

        return isValid;
    }

    /**
     * 验证区块哈希链
     * 检查当前区块的previousHash是否与前一区块的currentHash一致
     *
     * @param currentBlock  当前区块
     * @param previousBlock 前一区块
     * @return 是否有效
     */
    public boolean validateBlockLink(Block currentBlock, Block previousBlock) {
        if (currentBlock.isGenesisBlock()) {
            // 创世区块的previousHash应该为"0"
            return "0".equals(currentBlock.getPreviousHash());
        }

        if (previousBlock == null) {
            log.warn("前一区块为空 - 当前区块: {}", currentBlock.getBlockIndex());
            return false;
        }

        // 检查区块高度连续性
        if (!currentBlock.getBlockIndex().equals(previousBlock.getBlockIndex() + 1)) {
            log.warn("区块高度不连续 - 当前: {}, 前一: {}",
                    currentBlock.getBlockIndex(), previousBlock.getBlockIndex());
            return false;
        }

        // 检查哈希链接
        if (!currentBlock.getPreviousHash().equals(previousBlock.getCurrentHash())) {
            log.warn("区块哈希链断裂 - 当前previousHash: {}, 前一currentHash: {}",
                    currentBlock.getPreviousHash(), previousBlock.getCurrentHash());
            return false;
        }

        return true;
    }

    /**
     * 完整验证区块
     * 包括PoW验证和哈希链验证
     *
     * @param currentBlock  当前区块
     * @param previousBlock 前一区块（如果是创世区块则为null）
     * @return 是否有效
     */
    public boolean validateBlock(Block currentBlock, Block previousBlock) {
        // 验证PoW
        if (!validateProofOfWork(currentBlock)) {
            return false;
        }

        // 验证哈希链
        if (!validateBlockLink(currentBlock, previousBlock)) {
            return false;
        }

        log.debug("区块验证通过 - 区块高度: {}", currentBlock.getBlockIndex());
        return true;
    }

    /**
     * 获取当前难度
     *
     * @return 难度值
     */
    public int getDifficulty() {
        return difficulty;
    }

    /**
     * 获取难度目标字符串
     *
     * @return 目标前缀（例如："0000"）
     */
    public String getDifficultyTarget() {
        return HashUtil.getDifficultyTarget(difficulty);
    }

    /**
     * 设置难度（用于测试）
     *
     * @param difficulty 新的难度值
     */
    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
        log.info("挖矿难度已调整为: {}", difficulty);
    }
}
