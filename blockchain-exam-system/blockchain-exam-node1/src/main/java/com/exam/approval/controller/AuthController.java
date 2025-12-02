package com.exam.approval.controller;

import com.exam.approval.common.result.Result;
import com.exam.approval.entity.User;
import com.exam.approval.security.util.JwtUtil;
import com.exam.approval.security.util.PasswordUtil;
import com.exam.approval.security.util.RSAUtil;
import com.exam.approval.service.NonceService;
import com.exam.approval.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证Controller
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Slf4j
@Api(tags = "认证接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final NonceService nonceService;

    /**
     * 系统RSA公钥（用于前端加密密码）
     */
    @Value("${crypto.system-rsa.public-key}")
    private String systemPublicKey;

    /**
     * 系统RSA私钥（用于后端解密密码）
     */
    @Value("${crypto.system-rsa.private-key}")
    private String systemPrivateKey;

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> loginRequest) {
        String username = (String) loginRequest.get("username");
        String encryptedPassword = (String) loginRequest.get("encryptedPassword");
        Object timestampObj = loginRequest.get("timestamp");
        String nonce = (String) loginRequest.get("nonce");

        // 参数校验
        if (username == null || encryptedPassword == null || timestampObj == null || nonce == null) {
            log.warn("登录请求参数不完整");
            return Result.error("请求参数不完整");
        }

        // 转换时间戳
        long timestamp;
        try {
            if (timestampObj instanceof Number) {
                timestamp = ((Number) timestampObj).longValue();
            } else {
                timestamp = Long.parseLong(timestampObj.toString());
            }
        } catch (Exception e) {
            log.warn("时间戳格式错误: {}", timestampObj);
            return Result.error("时间戳格式错误");
        }

        // 验证时间戳和nonce（防重放攻击）
        if (!nonceService.validate(timestamp, nonce)) {
            log.warn("重放攻击检测 - username: {}, nonce: {}, timestamp: {}", username, nonce, timestamp);
            return Result.error("请求已过期或被拒绝，请重新登录");
        }

        // 解密密码（使用PKCS1 padding，兼容JSEncrypt）
        String password;
        try {
            password = RSAUtil.decryptByPrivateKeyPKCS1(encryptedPassword, systemPrivateKey);
            log.debug("密码解密成功 - username: {}", username);
        } catch (Exception e) {
            log.error("密码解密失败 - username: {}", username, e);
            return Result.error("密码解密失败，请重试");
        }

        // 查询用户
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 验证密码
        if (!PasswordUtil.matches(password, user.getPassword())) {
            return Result.error("密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            return Result.error("账号已被禁用");
        }

        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "realName", user.getRealName(),
            "role", user.getRole(),
            "department", user.getDepartment() != null ? user.getDepartment() : ""
        ));

        log.info("用户登录成功: {}", username);
        return Result.success("登录成功", data);
    }

    @ApiOperation("退出登录")
    @PostMapping("/logout")
    public Result<?> logout() {
        log.info("用户退出登录");
        return Result.success("退出成功");
    }

    /**
     * 获取系统RSA公钥
     * 前端使用此公钥加密密码后再发送登录请求
     *
     * @return RSA公钥（Base64编码）
     */
    @ApiOperation("获取RSA公钥")
    @GetMapping("/public-key")
    public Result<Map<String, String>> getPublicKey() {
        Map<String, String> data = new HashMap<>();
        data.put("publicKey", systemPublicKey);
        log.debug("返回系统RSA公钥");
        return Result.success(data);
    }

    /**
     * 获取当前登录用户的RSA公钥
     * 用于其他用户加密发送给该用户的试卷
     *
     * @param userId 当前用户ID (从JWT中获取)
     * @return 用户公钥
     */
    @ApiOperation("获取当前用户公钥")
    @GetMapping("/my-public-key")
    public Result<Map<String, Object>> getMyPublicKey(@RequestHeader("Authorization") String authHeader) {
        try {
            // 从 Authorization header 提取 token
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(token);

            User user = userService.getUserById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            if (user.getRsaPublicKey() == null || user.getRsaPublicKey().isEmpty()) {
                return Result.error("用户未生成密钥，请联系管理员");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId());
            data.put("username", user.getUsername());
            data.put("publicKey", user.getRsaPublicKey());
            data.put("keyGeneratedTime", user.getKeyGeneratedTime());

            return Result.success(data);
        } catch (Exception e) {
            log.error("获取用户公钥失败", e);
            return Result.error("获取公钥失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定用户的RSA公钥
     * 用于加密发送给该用户的试卷（如发送给审批人）
     *
     * @param userId 目标用户ID
     * @return 用户公钥
     */
    @ApiOperation("获取指定用户公钥")
    @GetMapping("/user/{userId}/public-key")
    public Result<Map<String, Object>> getUserPublicKey(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);
            if (user == null) {
                return Result.notFound("用户不存在");
            }

            if (user.getRsaPublicKey() == null || user.getRsaPublicKey().isEmpty()) {
                return Result.error("该用户未生成密钥");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId());
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("publicKey", user.getRsaPublicKey());

            return Result.success(data);
        } catch (Exception e) {
            log.error("获取用户公钥失败 - userId: {}", userId, e);
            return Result.error("获取公钥失败: " + e.getMessage());
        }
    }
}
