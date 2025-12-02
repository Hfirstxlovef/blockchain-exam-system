package com.exam.approval.tools;

import cn.hutool.core.codec.Base64;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户密钥批量生成工具
 *
 * 功能：
 * 1. 为所有用户生成RSA-2048密钥对
 * 2. 公钥存入数据库
 * 3. 私钥导出为PEM文件到 keys/ 目录
 *
 * 运行方式:
 *   cd blockchain-exam-node1
 *   mvn exec:java -Dexec.mainClass="com.exam.approval.tools.GenerateUserKeys"
 *
 * @author Claude Code
 * @since 2025-11-28
 */
public class GenerateUserKeys {

    // 数据库配置
    private static final String DB_URL = "jdbc:mysql://localhost:3306/blockchain_exam_system?useSSL=false&serverTimezone=Asia/Shanghai";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "sunbuyang123";

    // 密钥输出目录
    private static final String KEYS_DIR = "keys";

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("    用户RSA密钥批量生成工具");
        System.out.println("========================================\n");

        try {
            // 创建 keys 目录
            File keysDir = new File(KEYS_DIR);
            if (!keysDir.exists()) {
                keysDir.mkdirs();
                System.out.println("创建目录: " + keysDir.getAbsolutePath());
            }

            // 连接数据库
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("数据库连接成功\n");

            // 查询所有用户
            String selectSql = "SELECT id, username, real_name, role, department FROM sys_user WHERE deleted = 0";
            PreparedStatement selectStmt = conn.prepareStatement(selectSql);
            ResultSet rs = selectStmt.executeQuery();

            // 更新语句
            String updateSql = "UPDATE sys_user SET rsa_public_key = ?, key_generated_time = ? WHERE id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);

            int count = 0;
            while (rs.next()) {
                long userId = rs.getLong("id");
                String username = rs.getString("username");
                String realName = rs.getString("real_name");
                String role = rs.getString("role");
                String department = rs.getString("department");

                System.out.println("处理用户: " + username + " (" + realName + ")");
                System.out.println("  角色: " + role + ", 部门: " + department);

                // 生成密钥对
                KeyPair keyPair = generateKeyPair();
                String publicKeyBase64 = Base64.encode(keyPair.getPublic().getEncoded());
                String privateKeyBase64 = Base64.encode(keyPair.getPrivate().getEncoded());

                // 更新数据库（公钥）
                updateStmt.setString(1, publicKeyBase64);
                updateStmt.setTimestamp(2, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                updateStmt.setLong(3, userId);
                updateStmt.executeUpdate();
                System.out.println("  公钥已存入数据库");

                // 导出私钥为 PEM 文件
                String pemContent = convertToPEM(privateKeyBase64, "PRIVATE KEY");
                String pemFilePath = KEYS_DIR + "/" + username + "_private.pem";
                try (FileWriter writer = new FileWriter(pemFilePath)) {
                    writer.write(pemContent);
                }
                System.out.println("  私钥已导出: " + pemFilePath);

                // 同时导出公钥（方便验证）
                String pubPemContent = convertToPEM(publicKeyBase64, "PUBLIC KEY");
                String pubPemFilePath = KEYS_DIR + "/" + username + "_public.pem";
                try (FileWriter writer = new FileWriter(pubPemFilePath)) {
                    writer.write(pubPemContent);
                }
                System.out.println("  公钥已导出: " + pubPemFilePath);

                System.out.println();
                count++;
            }

            rs.close();
            selectStmt.close();
            updateStmt.close();
            conn.close();

            System.out.println("========================================");
            System.out.println("完成! 共处理 " + count + " 个用户");
            System.out.println("私钥文件位于: " + new File(KEYS_DIR).getAbsolutePath());
            System.out.println("========================================");
            System.out.println("\n⚠️  重要提示:");
            System.out.println("  1. 私钥文件请妥善保管，不要泄露!");
            System.out.println("  2. 将对应的 .pem 文件分发给各用户");
            System.out.println("  3. 用户需要私钥来解密试卷和签名审批");

        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 生成 RSA-2048 密钥对
     */
    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048, new SecureRandom());
        return keyPairGen.generateKeyPair();
    }

    /**
     * 将 Base64 编码的密钥转换为 PEM 格式
     */
    private static String convertToPEM(String base64Key, String keyType) {
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN ").append(keyType).append("-----\n");

        // 每64字符换行
        int index = 0;
        while (index < base64Key.length()) {
            int end = Math.min(index + 64, base64Key.length());
            pem.append(base64Key.substring(index, end)).append("\n");
            index = end;
        }

        pem.append("-----END ").append(keyType).append("-----\n");
        return pem.toString();
    }
}
