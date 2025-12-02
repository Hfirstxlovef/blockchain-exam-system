package com.exam.approval.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 试卷状态枚举
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Getter
@AllArgsConstructor
public enum PaperStatus {
    DRAFT("draft", "草稿"),
    PENDING("pending", "待审批"),
    DEPT_APPROVED("dept_approved", "系已审批"),
    COLLEGE_APPROVED("college_approved", "院已审批"),
    REJECTED("rejected", "已驳回");

    private final String code;
    private final String name;

    public static PaperStatus fromCode(String code) {
        for (PaperStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知状态: " + code);
    }
}
