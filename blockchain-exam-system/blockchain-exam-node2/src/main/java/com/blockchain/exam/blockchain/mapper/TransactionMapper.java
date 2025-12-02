package com.blockchain.exam.blockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blockchain.exam.blockchain.entity.Transaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 交易池Mapper接口
 *
 * 提供交易池数据的数据库访问操作
 * 交易池存储待打包的交易，矿工从池中获取交易进行打包
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Mapper
public interface TransactionMapper extends BaseMapper<Transaction> {

    /**
     * 查询所有待打包的交易
     * 按创建时间升序排序，优先打包早创建的交易
     *
     * @return 待打包的交易列表
     */
    @Select("SELECT * FROM ${tableName} WHERE status = 'PENDING' ORDER BY create_time ASC")
    List<Transaction> selectPendingTransactions(String tableName);

    /**
     * 查询指定数量的待打包交易
     * 用于矿工批量打包交易
     *
     * @param limit 最多获取的交易数量
     * @return 待打包的交易列表
     */
    @Select("SELECT * FROM ${tableName} WHERE status = 'PENDING' ORDER BY create_time ASC LIMIT #{limit}")
    List<Transaction> selectPendingTransactionsLimit(String tableName, int limit);

    /**
     * 根据交易类型查询待打包交易
     *
     * @param transactionType 交易类型
     * @return 指定类型的待打包交易列表
     */
    @Select("SELECT * FROM ${tableName} WHERE status = 'PENDING' AND transaction_type = #{transactionType} ORDER BY create_time ASC")
    List<Transaction> selectPendingByType(String tableName, String transactionType);

    /**
     * 查询已打包到指定区块的交易
     *
     * @param blockIndex 区块高度
     * @return 指定区块中的交易列表
     */
    @Select("SELECT * FROM ${tableName} WHERE status = 'MINED' AND block_index = #{blockIndex}")
    List<Transaction> selectByBlockIndex(String tableName, Long blockIndex);

    /**
     * 批量更新交易状态为已打包
     *
     * @param ids         交易ID列表
     * @param blockIndex  区块高度
     * @return 更新的记录数
     */
    @Update("<script>" +
            "UPDATE ${tableName} SET status = 'MINED', block_index = #{blockIndex} " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateToMined(String tableName, List<Long> ids, Long blockIndex);

    /**
     * 批量更新交易状态为无效
     *
     * @param ids 交易ID列表
     * @return 更新的记录数
     */
    @Update("<script>" +
            "UPDATE ${tableName} SET status = 'INVALID' " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateToInvalid(String tableName, List<Long> ids);

    /**
     * 获取交易池中待打包交易的数量
     *
     * @return 待打包交易数量
     */
    @Select("SELECT COUNT(*) FROM ${tableName} WHERE status = 'PENDING'")
    Long countPendingTransactions(String tableName);

    /**
     * 根据创建节点查询待打包交易
     *
     * @param creatorNode 创建节点ID
     * @return 指定节点创建的待打包交易列表
     */
    @Select("SELECT * FROM ${tableName} WHERE status = 'PENDING' AND creator_node = #{creatorNode} ORDER BY create_time ASC")
    List<Transaction> selectPendingByCreator(String tableName, String creatorNode);

    /**
     * 从指定表查询所有交易
     *
     * @param tableName 表名
     * @return 交易列表
     */
    @Select("SELECT *, '${tableName}' as source_table FROM ${tableName} ORDER BY create_time DESC")
    List<Transaction> selectAllFromTable(String tableName);

    /**
     * 从所有节点表查询交易（合并查询）
     *
     * @return 所有节点的交易列表
     */
    @Select("SELECT *, 'node1_block_pool' as source_table FROM node1_block_pool " +
            "UNION ALL " +
            "SELECT *, 'node2_block_pool' as source_table FROM node2_block_pool " +
            "UNION ALL " +
            "SELECT *, 'node3_block_pool' as source_table FROM node3_block_pool " +
            "ORDER BY create_time DESC")
    List<Transaction> selectAllFromAllNodes();

    /**
     * 按类型从所有节点表查询交易
     *
     * @param transactionType 交易类型
     * @return 交易列表
     */
    @Select("SELECT *, 'node1_block_pool' as source_table FROM node1_block_pool WHERE transaction_type = #{transactionType} " +
            "UNION ALL " +
            "SELECT *, 'node2_block_pool' as source_table FROM node2_block_pool WHERE transaction_type = #{transactionType} " +
            "UNION ALL " +
            "SELECT *, 'node3_block_pool' as source_table FROM node3_block_pool WHERE transaction_type = #{transactionType} " +
            "ORDER BY create_time DESC")
    List<Transaction> selectByTypeFromAllNodes(String transactionType);

    /**
     * 统计所有节点的交易数量
     *
     * @return 交易总数
     */
    @Select("SELECT " +
            "(SELECT COUNT(*) FROM node1_block_pool) + " +
            "(SELECT COUNT(*) FROM node2_block_pool) + " +
            "(SELECT COUNT(*) FROM node3_block_pool)")
    Long countAllFromAllNodes();
}
