package com.exam.approval.security.util;

import cn.hutool.crypto.digest.BCrypt;
import lombok.extern.slf4j.Slf4j;

/**
 * 密码哈希工具类（BCrypt）
 *
 * BCrypt特性：
 * - 自适应哈希算法（computational cost可调）
 * - 自动加盐（salt）
 * - 慢速哈希，抵御暴力破解
 * - 单向不可逆
 *
 * 用途：
 * - 用户密码存储
 * - 密码验证
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Slf4j
public class PasswordUtil {

    /**
     * 默认BCrypt强度（10轮）
     * 值越大越安全，但计算越慢
     * 推荐范围：10-12
     */
    private static final int DEFAULT_STRENGTH = 10;

    /**
     * 密码加密（BCrypt哈希）
     *
     * @param password 明文密码
     * @return BCrypt哈希后的密码
     */
    public static String encode(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        try {
            return BCrypt.hashpw(password, BCrypt.gensalt(DEFAULT_STRENGTH));
        } catch (Exception e) {
            log.error("密码加密失败", e);
            throw new RuntimeException("密码加密失败", e);
        }
    }

    /**
     * 密码验证
     *
     * @param rawPassword     明文密码
     * @param encodedPassword BCrypt哈希后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, encodedPassword);
        } catch (Exception e) {
            log.error("密码验证失败", e);
            return false;
        }
    }

    /**
     * 检查密码强度
     *
     * @param password 密码
     * @return 密码强度：0-弱, 1-中, 2-强
     */
    public static int checkStrength(String password) {
        if (password == null || password.length() < 6) {
            return 0; // 弱
        }

        int strength = 0;

        // 检查长度
        if (password.length() >= 8) {
            strength++;
        }

        // 检查是否包含数字
        if (password.matches(".*\\d.*")) {
            strength++;
        }

        // 检查是否包含小写字母
        if (password.matches(".*[a-z].*")) {
            strength++;
        }

        // 检查是否包含大写字母
        if (password.matches(".*[A-Z].*")) {
            strength++;
        }

        // 检查是否包含特殊字符
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            strength++;
        }

        // 强度分级
        if (strength <= 2) {
            return 0; // 弱
        } else if (strength <= 4) {
            return 1; // 中
        } else {
            return 2; // 强
        }
    }

    /**
     * 验证密码是否符合要求
     *
     * @param password 密码
     * @return 是否符合要求
     */
    public static boolean validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 20) {
            return false;
        }
        // 至少包含字母和数字
        return password.matches(".*[a-zA-Z].*") && password.matches(".*\\d.*");
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        System.out.println("=== 密码哈希测试 ===");

        String password = "123456";
        System.out.println("原始密码: " + password);

        // 加密
        String encoded1 = encode(password);
        System.out.println("第一次哈希: " + encoded1);

        String encoded2 = encode(password);
        System.out.println("第二次哈希: " + encoded2);

        System.out.println("两次哈希是否不同: " + !encoded1.equals(encoded2) + " (BCrypt每次加盐不同)");

        // 验证
        boolean match1 = matches(password, encoded1);
        System.out.println("\n正确密码验证: " + match1);

        boolean match2 = matches("wrong-password", encoded1);
        System.out.println("错误密码验证: " + match2);

        // 密码强度测试
        System.out.println("\n=== 密码强度测试 ===");
        String[] passwords = {
                "123",
                "123456",
                "abc123",
                "Abc123",
                "Abc123!@#",
                "MyP@ssw0rd2024!"
        };

        for (String pwd : passwords) {
            int strength = checkStrength(pwd);
            String[] levels = {"弱", "中", "强"};
            boolean valid = validatePassword(pwd);
            System.out.printf("密码: %-20s  强度: %s  是否符合要求: %s%n",
                    pwd, levels[strength], valid);
        }

        // 实际用例：用户登录验证
        System.out.println("\n=== 实际用例：用户登录 ===");
        String userPassword = "Teacher123";
        String storedHash = encode(userPassword);
        System.out.println("数据库存储的哈希: " + storedHash);

        // 模拟登录验证
        String loginPassword = "Teacher123";
        boolean loginSuccess = matches(loginPassword, storedHash);
        System.out.println("登录验证结果: " + (loginSuccess ? "成功" : "失败"));
    }
}
