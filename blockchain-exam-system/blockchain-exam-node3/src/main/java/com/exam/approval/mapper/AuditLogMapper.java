package com.exam.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.approval.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志Mapper接口
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
