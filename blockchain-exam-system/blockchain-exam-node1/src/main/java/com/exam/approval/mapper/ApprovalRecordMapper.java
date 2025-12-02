package com.exam.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.approval.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批记录Mapper接口
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecord> {
}
