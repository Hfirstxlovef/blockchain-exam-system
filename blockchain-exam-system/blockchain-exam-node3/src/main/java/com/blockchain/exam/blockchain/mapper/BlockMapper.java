package com.blockchain.exam.blockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blockchain.exam.blockchain.entity.Block;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 区块Mapper接口
 *
 * 提供区块链数据的数据库访问操作
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Mapper
public interface BlockMapper extends BaseMapper<Block> {

    /**
     * 获取最新区块
     * 按区块高度降序排序，获取第一条记录
     *
     * @return 最新区块，如果区块链为空则返回null
     */
    @Select("SELECT * FROM ${tableName} ORDER BY block_index DESC LIMIT 1")
    Block selectLatestBlock(String tableName);

    /**
     * 根据区块高度查询区块
     *
     * @param blockIndex 区块高度
     * @return 指定高度的区块
     */
    @Select("SELECT * FROM ${tableName} WHERE block_index = #{blockIndex}")
    Block selectByBlockIndex(String tableName, Long blockIndex);

    /**
     * 根据区块哈希查询区块
     *
     * @param currentHash 区块哈希
     * @return 指定哈希的区块
     */
    @Select("SELECT * FROM ${tableName} WHERE current_hash = #{currentHash}")
    Block selectByHash(String tableName, String currentHash);

    /**
     * 获取区块链中所有区块（按高度升序）
     *
     * @return 所有区块列表
     */
    @Select("SELECT * FROM ${tableName} ORDER BY block_index ASC")
    List<Block> selectAllBlocks(String tableName);

    /**
     * 获取指定范围的区块
     *
     * @param startIndex 起始高度
     * @param endIndex   结束高度
     * @return 指定范围内的区块列表
     */
    @Select("SELECT * FROM ${tableName} WHERE block_index >= #{startIndex} AND block_index <= #{endIndex} ORDER BY block_index ASC")
    List<Block> selectBlockRange(String tableName, Long startIndex, Long endIndex);

    /**
     * 获取区块链高度（最大区块高度）
     *
     * @return 当前区块链高度，如果区块链为空则返回-1
     */
    @Select("SELECT COALESCE(MAX(block_index), -1) FROM ${tableName}")
    Long selectChainHeight(String tableName);
}
