package com.exam.approval.common.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解
 * 标记需要记录操作日志的方法
 *
 * 使用示例：
 * @AuditLog(operation = "创建试卷", resourceType = "exam_paper")
 * public void createPaper(ExamPaper paper) { ... }
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    /**
     * 操作描述
     */
    String operation() default "";

    /**
     * 资源类型
     */
    String resourceType() default "";
}
