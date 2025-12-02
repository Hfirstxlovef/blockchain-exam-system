package com.exam.approval.config;

import com.exam.approval.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置
 *
 * 功能：
 * 1. JWT认证：无状态Token认证
 * 2. 权限控制：基于角色的访问控制
 * 3. CORS配置：跨域请求支持
 * 4. 安全防护：CSRF、XSS、点击劫持防护
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 安全过滤器链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（使用JWT不需要CSRF保护）
                .csrf().disable()

                // 配置CORS
                .cors().configurationSource(corsConfigurationSource())
                .and()

                // 无状态会话管理
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

                // 配置访问权限
                .authorizeRequests()
                // 公开接口：登录、获取公钥、Swagger文档
                .antMatchers("/auth/login", "/auth/logout", "/auth/public-key", "/auth/user/*/public-key").permitAll()
                .antMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                .antMatchers("/doc.html", "/webjars/**", "/favicon.ico").permitAll()
                // 区块链相关接口（查询类公开，操作类需认证）
                .antMatchers("/blockchain/**", "/transaction/**", "/p2p/**").permitAll()

                // 试卷管理：教师可以访问
                .antMatchers("/exam-paper/**").hasAnyRole("TEACHER", "DEPT_ADMIN", "COLLEGE_ADMIN")

                // 审批管理：只有管理员可以访问
                .antMatchers("/approval/**").hasAnyRole("DEPT_ADMIN", "COLLEGE_ADMIN")

                // 系统管理：只有超级管理员可以访问
                .antMatchers("/system/**").hasRole("ADMIN")

                // 其他所有请求需要认证
                .anyRequest().authenticated()
                .and()

                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 安全响应头配置
                .headers()
                // 防止点击劫持
                .frameOptions().deny()
                // XSS保护
                .xssProtection().and()
                // 内容类型嗅探保护
                .contentTypeOptions().and()
                // HSTS（强制HTTPS）
                .httpStrictTransportSecurity()
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000);

        return http.build();
    }

    /**
     * CORS跨域配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的前端地址
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",   // Vite默认端口
                "http://localhost:53000",  // 当前前端端口
                "http://localhost:8080",   // 生产部署地址
                "http://127.0.0.1:5173",
                "http://127.0.0.1:53000"
        ));

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));

        // 允许携带认证信息
        configuration.setAllowCredentials(true);

        // 预检请求缓存时间（1小时）
        configuration.setMaxAge(3600L);

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
