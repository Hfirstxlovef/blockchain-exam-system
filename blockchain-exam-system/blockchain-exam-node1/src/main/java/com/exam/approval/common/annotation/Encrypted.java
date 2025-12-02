package com.exam.approval.common.annotation;

import java.lang.annotation.*;

/**
 * 加密字段注解
 * 标记需要加密存储的数据库字段
 *
 * 使用示例：
 * @Encrypted
 * private String realName;
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Encrypted {
    /**
     * 是否加密（默认true）
     */
    boolean value() default true;
}
