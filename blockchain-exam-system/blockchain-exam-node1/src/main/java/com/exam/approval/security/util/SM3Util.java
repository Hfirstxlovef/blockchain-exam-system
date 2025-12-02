package com.exam.approval.security.util;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.digest.SM3;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * 国密SM3哈希工具类
 *
 * SM3是中国国家密码管理局发布的密码杂凑算法标准
 * - 输出长度：256位（32字节）
 * - 安全强度：与SHA-256相当
 *
 * 用途：
 * - 数据完整性校验
 * - 敏感字段哈希索引
 * - HMAC消息认证
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Slf4j
public class SM3Util {

    /**
     * SM3哈希（返回16进制字符串）
     *
     * @param data 原始数据
     * @return 16进制哈希值
     */
    public static String hash(String data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        try {
            SM3 sm3 = SmUtil.sm3();
            return sm3.digestHex(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("SM3哈希失败", e);
            throw new RuntimeException("SM3哈希失败", e);
        }
    }

    /**
     * SM3哈希（返回字节数组）
     *
     * @param data 原始数据
     * @return 哈希值字节数组
     */
    public static byte[] hashBytes(byte[] data) {
        try {
            SM3 sm3 = SmUtil.sm3();
            return sm3.digest(data);
        } catch (Exception e) {
            log.error("SM3哈希失败", e);
            throw new RuntimeException("SM3哈希失败", e);
        }
    }

    /**
     * SM3-HMAC（带密钥的哈希）
     * 用于消息认证码（MAC）
     *
     * @param data 原始数据
     * @param key  密钥
     * @return 16进制HMAC值
     */
    public static String hmac(String data, String key) {
        try {
            SM3 sm3 = SmUtil.sm3WithSalt(key.getBytes(StandardCharsets.UTF_8));
            return sm3.digestHex(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("SM3-HMAC失败", e);
            throw new RuntimeException("SM3-HMAC失败", e);
        }
    }

    /**
     * 验证SM3哈希
     *
     * @param data         原始数据
     * @param expectedHash 预期的哈希值
     * @return 是否匹配
     */
    public static boolean verify(String data, String expectedHash) {
        String actualHash = hash(data);
        return actualHash.equalsIgnoreCase(expectedHash);
    }

    /**
     * 生成加密字段的哈希索引
     * 用于加密字段的查询索引
     *
     * @param plainText 明文
     * @return 哈希值（用于索引）
     */
    public static String generateIndex(String plainText) {
        return hash(plainText);
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        System.out.println("=== SM3哈希测试 ===");
        String data1 = "这是一段需要哈希的数据";
        String hash1 = hash(data1);
        System.out.println("原文: " + data1);
        System.out.println("SM3哈希: " + hash1);
        System.out.println("哈希长度: " + hash1.length() + " 字符 (256位)");

        // 测试相同数据哈希一致性
        String hash2 = hash(data1);
        System.out.println("再次哈希: " + hash2);
        System.out.println("两次哈希是否一致: " + hash1.equals(hash2));

        // 测试不同数据
        String data2 = "这是另一段数据";
        String hash3 = hash(data2);
        System.out.println("\n不同数据的哈希: " + hash3);
        System.out.println("两个哈希是否不同: " + !hash1.equals(hash3));

        // 测试验证
        boolean verifyResult = verify(data1, hash1);
        System.out.println("\n哈希验证: " + verifyResult);

        // 测试HMAC
        System.out.println("\n=== SM3-HMAC测试 ===");
        String key = "my-secret-key";
        String hmac1 = hmac(data1, key);
        System.out.println("HMAC: " + hmac1);

        String hmac2 = hmac(data1, "wrong-key");
        System.out.println("错误密钥HMAC: " + hmac2);
        System.out.println("两个HMAC是否不同: " + !hmac1.equals(hmac2));

        // 测试索引生成
        System.out.println("\n=== 加密字段索引生成 ===");
        String username = "teacher1";
        String usernameIndex = generateIndex(username);
        System.out.println("用户名: " + username);
        System.out.println("索引哈希: " + usernameIndex);
    }
}
