package com.exam.approval.config;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.exam.approval.interceptor.MybatisCryptoInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置
 *
 * @author 网络信息安全大作业
 * @date 2025-11-13
 */
@Slf4j
@Configuration
public class MyBatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());

        log.info("MyBatis-Plus 插件注册完成");
        return interceptor;
    }

    /**
     * 创建加密拦截器 Bean
     * MyBatis-Plus 会自动检测并注册所有实现了 Interceptor 接口的 Bean
     */
    @Bean
    public MybatisCryptoInterceptor mybatisCryptoInterceptor(@Value("${crypto.master-key}") String masterKey) {
        log.info("创建 MybatisCryptoInterceptor Bean");
        MybatisCryptoInterceptor interceptor = new MybatisCryptoInterceptor();
        interceptor.setMasterKey(masterKey);
        log.info("加密拦截器创建成功，将由 MyBatis-Plus 自动注册");
        return interceptor;
    }

    /**
     * 自动填充处理器
     */
    @Component
    public static class MetaObjectHandlerImpl implements MetaObjectHandler {

        /**
         * 插入时自动填充
         */
        @Override
        public void insertFill(MetaObject metaObject) {
            log.debug("开始插入填充...");

            // 填充创建时间
            this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());

            // 填充更新时间
            this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        }

        /**
         * 更新时自动填充
         */
        @Override
        public void updateFill(MetaObject metaObject) {
            log.debug("开始更新填充...");

            // 填充更新时间
            this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        }
    }
}
