package com.exam.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.approval.entity.PaperDecryptRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 试卷解密记录Mapper接口
 *
 * @author 网络信息安全大作业
 * @date 2025-11-29
 */
@Mapper
public interface PaperDecryptRecordMapper extends BaseMapper<PaperDecryptRecord> {

    /**
     * 查询试卷的所有解密记录（按时间倒序）
     *
     * @param paperId 试卷ID
     * @return 解密记录列表
     */
    @Select("SELECT * FROM paper_decrypt_record WHERE paper_id = #{paperId} ORDER BY decrypt_time DESC")
    List<PaperDecryptRecord> findByPaperId(@Param("paperId") Long paperId);

    /**
     * 查询用户的所有解密记录
     *
     * @param userId 用户ID
     * @return 解密记录列表
     */
    @Select("SELECT * FROM paper_decrypt_record WHERE user_id = #{userId} ORDER BY decrypt_time DESC")
    List<PaperDecryptRecord> findByUserId(@Param("userId") Long userId);

    /**
     * 统计解密记录总数
     *
     * @return 记录总数
     */
    @Select("SELECT COUNT(*) FROM paper_decrypt_record")
    Long countAll();

    /**
     * 查询所有解密记录（分页，按时间倒序）
     *
     * @param offset 偏移量
     * @param limit 数量
     * @return 解密记录列表
     */
    @Select("SELECT * FROM paper_decrypt_record ORDER BY decrypt_time DESC LIMIT #{offset}, #{limit}")
    List<PaperDecryptRecord> findAllPaged(@Param("offset") int offset, @Param("limit") int limit);
}
