package com.exam.approval.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 试卷加密请求DTO
 * 接收前端发送的加密数据
 *
 * 加密方案：
 * - 小字段（courseName, examType, semester, department, filePath）使用RSA加密
 * - 大字段（content）使用AES加密
 * - AES密钥使用RSA加密传输
 * - 包含timestamp和nonce防重放
 * - 包含HMAC签名验证完整性
 *
 * @author 网络信息安全大作业
 * @date 2025-11-16
 */
@Slf4j
@Data
public class PaperEncryptedRequest {

    /**
     * 加密的课程名称（RSA加密，Base64编码）
     */
    private String courseName;

    /**
     * 加密的考试类型（RSA加密，Base64编码）
     */
    private String examType;

    /**
     * 加密的学期（RSA加密，Base64编码）
     */
    private String semester;

    /**
     * 加密的院系（RSA加密，Base64编码）
     */
    private String department;

    /**
     * 加密的试卷内容（AES加密，Base64编码）
     */
    private String content;

    /**
     * 加密的文件路径（RSA加密，Base64编码，可选）
     */
    private String filePath;

    /**
     * 加密的AES密钥（RSA加密，Base64编码）
     */
    private String encryptedAesKey;

    /**
     * 时间戳（防重放）
     */
    private Long timestamp;

    /**
     * 随机数（防重放）
     */
    private String nonce;

    /**
     * HMAC签名（完整性验证）
     */
    private String hmac;

    /**
     * 用户RSA私钥（Base64编码，用于数字签名验证）
     * 用于验证试卷创建者身份，确保不可抵赖
     */
    private String privateKey;

    /**
     * 获取待签名数据（用于HMAC验证）
     * 使用ObjectMapper确保与前端JSON.stringify格式一致
     *
     * @return JSON格式的待签名数据
     */
    @JsonIgnore
    public String toSignData() {
        try {
            // 使用LinkedHashMap保证字段顺序与前端一致
            Map<String, Object> signMap = new LinkedHashMap<>();
            signMap.put("courseName", courseName != null ? courseName : "");
            signMap.put("examType", examType != null ? examType : "");
            signMap.put("semester", semester != null ? semester : "");
            signMap.put("department", department != null ? department : "");
            signMap.put("content", content != null ? content : "");
            signMap.put("filePath", filePath != null ? filePath : "");
            signMap.put("encryptedAesKey", encryptedAesKey != null ? encryptedAesKey : "");
            signMap.put("timestamp", timestamp != null ? timestamp : 0);
            signMap.put("nonce", nonce != null ? nonce : "");

            // 使用ObjectMapper序列化为JSON字符串
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonData = objectMapper.writeValueAsString(signMap);

            log.debug("待签名数据: {}", jsonData.substring(0, Math.min(100, jsonData.length())) + "...");
            return jsonData;

        } catch (JsonProcessingException e) {
            log.error("生成签名数据失败", e);
            throw new RuntimeException("生成签名数据失败", e);
        }
    }
}
