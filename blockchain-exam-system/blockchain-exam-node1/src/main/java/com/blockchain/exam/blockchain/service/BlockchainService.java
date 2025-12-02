package com.blockchain.exam.blockchain.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blockchain.exam.blockchain.entity.Block;
import com.blockchain.exam.blockchain.entity.BlockData;
import com.blockchain.exam.blockchain.mapper.BlockMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 区块链服务
 *
 * 管理区块链的核心功能：
 * - 创建创世区块
 * - 添加新区块（挖矿）
 * - 验证区块链
 * - 查询区块
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@Service
public class BlockchainService {

    @Autowired
    private BlockMapper blockMapper;

    @Autowired
    private ConsensusService consensusService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 区块链表名
     */
    @Value("${blockchain.table-name}")
    private String tableName;

    /**
     * 当前节点ID
     */
    @Value("${p2p.node.id}")
    private String nodeId;

    /**
     * 当前节点名称
     */
    @Value("${p2p.node.name}")
    private String nodeName;

    /**
     * 初始化区块链
     * 如果区块链为空，则创建创世区块
     */
    @PostConstruct
    public void initBlockchain() {
        try {
            Long chainHeight = getChainHeight();
            if (chainHeight == -1) {
                log.info("区块链为空，创建创世区块...");
                createGenesisBlock();
            } else {
                log.info("区块链已存在 - 节点: {}, 当前高度: {}", nodeId, chainHeight);
                validateChain();
            }
        } catch (Exception e) {
            log.error("初始化区块链失败", e);
        }
    }

    /**
     * 创建创世区块
     */
    @Transactional
    public Block createGenesisBlock() {
        log.info("开始创建创世区块 - 节点: {}", nodeId);

        try {
            // 创建创世区块数据
            BlockData genesisData = new BlockData();
            genesisData.setType("GENESIS");
            genesisData.setTimestamp(System.currentTimeMillis());
            genesisData.setData(new java.util.HashMap<String, Object>() {{
                put("message", "创世区块 - " + nodeName);
                put("nodeId", nodeId);
                put("nodeName", nodeName);
                put("timestamp", System.currentTimeMillis());
            }});

            // 创建区块
            Block genesisBlock = new Block();
            genesisBlock.setBlockIndex(0L);
            genesisBlock.setPreviousHash("0");
            genesisBlock.setTimestamp(System.currentTimeMillis());
            genesisBlock.setData(objectMapper.writeValueAsString(genesisData));
            genesisBlock.setNonce(0);

            // 执行PoW（创世区块也需要挖矿）
            genesisBlock = consensusService.proofOfWork(genesisBlock);

            // 保存到数据库
            blockMapper.insert(genesisBlock);

            log.info("创世区块创建成功 - 哈希: {}, 难度: {}",
                    genesisBlock.getCurrentHash(), genesisBlock.getDifficulty());

            return genesisBlock;
        } catch (JsonProcessingException e) {
            log.error("创建创世区块失败 - JSON序列化错误", e);
            throw new RuntimeException("创建创世区块失败", e);
        }
    }

    /**
     * 添加新区块
     *
     * @param blockData 区块数据
     * @return 新区块
     */
    @Transactional
    public Block addBlock(BlockData blockData) {
        log.info("开始添加新区块 - 类型: {}", blockData.getType());

        try {
            // 获取最新区块
            Block latestBlock = getLatestBlock();
            if (latestBlock == null) {
                log.error("区块链为空，无法添加新区块");
                throw new RuntimeException("区块链为空，请先创建创世区块");
            }

            // 创建新区块
            Block newBlock = new Block();
            newBlock.setBlockIndex(latestBlock.getBlockIndex() + 1);
            newBlock.setPreviousHash(latestBlock.getCurrentHash());
            newBlock.setTimestamp(System.currentTimeMillis());
            newBlock.setData(objectMapper.writeValueAsString(blockData));
            newBlock.setNonce(0);

            // 执行PoW挖矿
            newBlock = consensusService.proofOfWork(newBlock);

            // 验证新区块
            if (!consensusService.validateBlock(newBlock, latestBlock)) {
                log.error("新区块验证失败");
                throw new RuntimeException("新区块验证失败");
            }

            // 保存到数据库
            blockMapper.insert(newBlock);

            log.info("新区块添加成功 - 高度: {}, 哈希: {}",
                    newBlock.getBlockIndex(), newBlock.getCurrentHash());

            return newBlock;
        } catch (JsonProcessingException e) {
            log.error("添加新区块失败 - JSON序列化错误", e);
            throw new RuntimeException("添加新区块失败", e);
        }
    }

