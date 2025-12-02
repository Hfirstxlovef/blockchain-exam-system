package com.exam.approval.interceptor;

import com.exam.approval.common.annotation.Encrypted;
import com.exam.approval.security.util.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * MyBatis加密拦截器
 *
 * 功能：
 * 1. 插入/更新时：自动加密标注@Encrypted的字段
 * 2. 查询时：自动解密标注@Encrypted的字段
 *
 * 实现原理：
 * - 拦截ParameterHandler.setParameters() - 参数设置前加密
 * - 拦截ResultSetHandler.handleResultSets() - 结果返回前解密
 *
 * 注意：通过 MyBatisPlusConfig.configurationCustomizer() 注册，不使用 @Component
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Slf4j
@Intercepts({
    @Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class}),
    @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})
})
public class MybatisCryptoInterceptor implements Interceptor {

    private String masterKey;

    private volatile boolean initialized = false;

    /**
     * 设置主密钥（由 Spring 配置类调用）
     */
    public void setMasterKey(String masterKey) {
        this.masterKey = masterKey;
    }

    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    if (masterKey != null) {
                        try {
                            byte[] keyBytes = java.util.Base64.getDecoder().decode(masterKey);
                            log.info("加密拦截器初始化成功 - AES密钥: {}字节", keyBytes.length);
                        } catch (Exception e) {
                            log.error("MasterKey不是有效的Base64编码", e);
                        }
                    } else {
                        log.error("MasterKey未配置!");
                    }
                    initialized = true;
                }
            }
        }
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();

        // 参数加密（插入/更新）
        if (target instanceof ParameterHandler) {
            ParameterHandler parameterHandler = (ParameterHandler) target;
            Object parameterObject = parameterHandler.getParameterObject();
            log.debug("参数加密拦截: {}", parameterObject != null ? parameterObject.getClass().getSimpleName() : "null");
            if (parameterObject != null) {
                encryptFields(parameterObject);
            }
        }

        // 执行原方法
        Object result = invocation.proceed();

        // 结果解密（查询）
        if (target instanceof ResultSetHandler) {
            if (result instanceof List) {
                List<?> list = (List<?>) result;
                log.debug("结果解密拦截: {} 条记录", list.size());
                for (Object item : list) {
                    decryptFields(item);
                }
            }
        }

        return result;
    }

    /**
     * 加密字段
     */
    private void encryptFields(Object obj) {
        if (obj == null) {
            return;
        }

        Class<?> clazz = obj.getClass();
        log.debug("加密处理: {}", clazz.getSimpleName());

        // 【关键修复】如果是 Map（如 ParamMap），递归处理 Map 中的值
        if (obj instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
            log.debug("Map类型，递归处理 {} 个值", map.size());
            for (Object value : map.values()) {
                if (value != null) {
                    encryptFields(value);
                }
            }
            return;
        }

        // 跳过基本类型和JDK类
        if (clazz.isPrimitive() || clazz.getName().startsWith("java.")) {
            log.debug("跳过JDK类: {}", clazz.getSimpleName());
            return;
        }

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Encrypted.class)) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);

                    if (value instanceof String) {
                        String plainText = (String) value;
                        log.debug("@Encrypted字段: {}.{}, 长度={}",
                            clazz.getSimpleName(), field.getName(),
                            plainText != null ? plainText.length() : 0);

                        if (plainText != null && !plainText.isEmpty()) {
                            boolean alreadyEncrypted = isEncrypted(plainText);

                            if (!alreadyEncrypted) {
                                try {
                                    String encrypted = AESUtil.encrypt(plainText, masterKey);
                                    field.set(obj, encrypted);
                                    log.info("✓ 加密成功: {}.{} ({}字节 → {}字节)",
                                        clazz.getSimpleName(), field.getName(),
                                        plainText.length(), encrypted.length());
                                } catch (Exception encryptEx) {
                                    log.error("✗ 加密失败: {}.{} - {}",
                                        clazz.getSimpleName(), field.getName(), encryptEx.getMessage());
                                }
                            } else {
                                log.debug("跳过已加密字段: {}.{}", clazz.getSimpleName(), field.getName());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("加密字段异常: {}.{} - {}", clazz.getSimpleName(), field.getName(), e.getMessage());
                }
            }
        }
    }

    /**
     * 解密字段
     */
    private void decryptFields(Object obj) {
        if (obj == null) {
            return;
        }

        Class<?> clazz = obj.getClass();
        if (clazz.isPrimitive() || clazz.getName().startsWith("java.")) {
            return;
        }

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Encrypted.class)) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);

                    if (value instanceof String) {
                        String encrypted = (String) value;

                        if (encrypted != null && !encrypted.isEmpty()) {
                            // 先判断是否是加密数据，避免对明文数据进行解密
                            if (!isEncrypted(encrypted)) {
                                log.debug("跳过明文数据: {}.{}", clazz.getSimpleName(), field.getName());
                                continue;
                            }

                            try {
                                String decrypted = AESUtil.decrypt(encrypted, masterKey);
                                field.set(obj, decrypted);
                                log.info("✓ 解密成功: {}.{} ({}字节 → {}字节)",
                                    clazz.getSimpleName(), field.getName(),
                                    encrypted.length(), decrypted.length());
                            } catch (Exception e) {
                                log.warn("✗ 解密失败: {}.{} - {}",
                                    clazz.getSimpleName(), field.getName(), e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("解密字段异常: {}.{} - {}", clazz.getSimpleName(), field.getName(), e.getMessage());
                }
            }
        }
    }

    /**
     * 判断是否已加密（Base64格式且符合AES-GCM结构）
     */
    private boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // 极短字符串（<16字符）肯定不是加密数据
        // AES-GCM 加密后：12字节IV + 至少1字节密文 + 16字节Tag = 至少29字节
        // Base64编码后：至少 ceil(29*4/3) = 39字符
        if (text.length() < 16) {
            return false;
        }

        // Base64字符集检查
        if (!text.matches("^[A-Za-z0-9+/=]+$")) {
            return false;
        }

        // 加密后的Base64字符串通常较长（至少39字符）
        if (text.length() < 39) {
            return false;
        }

        // 尝试Base64解码，检查字节长度是否符合AES-GCM格式
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(text);
            // AES-GCM格式：12字节IV + 密文 + 16字节Tag，至少29字节
            return decoded.length >= 29;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可以从配置文件读取属性
    }
}
