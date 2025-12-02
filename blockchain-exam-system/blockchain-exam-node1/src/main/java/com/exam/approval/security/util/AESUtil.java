package com.exam.approval.security.util;

import cn.hutool.core.codec.Base64;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * AES-256-GCM 加密工具类
 * 使用AES-GCM模式提供认证加密（Authenticated Encryption）
 *
 * 特性：
 * - AES-256位密钥
 * - GCM模式（Galois/Counter Mode）
 * - 自动生成随机IV
 * - 带认证标签（防篡改）
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Slf4j
public class AESUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256; // AES-256
    private static final int GCM_IV_LENGTH = 12; // GCM标准IV长度
    private static final int GCM_TAG_LENGTH = 128; // 认证标签长度（位）

    /**
     * 生成AES密钥
     *
     * @return Base64编码的密钥字符串
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_SIZE, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            return Base64.encode(secretKey.getEncoded());
        } catch (Exception e) {
            log.error("生成AES密钥失败", e);
            throw new RuntimeException("生成AES密钥失败", e);
        }
    }

    /**
     * 加密数据
     *
     * @param plainText 明文
     * @param key       Base64编码的密钥
     * @return Base64编码的密文（包含IV和认证标签）
     */
    public static String encrypt(String plainText, String key) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // 解码密钥
            byte[] keyBytes = Base64.decode(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // 初始化Cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 加密
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 将IV和密文组合：IV(12字节) + 密文
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);

            return Base64.encode(byteBuffer.array());
        } catch (Exception e) {
            log.error("AES加密失败", e);
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * 解密数据
     *
     * @param cipherText Base64编码的密文（包含IV）
     * @param key        Base64编码的密钥
     * @return 明文
     */
    public static String decrypt(String cipherText, String key) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }

        try {
            // 解码密钥
            byte[] keyBytes = Base64.decode(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // 解码密文
            byte[] decodedData = Base64.decode(cipherText);

            // 提取IV和密文
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encryptedBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedBytes);

            // 初始化Cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 解密
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("AES解密失败", e);
            throw new RuntimeException("AES解密失败", e);
        }
    }

    /**
     * 加密字节数组（用于文件加密）
     *
     * @param data 原始数据
     * @param key  Base64编码的密钥
     * @return 加密后的数据（包含IV）
     */
    public static byte[] encryptBytes(byte[] data, String key) {
        try {
            byte[] keyBytes = Base64.decode(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] encryptedBytes = cipher.doFinal(data);

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);

            return byteBuffer.array();
        } catch (Exception e) {
            log.error("AES字节加密失败", e);
            throw new RuntimeException("AES字节加密失败", e);
        }
    }

    /**
     * AES-256-CBC 解密（用于前端会话加密）
     * 与前端CryptoJS的AES-CBC模式兼容
     *
     * @param cipherText Base64编码的密文（包含IV）
     * @param key        Base64编码的密钥
     * @return 明文
     */
    public static String decryptCBC(String cipherText, String key) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }

        try {
            // 解码密钥
            byte[] keyBytes = Base64.decode(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // 解码密文
            byte[] decodedData = Base64.decode(cipherText);

            // 提取IV（前16字节，CBC模式标准）
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedData);
            byte[] iv = new byte[16];  // CBC模式IV长度为16字节
            byteBuffer.get(iv);
            byte[] encryptedBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedBytes);

            // 初始化Cipher（CBC模式）
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            // 解密
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("AES-CBC解密失败", e);
            throw new RuntimeException("AES-CBC解密失败", e);
        }
    }

    /**
     * 解密字节数组（用于文件解密）
     *
     * @param encryptedData 加密的数据（包含IV）
     * @param key           Base64编码的密钥
     * @return 原始数据
     */
    public static byte[] decryptBytes(byte[] encryptedData, String key) {
        try {
            byte[] keyBytes = Base64.decode(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            return cipher.doFinal(cipherBytes);
        } catch (Exception e) {
            log.error("AES字节解密失败", e);
            throw new RuntimeException("AES字节解密失败", e);
        }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        // 生成密钥
        String key = generateKey();
        System.out.println("生成的AES密钥: " + key);

        // 测试加密解密
        String plainText = "这是一段需要加密的试卷内容：试题一、请简述计算机网络的OSI七层模型。";
        System.out.println("原文: " + plainText);

        String encrypted = encrypt(plainText, key);
        System.out.println("密文: " + encrypted);

        String decrypted = decrypt(encrypted, key);
        System.out.println("解密: " + decrypted);

        System.out.println("加解密是否一致: " + plainText.equals(decrypted));
    }
}
