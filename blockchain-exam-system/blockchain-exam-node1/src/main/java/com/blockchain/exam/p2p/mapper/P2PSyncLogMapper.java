package com.blockchain.exam.p2p.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blockchain.exam.p2p.entity.P2PSyncLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * P2P同步日志Mapper接口
 *
 * 提供P2P同步日志数据的数据库访问操作
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Mapper
public interface P2PSyncLogMapper extends BaseMapper<P2PSyncLog> {

    /**
     * 查询指定节点的最新同步日志
     *
     * @param sourceNode 源节点ID
     * @param targetNode 目标节点ID
     * @param syncType   同步类型
     * @return 最新的同步日志
     */
    @Select("SELECT * FROM p2p_sync_log WHERE source_node = #{sourceNode} AND target_node = #{targetNode} AND sync_type = #{syncType} ORDER BY sync_time DESC LIMIT 1")
    P2PSyncLog selectLatestLog(String sourceNode, String targetNode, String syncType);

    /**
     * 查询指定节点的同步历史
     *
     * @param sourceNode 源节点ID
     * @param targetNode 目标节点ID
     * @return 同步日志列表
     */
    @Select("SELECT * FROM p2p_sync_log WHERE source_node = #{sourceNode} AND target_node = #{targetNode} ORDER BY sync_time DESC")
    List<P2PSyncLog> selectSyncHistory(String sourceNode, String targetNode);

    /**
     * 查询指定节点的所有同步日志
     *
     * @param nodeId 节点ID（作为源节点或目标节点）
     * @return 同步日志列表
     */
    @Select("SELECT * FROM p2p_sync_log WHERE source_node = #{nodeId} OR target_node = #{nodeId} ORDER BY sync_time DESC")
    List<P2PSyncLog> selectByNode(String nodeId);

    /**
     * 查询失败的同步日志
     *
     * @return 失败的同步日志列表
     */
    @Select("SELECT * FROM p2p_sync_log WHERE sync_status = 'FAILURE' ORDER BY sync_time DESC")
    List<P2PSyncLog> selectFailedLogs();

    /**
     * 查询正在进行的同步
     *
     * @return 正在进行的同步列表
     */
    @Select("SELECT * FROM p2p_sync_log WHERE sync_status = 'IN_PROGRESS' ORDER BY sync_time DESC")
    List<P2PSyncLog> selectInProgressLogs();

    /**
     * 查询指定时间范围内的同步日志
     *
     * @param startTime 开始时间（格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime   结束时间（格式：yyyy-MM-dd HH:mm:ss）
     * @return 同步日志列表
     */
    @Select("SELECT * FROM p2p_sync_log WHERE sync_time BETWEEN #{startTime} AND #{endTime} ORDER BY sync_time DESC")
    List<P2PSyncLog> selectByTimeRange(String startTime, String endTime);

    /**
     * 统计成功的同步次数
     *
     * @param sourceNode 源节点ID
     * @param targetNode 目标节点ID
     * @return 成功次数
     */
    @Select("SELECT COUNT(*) FROM p2p_sync_log WHERE source_node = #{sourceNode} AND target_node = #{targetNode} AND sync_status = 'SUCCESS'")
    Long countSuccessLogs(String sourceNode, String targetNode);

    /**
     * 统计失败的同步次数
     *
     * @param sourceNode 源节点ID
     * @param targetNode 目标节点ID
     * @return 失败次数
     */
    @Select("SELECT COUNT(*) FROM p2p_sync_log WHERE source_node = #{sourceNode} AND target_node = #{targetNode} AND sync_status = 'FAILURE'")
    Long countFailureLogs(String sourceNode, String targetNode);
}
