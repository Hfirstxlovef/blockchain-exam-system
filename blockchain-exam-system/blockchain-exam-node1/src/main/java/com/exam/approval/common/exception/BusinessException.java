package com.exam.approval.common.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Getter
public class BusinessException extends RuntimeException {

    private Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
}
