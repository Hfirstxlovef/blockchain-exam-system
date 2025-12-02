package com.blockchain.exam.p2p.controller;

import com.blockchain.exam.p2p.entity.P2PNode;
import com.blockchain.exam.p2p.service.P2PNodeService;
import com.blockchain.exam.p2p.service.P2PSyncService;
import com.exam.approval.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * P2P控制器
 *
 * 提供P2P网络相关的REST API
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@RestController
@RequestMapping("/p2p")
public class P2PController {

    @Autowired
    private P2PNodeService nodeService;

    @Autowired
    private P2PSyncService syncService;

    /**
     * 获取所有节点
     *
     * @return 节点列表
     */
    @GetMapping("/nodes")
    public Result<List<P2PNode>> getAllNodes() {
        try {
            List<P2PNode> nodes = nodeService.getAllNodes();
            return Result.success(nodes);
        } catch (Exception e) {
            log.error("获取节点列表失败", e);
            return Result.error("获取节点列表失败");
        }
    }

    /**
     * 获取在线节点
     *
     * @return 在线节点列表
     */
    @GetMapping("/nodes/online")
    public Result<List<P2PNode>> getOnlineNodes() {
        try {
            List<P2PNode> nodes = nodeService.getOnlineNodes();
            return Result.success(nodes);
        } catch (Exception e) {
            log.error("获取在线节点失败", e);
            return Result.error("获取在线节点失败");
        }
    }

    /**
     * 获取邻居节点
     *
     * @return 邻居节点列表
     */
    @GetMapping("/nodes/neighbors")
    public Result<List<P2PNode>> getNeighborNodes() {
        try {
            List<P2PNode> neighbors = nodeService.getNeighborNodes();
            return Result.success(neighbors);
        } catch (Exception e) {
            log.error("获取邻居节点失败", e);
            return Result.error("获取邻居节点失败");
        }
    }

    /**
     * 获取当前节点信息
     *
     * @return 当前节点
     */
    @GetMapping("/nodes/current")
    public Result<P2PNode> getCurrentNode() {
        try {
            P2PNode currentNode = nodeService.getCurrentNode();
            if (currentNode == null) {
                return Result.notFound("当前节点不存在");
            }
            return Result.success(currentNode);
        } catch (Exception e) {
            log.error("获取当前节点信息失败", e);
            return Result.error("获取当前节点信息失败");
        }
    }

    /**
     * 获取网络统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/network/stats")
    public Result<Map<String, Object>> getNetworkStats() {
        try {
            Map<String, Object> stats = nodeService.getNetworkStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取网络统计失败", e);
            return Result.error("获取网络统计失败");
        }
    }

    /**
     * 获取同步统计信息
     *
     * @return 同步统计
     */
    @GetMapping("/sync/stats")
    public Result<Map<String, Object>> getSyncStats() {
        try {
            Map<String, Object> stats = syncService.getSyncStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取同步统计失败", e);
            return Result.error("获取同步统计失败");
        }
    }

    /**
     * 启用/禁用自动同步
     *
     * @param enabled 是否启用
     * @return 操作结果
     */
    @PostMapping("/sync/toggle")
    public Result<Map<String, Object>> toggleSync(@RequestParam boolean enabled) {
        try {
            if (enabled) {
                syncService.enableSync();
            } else {
                syncService.disableSync();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("enabled", enabled);
            result.put("message", enabled ? "自动同步已启用" : "自动同步已禁用");
            return Result.success(result);
        } catch (Exception e) {
            log.error("切换同步状态失败", e);
            return Result.error("切换同步状态失败");
        }
    }

    /**
     * 心跳检测端点
     * 用于其他节点检测本节点是否在线
     *
     * @return 心跳响应
     */
    @GetMapping("/heartbeat")
    public Result<Map<String, Object>> heartbeat() {
        Map<String, Object> response = new HashMap<>();
        response.put("nodeId", nodeService.getCurrentNodeId());
        response.put("status", "ONLINE");
        response.put("timestamp", System.currentTimeMillis());
        return Result.success(response);
    }

    /**
     * 健康检查端点
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("nodeId", nodeService.getCurrentNodeId());
            health.put("neighbors", nodeService.getNeighborNodes().size());
            health.put("syncEnabled", syncService.isSyncEnabled());
            return Result.success(health);
        } catch (Exception e) {
            log.error("健康检查失败", e);
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return Result.error(503, "健康检查失败", health);
        }
    }
}
