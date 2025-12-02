package com.exam.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.approval.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // MyBatis-Plus已提供基础CRUD方法
    // 如需自定义SQL，可在此添加方法
}