    /**
     * 批量添加新区块（用于同步）
     *
     * @param blockData 区块数据
     * @return 新区块
     */
    @Transactional
    public Block addBlockWithData(String blockData) {
        log.info("开始添加新区块（带数据）");

        // 获取最新区块
        Block latestBlock = getLatestBlock();
        if (latestBlock == null) {
            log.error("区块链为空，无法添加新区块");
            throw new RuntimeException("区块链为空，请先创建创世区块");
        }

        // 创建新区块
        Block newBlock = new Block();
        newBlock.setBlockIndex(latestBlock.getBlockIndex() + 1);
        newBlock.setPreviousHash(latestBlock.getCurrentHash());
        newBlock.setTimestamp(System.currentTimeMillis());
        newBlock.setData(blockData);
        newBlock.setNonce(0);

        // 执行PoW挖矿
        newBlock = consensusService.proofOfWork(newBlock);

        // 验证新区块
        if (!consensusService.validateBlock(newBlock, latestBlock)) {
            log.error("新区块验证失败");
            throw new RuntimeException("新区块验证失败");
        }

        // 保存到数据库
        blockMapper.insert(newBlock);

        log.info("新区块添加成功 - 高度: {}, 哈希: {}",
                newBlock.getBlockIndex(), newBlock.getCurrentHash());

        return newBlock;
    }

    /**
     * 获取最新区块
     *
     * @return 最新区块，如果区块链为空则返回null
     */
    public Block getLatestBlock() {
        QueryWrapper<Block> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("block_index");
        queryWrapper.last("LIMIT 1");
        return blockMapper.selectOne(queryWrapper);
    }

    /**
     * 根据区块高度查询区块
     *
     * @param blockIndex 区块高度
     * @return 区块
     */
    public Block getBlockByIndex(Long blockIndex) {
        QueryWrapper<Block> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("block_index", blockIndex);
        return blockMapper.selectOne(queryWrapper);
    }

    /**
     * 根据区块哈希查询区块
     *
     * @param hash 区块哈希
     * @return 区块
     */
    public Block getBlockByHash(String hash) {
        QueryWrapper<Block> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("current_hash", hash);
        return blockMapper.selectOne(queryWrapper);
    }

    /**
     * 获取所有区块
     *
     * @return 所有区块列表（按高度升序）
     */
    public List<Block> getAllBlocks() {
        QueryWrapper<Block> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("block_index");
        return blockMapper.selectList(queryWrapper);
    }

    /**
     * 获取区块链高度
     *
     * @return 当前区块链高度，如果区块链为空则返回-1
     */
    public Long getChainHeight() {
        QueryWrapper<Block> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("COALESCE(MAX(block_index), -1) as block_index");
        Block result = blockMapper.selectOne(queryWrapper);
        return result != null ? result.getBlockIndex() : -1L;
    }

    /**
     * 验证整个区块链
     *
     * @return 是否有效
     */
    public boolean validateChain() {
        log.info("开始验证区块链...");

        List<Block> blocks = getAllBlocks();

        if (blocks.isEmpty()) {
            log.warn("区块链为空");
            return false;
        }

        // 验证创世区块
        Block genesisBlock = blocks.get(0);
        if (!genesisBlock.isGenesisBlock()) {
            log.error("第一个区块不是创世区块");
            return false;
        }

        if (!consensusService.validateProofOfWork(genesisBlock)) {
            log.error("创世区块PoW验证失败");
            return false;
        }

        // 验证后续区块
        for (int i = 1; i < blocks.size(); i++) {
            Block currentBlock = blocks.get(i);
            Block previousBlock = blocks.get(i - 1);

            if (!consensusService.validateBlock(currentBlock, previousBlock)) {
                log.error("区块验证失败 - 区块高度: {}", currentBlock.getBlockIndex());
                return false;
            }
        }

        log.info("区块链验证通过 - 总区块数: {}", blocks.size());
        return true;
    }

    /**
     * 获取指定范围的区块
     *
     * @param startIndex 起始高度
     * @param endIndex   结束高度
     * @return 区块列表
     */
    public List<Block> getBlockRange(Long startIndex, Long endIndex) {
        QueryWrapper<Block> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("block_index", startIndex);
        queryWrapper.le("block_index", endIndex);
        queryWrapper.orderByAsc("block_index");
        return blockMapper.selectList(queryWrapper);
    }

    /**
     * 获取区块链统计信息
     *
     * @return 统计信息Map
     */
    public java.util.Map<String, Object> getChainStats() {
        Long chainHeight = getChainHeight();
        Block latestBlock = getLatestBlock();

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("nodeId", nodeId);
        stats.put("nodeName", nodeName);
        stats.put("chainHeight", chainHeight);
        stats.put("totalBlocks", chainHeight + 1);
        stats.put("difficulty", consensusService.getDifficulty());

        if (latestBlock != null) {
            stats.put("latestBlockHash", latestBlock.getCurrentHash());
            stats.put("latestBlockTime", latestBlock.getTimestamp());
            stats.put("latestBlockMiner", latestBlock.getMinerAddress());
        }

        return stats;
    }
}
