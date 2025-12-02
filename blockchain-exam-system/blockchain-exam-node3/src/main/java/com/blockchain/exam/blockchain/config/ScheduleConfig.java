package com.blockchain.exam.blockchain.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 *
 * 启用Spring的@Scheduled定时任务支持
 * 用于矿工定时挖矿任务
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
}
