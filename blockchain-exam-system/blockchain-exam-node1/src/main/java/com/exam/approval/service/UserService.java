package com.exam.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.approval.entity.User;
import com.exam.approval.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户Service
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Slf4j
@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户对象
     */
    public User getUserByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, 0));
    }

    /**
     * 根据ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象
     */
    public User getUserById(Long userId) {
        return this.getById(userId);
    }

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    public boolean existsByUsername(String username) {
        return this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, 0)) > 0;
    }

    /**
     * 根据部门和角色查询用户
     *
     * @param department 部门
     * @param role 角色
     * @return 用户对象（如有多个返回第一个）
     */
    public User findByDepartmentAndRole(String department, String role) {
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getDepartment, department)
                .eq(User::getRole, role)
                .eq(User::getDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 根据角色查询所有用户
     *
     * @param role 角色
     * @return 用户列表
     */
    public java.util.List<User> findByRole(String role) {
        return this.list(new LambdaQueryWrapper<User>()
                .eq(User::getRole, role)
                .eq(User::getDeleted, 0));
    }
}
