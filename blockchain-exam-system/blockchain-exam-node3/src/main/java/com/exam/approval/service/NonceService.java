package com.exam.approval.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Nonce服务 - 防重放攻击
 *
 * 用于登录请求的防重放攻击：
 * 1. 客户端每次请求生成唯一nonce（随机字符串）
 * 2. 服务端验证nonce是否已使用过
 * 3. 使用Redis存储已使用的nonce，设置TTL为时间窗口
 * 4. 过期后自动删除，节省存储空间
 *
 * @author 网络信息安全大作业
 * @date 2025-11-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NonceService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 防重放攻击时间窗口（毫秒）
     * 从配置文件读取：security.replay-time-window
     */
    @Value("${security.replay-time-window:300000}")
    private long timeWindow;

    /**
     * Nonce的Redis Key前缀
     */
    private static final String NONCE_PREFIX = "nonce:";

    /**
     * 验证并记录nonce
     * 如果nonce已存在（已被使用），返回false
     * 如果nonce不存在（首次使用），记录到Redis并返回true
     *
     * @param nonce 客户端生成的随机字符串
     * @return true-验证通过（首次使用），false-验证失败（重放攻击）
     */
    public boolean validateAndRecord(String nonce) {
        if (nonce == null || nonce.trim().isEmpty()) {
            log.warn("Nonce为空，验证失败");
            return false;
        }

        String key = NONCE_PREFIX + nonce;

        try {
            // 使用setIfAbsent实现原子性检查和设置
            // 如果key不存在，设置成功返回true
            // 如果key已存在，设置失败返回false
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(key, System.currentTimeMillis(), timeWindow, TimeUnit.MILLISECONDS);

            if (Boolean.TRUE.equals(success)) {
                log.debug("Nonce验证通过: {}", nonce);
                return true;
            } else {
                log.warn("检测到重放攻击，nonce已被使用: {}", nonce);
                return false;
            }
        } catch (Exception e) {
            // Redis异常时，为了安全起见，拒绝请求
            log.error("Nonce验证异常，拒绝请求: {}", nonce, e);
            return false;
        }
    }

    /**
     * 验证时间戳是否在允许的时间窗口内
     * 防止使用过期的请求进行攻击
     *
     * @param timestamp 客户端请求时间戳（毫秒）
     * @return true-时间戳有效，false-时间戳无效（超出时间窗口）
     */
    public boolean validateTimestamp(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long diff = Math.abs(currentTime - timestamp);

        if (diff > timeWindow) {
            log.warn("时间戳超出允许范围，diff={}ms, timeWindow={}ms", diff, timeWindow);
            return false;
        }

        return true;
    }

    /**
     * 同时验证时间戳和nonce
     *
     * @param timestamp 时间戳
     * @param nonce     随机字符串
     * @return true-验证通过，false-验证失败
     */
    public boolean validate(long timestamp, String nonce) {
        // 先验证时间戳，避免过期请求的nonce污染Redis
        if (!validateTimestamp(timestamp)) {
            log.warn("时间戳验证失败: timestamp={}", timestamp);
            return false;
        }

        // 验证并记录nonce
        if (!validateAndRecord(nonce)) {
            log.warn("Nonce验证失败: nonce={}", nonce);
            return false;
        }

        return true;
    }

    /**
     * 手动清除nonce（仅用于测试）
     *
     * @param nonce 要清除的nonce
     */
    public void clearNonce(String nonce) {
        String key = NONCE_PREFIX + nonce;
        redisTemplate.delete(key);
        log.debug("已清除nonce: {}", nonce);
    }
}
