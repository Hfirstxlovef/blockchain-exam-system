package com.blockchain.exam.p2p.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blockchain.exam.p2p.entity.P2PNode;
import com.blockchain.exam.p2p.mapper.P2PNodeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * P2P节点管理服务
 *
 * 负责P2P网络中节点的管理：
 * - 节点注册和发现
 * - 心跳检测
 * - 状态管理
 * - 超时检测
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@Service
public class P2PNodeService {

    @Autowired
    private P2PNodeMapper p2pNodeMapper;

    /**
     * 当前节点ID
     */
    @Value("${p2p.node.id}")
    private String currentNodeId;

    /**
     * 当前节点名称
     */
    @Value("${p2p.node.name}")
    private String currentNodeName;

    /**
     * 节点超时时间（分钟）
     */
    private static final int TIMEOUT_MINUTES = 5;

    /**
     * 初始化当前节点
     * 在应用启动时注册当前节点为在线状态
     */
    @PostConstruct
    public void initCurrentNode() {
        try {
            P2PNode currentNode = getNodeById(currentNodeId);
            if (currentNode != null) {
                currentNode.markAsOnline();
                p2pNodeMapper.updateById(currentNode);
                log.info("当前节点已上线 - {}: {}", currentNodeId, currentNodeName);
            } else {
                log.warn("当前节点不存在于数据库中 - {}", currentNodeId);
            }
        } catch (Exception e) {
            log.error("初始化当前节点失败", e);
        }
    }

    /**
     * 心跳任务
     * 每分钟更新一次当前节点的在线时间
     */
    @Scheduled(fixedRate = 60000)
    public void heartbeat() {
        try {
            p2pNodeMapper.updateLastSeenTime(currentNodeId);
            log.debug("心跳更新 - {}", currentNodeId);
        } catch (Exception e) {
            log.error("心跳更新失败", e);
        }
    }

    /**
     * 超时检测任务
     * 每5分钟检查一次，将超时的节点标记为离线
     */
    @Scheduled(fixedRate = 300000)
    public void detectTimeoutNodes() {
        try {
            List<P2PNode> timeoutNodes = p2pNodeMapper.selectTimeoutNodes(TIMEOUT_MINUTES);
            for (P2PNode node : timeoutNodes) {
                if (!node.getNodeId().equals(currentNodeId)) {
                    node.markAsOffline();
                    p2pNodeMapper.updateById(node);
                    log.warn("节点超时离线 - {}: {}", node.getNodeId(), node.getNodeName());
                }
            }
        } catch (Exception e) {
            log.error("超时检测失败", e);
        }
    }

    /**
     * 根据节点ID获取节点
     *
     * @param nodeId 节点ID
     * @return P2P节点
     */
    public P2PNode getNodeById(String nodeId) {
        return p2pNodeMapper.selectByNodeId(nodeId);
    }

    /**
     * 获取所有在线节点
     *
     * @return 在线节点列表
     */
    public List<P2PNode> getOnlineNodes() {
        return p2pNodeMapper.selectOnlineNodes();
    }

    /**
     * 获取所有离线节点
     *
     * @return 离线节点列表
     */
    public List<P2PNode> getOfflineNodes() {
        return p2pNodeMapper.selectOfflineNodes();
    }

    /**
     * 获取所有节点
     *
     * @return 所有节点列表
     */
    public List<P2PNode> getAllNodes() {
        return p2pNodeMapper.selectList(new QueryWrapper<>());
    }

    /**
     * 获取邻居节点（除当前节点外的所有在线节点）
     *
     * @return 邻居节点列表
     */
    public List<P2PNode> getNeighborNodes() {
        return p2pNodeMapper.selectOnlineNodesExcept(currentNodeId);
    }

    /**
     * 获取邻居节点的URL列表
     *
     * @return URL列表
     */
    public List<String> getNeighborUrls() {
        return getNeighborNodes().stream()
                .map(P2PNode::getNodeUrl)
                .collect(Collectors.toList());
    }

    /**
     * 标记节点为在线
     *
     * @param nodeId 节点ID
     * @return 是否成功
     */
    @Transactional
    public boolean markNodeOnline(String nodeId) {
        try {
            P2PNode node = getNodeById(nodeId);
            if (node != null) {
                node.markAsOnline();
                p2pNodeMapper.updateById(node);
                log.info("节点上线 - {}: {}", nodeId, node.getNodeName());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("标记节点在线失败 - {}", nodeId, e);
            return false;
        }
    }

    /**
     * 标记节点为离线
     *
     * @param nodeId 节点ID
     * @return 是否成功
     */
    @Transactional
    public boolean markNodeOffline(String nodeId) {
        try {
            P2PNode node = getNodeById(nodeId);
            if (node != null) {
                node.markAsOffline();
                p2pNodeMapper.updateById(node);
                log.info("节点离线 - {}: {}", nodeId, node.getNodeName());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("标记节点离线失败 - {}", nodeId, e);
            return false;
        }
    }

    /**
     * 获取网络统计信息
     *
     * @return 统计信息Map
     */
    public java.util.Map<String, Object> getNetworkStats() {
        Long onlineCount = p2pNodeMapper.countOnlineNodes();
        Long totalCount = p2pNodeMapper.selectCount(new QueryWrapper<>());

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("currentNodeId", currentNodeId);
        stats.put("currentNodeName", currentNodeName);
        stats.put("totalNodes", totalCount);
        stats.put("onlineNodes", onlineCount);
        stats.put("offlineNodes", totalCount - onlineCount);
        stats.put("neighborCount", onlineCount - 1); // 排除当前节点

        return stats;
    }

    /**
     * 检查节点是否在线
     *
     * @param nodeId 节点ID
     * @return 是否在线
     */
    public boolean isNodeOnline(String nodeId) {
        P2PNode node = getNodeById(nodeId);
        return node != null && node.isOnline();
    }

    /**
     * 获取当前节点ID
     *
     * @return 当前节点ID
     */
    public String getCurrentNodeId() {
        return currentNodeId;
    }

    /**
     * 获取当前节点信息
     *
     * @return 当前节点
     */
    public P2PNode getCurrentNode() {
        return getNodeById(currentNodeId);
    }
}
