package com.blockchain.exam.p2p.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blockchain.exam.p2p.entity.P2PNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * P2P节点Mapper接口
 *
 * 提供P2P节点数据的数据库访问操作
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Mapper
public interface P2PNodeMapper extends BaseMapper<P2PNode> {

    /**
     * 根据节点ID查询节点
     *
     * @param nodeId 节点ID
     * @return P2P节点
     */
    @Select("SELECT * FROM p2p_node WHERE node_id = #{nodeId}")
    P2PNode selectByNodeId(String nodeId);

    /**
     * 查询所有在线节点
     *
     * @return 在线节点列表
     */
    @Select("SELECT * FROM p2p_node WHERE status = 'ONLINE' ORDER BY node_id")
    List<P2PNode> selectOnlineNodes();

    /**
     * 查询所有离线节点
     *
     * @return 离线节点列表
     */
    @Select("SELECT * FROM p2p_node WHERE status = 'OFFLINE' ORDER BY node_id")
    List<P2PNode> selectOfflineNodes();

    /**
     * 查询除指定节点外的所有在线节点（用于获取邻居节点）
     *
     * @param excludeNodeId 要排除的节点ID
     * @return 在线节点列表
     */
    @Select("SELECT * FROM p2p_node WHERE status = 'ONLINE' AND node_id != #{excludeNodeId} ORDER BY node_id")
    List<P2PNode> selectOnlineNodesExcept(String excludeNodeId);

    /**
     * 更新节点状态为在线
     *
     * @param nodeId 节点ID
     * @return 更新的记录数
     */
    @Update("UPDATE p2p_node SET status = 'ONLINE', last_seen_time = NOW() WHERE node_id = #{nodeId}")
    int updateStatusToOnline(String nodeId);

    /**
     * 更新节点状态为离线
     *
     * @param nodeId 节点ID
     * @return 更新的记录数
     */
    @Update("UPDATE p2p_node SET status = 'OFFLINE' WHERE node_id = #{nodeId}")
    int updateStatusToOffline(String nodeId);

    /**
     * 更新节点的最后在线时间
     *
     * @param nodeId 节点ID
     * @return 更新的记录数
     */
    @Update("UPDATE p2p_node SET last_seen_time = NOW() WHERE node_id = #{nodeId}")
    int updateLastSeenTime(String nodeId);

    /**
     * 统计在线节点数量
     *
     * @return 在线节点数量
     */
    @Select("SELECT COUNT(*) FROM p2p_node WHERE status = 'ONLINE'")
    Long countOnlineNodes();

    /**
     * 查询超时的节点（超过指定分钟数未见）
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 超时的节点列表
     */
    @Select("SELECT * FROM p2p_node WHERE status = 'ONLINE' AND last_seen_time < DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    List<P2PNode> selectTimeoutNodes(int timeoutMinutes);
}
