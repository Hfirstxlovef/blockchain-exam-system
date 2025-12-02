package com.exam.approval.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色枚举
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Getter
@AllArgsConstructor
public enum UserRole {
    TEACHER("teacher", "教师"),
    DEPT_ADMIN("dept_admin", "系管理员"),
    COLLEGE_ADMIN("college_admin", "院管理员");

    private final String code;
    private final String name;

    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知角色: " + code);
    }
}
