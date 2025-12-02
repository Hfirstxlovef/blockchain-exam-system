package com.exam.approval.dto;

import lombok.Data;

/**
 * 试卷解密请求DTO
 * 用于前端传递私钥解密试卷内容
 *
 * @author 网络信息安全大作业
 * @date 2025-11-28
 */
@Data
public class PaperDecryptRequest {

    /**
     * 试卷ID
     */
    private Long paperId;

    /**
     * 用户RSA私钥（Base64编码）
     * 前端从导出的PEM文件中读取
     */
    private String privateKey;
}
