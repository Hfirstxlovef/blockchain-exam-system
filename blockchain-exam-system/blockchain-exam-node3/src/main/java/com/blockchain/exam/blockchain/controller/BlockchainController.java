package com.blockchain.exam.blockchain.controller;

import com.blockchain.exam.blockchain.entity.Block;
import com.blockchain.exam.blockchain.service.BlockchainService;
import com.blockchain.exam.blockchain.service.MinerService;
import com.blockchain.exam.p2p.service.P2PSyncService;
import com.exam.approval.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 区块链控制器
 *
 * 提供区块链相关的REST API
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@RestController
@RequestMapping("/blockchain")
public class BlockchainController {

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private MinerService minerService;

    @Autowired
    private P2PSyncService syncService;

    /**
     * 获取完整区块链
     * 用于P2P节点间同步
     *
     * @return 所有区块列表
     */
    @GetMapping("/chain")
    public ResponseEntity<Result<List<Block>>> getChain() {
        try {
            List<Block> chain = blockchainService.getAllBlocks();
            return ResponseEntity.ok(Result.success(chain));
        } catch (Exception e) {
            log.error("获取区块链失败", e);
            return ResponseEntity.ok(Result.error("获取区块链失败"));
        }
    }

    /**
     * 获取最新区块
     *
     * @return 最新区块
     */
    @GetMapping("/latest")
    public ResponseEntity<Result<Block>> getLatestBlock() {
        try {
            Block latestBlock = blockchainService.getLatestBlock();
            if (latestBlock == null) {
                return ResponseEntity.ok(Result.error("没有区块"));
            }
            return ResponseEntity.ok(Result.success(latestBlock));
        } catch (Exception e) {
            log.error("获取最新区块失败", e);
            return ResponseEntity.ok(Result.error("获取最新区块失败"));
        }
    }

    /**
     * 根据高度查询区块
     *
     * @param index 区块高度
     * @return 区块
     */
    @GetMapping("/block/{index}")
    public ResponseEntity<Result<Block>> getBlockByIndex(@PathVariable Long index) {
        try {
            Block block = blockchainService.getBlockByIndex(index);
            if (block == null) {
                return ResponseEntity.ok(Result.error("区块不存在"));
            }
            return ResponseEntity.ok(Result.success(block));
        } catch (Exception e) {
            log.error("查询区块失败 - 高度: {}", index, e);
            return ResponseEntity.ok(Result.error("查询区块失败"));
        }
    }

    /**
     * 获取区块链统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Result<Map<String, Object>>> getStats() {
        try {
            Map<String, Object> stats = blockchainService.getChainStats();
            return ResponseEntity.ok(Result.success(stats));
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return ResponseEntity.ok(Result.error("获取统计信息失败"));
        }
    }

    /**
     * 验证区块链
     *
     * @return 验证结果
     */
    @GetMapping("/validate")
    public ResponseEntity<Result<Map<String, Object>>> validateChain() {
        try {
            boolean isValid = blockchainService.validateChain();
            Map<String, Object> data = new HashMap<>();
            data.put("valid", isValid);
            data.put("chainHeight", blockchainService.getChainHeight());
            return ResponseEntity.ok(Result.success(data));
        } catch (Exception e) {
            log.error("验证区块链失败", e);
            return ResponseEntity.ok(Result.error("验证区块链失败"));
        }
    }

    /**
     * 手动触发挖矿
     *
     * @return 操作结果
     */
    @PostMapping("/mine")
    public ResponseEntity<Result<Map<String, Object>>> mine() {
        try {
            boolean success = minerService.manualMine();
            Map<String, Object> data = new HashMap<>();
            data.put("success", success);
            data.put("message", success ? "挖矿成功" : "挖矿失败（可能没有待打包交易）");
            return ResponseEntity.ok(Result.success(data));
        } catch (Exception e) {
            log.error("手动挖矿失败", e);
            return ResponseEntity.ok(Result.error("挖矿失败: " + e.getMessage()));
        }
    }

    /**
     * 手动触发同步
     *
     * @return 操作结果
     */
    @PostMapping("/sync")
    public ResponseEntity<Result<Map<String, Object>>> sync() {
        try {
            boolean success = syncService.manualSync();
            Map<String, Object> data = new HashMap<>();
            data.put("success", success);
            data.put("message", success ? "同步成功" : "同步失败");
            return ResponseEntity.ok(Result.success(data));
        } catch (Exception e) {
            log.error("手动同步失败", e);
            return ResponseEntity.ok(Result.error("同步失败: " + e.getMessage()));
        }
    }

    /**
     * 获取挖矿状态
     *
     * @return 挖矿状态
     */
    @GetMapping("/mining/status")
    public ResponseEntity<Result<Map<String, Object>>> getMiningStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("enabled", minerService.isMiningEnabled());
            status.put("poolStats", minerService.getPoolStats());
            return ResponseEntity.ok(Result.success(status));
        } catch (Exception e) {
            log.error("获取挖矿状态失败", e);
            return ResponseEntity.ok(Result.error("获取挖矿状态失败"));
        }
    }

    /**
     * 启用/禁用挖矿
     *
     * @param enabled 是否启用
     * @return 操作结果
     */
    @PostMapping("/mining/toggle")
    public ResponseEntity<Result<Map<String, Object>>> toggleMining(@RequestParam boolean enabled) {
        try {
            if (enabled) {
                minerService.enableMining();
            } else {
                minerService.disableMining();
            }

            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("enabled", enabled);
            data.put("message", enabled ? "挖矿已启用" : "挖矿已禁用");
            return ResponseEntity.ok(Result.success(data));
        } catch (Exception e) {
            log.error("切换挖矿状态失败", e);
            return ResponseEntity.ok(Result.error("切换挖矿状态失败"));
        }
    }
}
