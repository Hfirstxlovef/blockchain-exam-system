package com.exam.approval.security.util;

import cn.hutool.core.codec.Base64;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * RSA-2048 加密和数字签名工具类
 *
 * 功能：
 * - RSA-2048非对称加密/解密
 * - RSA-SHA256数字签名/验证
 * - 支持密钥对生成
 *
 * 用途：
 * - 加密会话密钥（前后端协商AES密钥）
 * - 审批操作数字签名（不可抵赖）
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Slf4j
public class RSAUtil {

    private static final String ALGORITHM = "RSA";
    private static final String TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int KEY_SIZE = 2048;

    public static final String PUBLIC_KEY = "publicKey";
    public static final String PRIVATE_KEY = "privateKey";

    /**
     * 生成RSA密钥对
     *
     * @return Map包含publicKey和privateKey（Base64编码）
     */
    public static Map<String, String> generateKeyPair() {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyPairGen.initialize(KEY_SIZE, new SecureRandom());
            KeyPair keyPair = keyPairGen.generateKeyPair();

            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            Map<String, String> keyMap = new HashMap<>();
            keyMap.put(PUBLIC_KEY, Base64.encode(publicKey.getEncoded()));
            keyMap.put(PRIVATE_KEY, Base64.encode(privateKey.getEncoded()));

            return keyMap;
        } catch (Exception e) {
            log.error("生成RSA密钥对失败", e);
            throw new RuntimeException("生成RSA密钥对失败", e);
        }
    }

    /**
     * 公钥加密
     *
     * @param plainText     明文
     * @param publicKeyStr  Base64编码的公钥
     * @return Base64编码的密文
     */
    public static String encryptByPublicKey(String plainText, String publicKeyStr) {
        try {
            byte[] keyBytes = Base64.decode(publicKeyStr);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return Base64.encode(encryptedBytes);
        } catch (Exception e) {
            log.error("RSA公钥加密失败", e);
            throw new RuntimeException("RSA公钥加密失败", e);
        }
    }

    /**
     * 私钥解密（OAEP padding）
     *
     * @param cipherText    Base64编码的密文
     * @param privateKeyStr Base64编码的私钥
     * @return 明文
     */
    public static String decryptByPrivateKey(String cipherText, String privateKeyStr) {
        try {
            byte[] keyBytes = Base64.decode(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.decode(cipherText));

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("RSA私钥解密失败", e);
            throw new RuntimeException("RSA私钥解密失败", e);
        }
    }

    /**
     * 私钥解密（PKCS1 padding，用于前端JSEncrypt加密的数据）
     * JSEncrypt库默认使用PKCS1 padding，与OAEP不兼容
     *
     * @param cipherText    Base64编码的密文
     * @param privateKeyStr Base64编码的私钥
     * @return 明文
     */
    public static String decryptByPrivateKeyPKCS1(String cipherText, String privateKeyStr) {
        try {
            byte[] keyBytes = Base64.decode(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            // 使用PKCS1 padding
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.decode(cipherText));

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("RSA私钥解密失败（PKCS1）", e);
            throw new RuntimeException("RSA私钥解密失败（PKCS1）", e);
        }
    }

    /**
     * 私钥签名
     * 用于审批操作的数字签名，确保不可抵赖
     *
     * @param data          待签名数据
     * @param privateKeyStr Base64编码的私钥
     * @return Base64编码的签名
     */
    public static String sign(String data, String privateKeyStr) {
        try {
            byte[] keyBytes = Base64.decode(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signBytes = signature.sign();

            return Base64.encode(signBytes);
        } catch (Exception e) {
            log.error("RSA签名失败", e);
            throw new RuntimeException("RSA签名失败", e);
        }
    }

    /**
     * 公钥验证签名
     *
     * @param data         原始数据
     * @param signStr      Base64编码的签名
     * @param publicKeyStr Base64编码的公钥
     * @return 验证结果
     */
    public static boolean verify(String data, String signStr, String publicKeyStr) {
        try {
            byte[] keyBytes = Base64.decode(publicKeyStr);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));

            return signature.verify(Base64.decode(signStr));
        } catch (Exception e) {
            log.error("RSA签名验证失败", e);
            return false;
        }
    }

    /**
     * 生成审批签名数据
     * 将审批信息组合成待签名字符串
     *
     * @param paperId      试卷ID
     * @param approverId   审批人ID
     * @param action       操作（approve/reject）
     * @param comment      审批意见
     * @param timestamp    时间戳
     * @return 待签名数据字符串
     */
    public static String buildApprovalSignData(Long paperId, Long approverId,
                                               String action, String comment, long timestamp) {
        return String.format("paper=%d&approver=%d&action=%s&comment=%s&timestamp=%d",
                paperId, approverId, action, comment, timestamp);
    }

    /**
     * 构建试卷创建签名数据
     *
     * @param contentHash 试卷内容哈希
     * @param creatorId   创建者ID
     * @param timestamp   时间戳
     * @return 待签名数据字符串
     */
    public static String buildPaperSignData(String contentHash, Long creatorId, long timestamp) {
        return String.format("hash=%s&creator=%d&timestamp=%d",
                contentHash, creatorId, timestamp);
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        System.out.println("=== RSA密钥对生成 ===");
        Map<String, String> keyPair = generateKeyPair();
        String publicKey = keyPair.get(PUBLIC_KEY);
        String privateKey = keyPair.get(PRIVATE_KEY);
        System.out.println("公钥: " + publicKey.substring(0, 50) + "...");
        System.out.println("私钥: " + privateKey.substring(0, 50) + "...");

        System.out.println("\n=== RSA加密解密测试 ===");
        String plainText = "这是一个AES会话密钥: aGVsbG8td29ybGQ=";
        System.out.println("原文: " + plainText);

        String encrypted = encryptByPublicKey(plainText, publicKey);
        System.out.println("密文: " + encrypted.substring(0, 50) + "...");

        String decrypted = decryptByPrivateKey(encrypted, privateKey);
        System.out.println("解密: " + decrypted);
        System.out.println("加解密是否一致: " + plainText.equals(decrypted));

        System.out.println("\n=== RSA数字签名测试 ===");
        String approvalData = buildApprovalSignData(1001L, 2001L, "approve",
                "同意通过", System.currentTimeMillis());
        System.out.println("审批数据: " + approvalData);

        String signature = sign(approvalData, privateKey);
        System.out.println("签名: " + signature.substring(0, 50) + "...");

        boolean verifyResult = verify(approvalData, signature, publicKey);
        System.out.println("签名验证: " + verifyResult);

        // 测试篡改检测
        String tamperedData = approvalData.replace("approve", "reject");
        boolean tamperedVerify = verify(tamperedData, signature, publicKey);
        System.out.println("篡改后验证: " + tamperedVerify + " (应为false)");
    }
}
