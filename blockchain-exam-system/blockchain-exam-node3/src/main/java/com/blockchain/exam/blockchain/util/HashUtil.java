package com.blockchain.exam.blockchain.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256哈希工具类
 *
 * 用于区块链的哈希计算
 * - 算法：SHA-256
 * - 输出长度：256位（64个16进制字符）
 * - 用途：区块哈希、交易哈希、Merkle树等
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
public class HashUtil {

    private static final String ALGORITHM = "SHA-256";

    /**
     * 计算SHA-256哈希（返回16进制字符串）
     *
     * @param data 原始数据
     * @return 64位16进制哈希值
     */
    public static String sha256(String data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256算法不可用", e);
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 计算SHA-256哈希（返回字节数组）
     *
     * @param data 原始数据
     * @return 哈希值字节数组
     */
    public static byte[] sha256Bytes(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256算法不可用", e);
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 计算字符串的SHA-256哈希（返回字节数组）
     *
     * @param data 原始数据
     * @return 哈希值字节数组
     */
    public static byte[] sha256Bytes(String data) {
        return sha256Bytes(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 字节数组转16进制字符串
     *
     * @param bytes 字节数组
     * @return 16进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 验证哈希值
     *
     * @param data         原始数据
     * @param expectedHash 预期的哈希值
     * @return 是否匹配
     */
    public static boolean verify(String data, String expectedHash) {
        String actualHash = sha256(data);
        return actualHash.equalsIgnoreCase(expectedHash);
    }

    /**
     * 验证PoW哈希是否满足难度要求
     * 检查哈希值前缀是否有足够多的0
     *
     * @param hash       哈希值
     * @param difficulty 难度（前缀0的个数）
     * @return 是否满足难度要求
     */
    public static boolean validateProofOfWork(String hash, int difficulty) {
        if (hash == null || hash.length() < difficulty) {
            return false;
        }
        String prefix = hash.substring(0, difficulty);
        for (char c : prefix.toCharArray()) {
            if (c != '0') {
                return false;
            }
        }
        return true;
    }

    /**
     * 生成难度目标字符串
     * 例如：difficulty=4 返回 "0000"
     *
     * @param difficulty 难度值
     * @return 目标前缀
     */
    public static String getDifficultyTarget(int difficulty) {
        StringBuilder target = new StringBuilder();
        for (int i = 0; i < difficulty; i++) {
            target.append('0');
        }
        return target.toString();
    }

    /**
     * 计算Merkle树根哈希
     * 简化版本：直接对所有交易哈希进行连接后再哈希
     *
     * @param transactionHashes 交易哈希列表
     * @return Merkle根哈希
     */
    public static String calculateMerkleRoot(String[] transactionHashes) {
        if (transactionHashes == null || transactionHashes.length == 0) {
            return sha256("");
        }

        // 简化版本：将所有交易哈希连接后再哈希
        StringBuilder combined = new StringBuilder();
        for (String hash : transactionHashes) {
            combined.append(hash);
        }
        return sha256(combined.toString());
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        System.out.println("=== SHA-256哈希测试 ===");

        // 测试基本哈希
        String data1 = "Hello, Blockchain!";
        String hash1 = sha256(data1);
        System.out.println("原文: " + data1);
        System.out.println("SHA-256: " + hash1);
        System.out.println("哈希长度: " + hash1.length() + " 字符 (256位)");

        // 测试哈希一致性
        String hash2 = sha256(data1);
        System.out.println("\n两次哈希是否一致: " + hash1.equals(hash2));

        // 测试不同数据
        String data2 = "Hello, Blockchain";
        String hash3 = sha256(data2);
        System.out.println("\n不同数据哈希: " + hash3);
        System.out.println("两个哈希是否不同: " + !hash1.equals(hash3));

        // 测试验证
        boolean verifyResult = verify(data1, hash1);
        System.out.println("\n哈希验证: " + verifyResult);

        // 测试PoW验证
        System.out.println("\n=== PoW难度验证测试 ===");
        String powHash1 = "0000abc123def456";
        String powHash2 = "00abc123def456";
        String powHash3 = "0000000abc123def";

        System.out.println("哈希: " + powHash1 + " 难度4: " + validateProofOfWork(powHash1, 4));
        System.out.println("哈希: " + powHash2 + " 难度4: " + validateProofOfWork(powHash2, 4));
        System.out.println("哈希: " + powHash3 + " 难度4: " + validateProofOfWork(powHash3, 4));
        System.out.println("哈希: " + powHash3 + " 难度7: " + validateProofOfWork(powHash3, 7));

        // 测试难度目标
        System.out.println("\n=== 难度目标生成 ===");
        System.out.println("难度1: " + getDifficultyTarget(1));
        System.out.println("难度4: " + getDifficultyTarget(4));
        System.out.println("难度6: " + getDifficultyTarget(6));

        // 测试Merkle根
        System.out.println("\n=== Merkle根计算测试 ===");
        String[] txHashes = {
            sha256("transaction1"),
            sha256("transaction2"),
            sha256("transaction3")
        };
        String merkleRoot = calculateMerkleRoot(txHashes);
        System.out.println("交易1哈希: " + txHashes[0]);
        System.out.println("交易2哈希: " + txHashes[1]);
        System.out.println("交易3哈希: " + txHashes[2]);
        System.out.println("Merkle根: " + merkleRoot);

        // 测试区块哈希
        System.out.println("\n=== 模拟区块哈希计算 ===");
        long blockIndex = 1;
        String previousHash = "0000000000000000000000000000000000000000000000000000000000000000";
        long timestamp = System.currentTimeMillis();
        String blockData = "Block data content";
        int nonce = 12345;

        String blockHashData = blockIndex + previousHash + timestamp + blockData + nonce;
        String blockHash = sha256(blockHashData);

        System.out.println("区块高度: " + blockIndex);
        System.out.println("前一区块哈希: " + previousHash);
        System.out.println("时间戳: " + timestamp);
        System.out.println("区块数据: " + blockData);
        System.out.println("Nonce: " + nonce);
        System.out.println("区块哈希: " + blockHash);
    }
}
