package com.example.jobexecutor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 作用：
 * <p>
 * Spring Boot 的自动配置入口。
 * <p>
 * 启用 @ConfigurationProperties，并将 ExecutorProperties 绑定到上下文。
 * <p>
 * 创建并暴露五个核心 Bean：
 * <p>
 * JobAnnotationScanner（扫描与注册）
 * <p>
 * HeartbeatSender（心跳发送）
 * <p>
 * ExecutionResultReporter（执行结果上报）
 * <p>
 * JobLogCollector（任务日志采集）
 * <p>
 * JobExecutionController（接收指令）
 * <p>
 * 为什么需要它：让引入 starter 的业务系统无需任何手动 @Bean 配置，自动装配所有功能。
 */
@Configuration
@EnableConfigurationProperties(ExecutorProperties.class)
public class JobExecutorAutoConfiguration {
    @Bean
    public JobAnnotationScanner jobAnnotationScanner(ExecutorProperties properties, ApplicationContext ctx) {
        return new JobAnnotationScanner(properties, ctx);
    }

    @Bean
    public HeartbeatSender heartbeatSender(ExecutorProperties properties) {
        return new HeartbeatSender(properties);
    }

    @Bean
    public ExecutionResultReporter executionResultReporter(ExecutorProperties properties) {
        return new ExecutionResultReporter(properties);
    }

    @Bean
    public JobLogCollector jobLogCollector(ExecutorProperties properties) {
        return new JobLogCollector(properties);
    }

    @Bean
    public JobExecutionController jobExecutionController(ExecutionResultReporter reporter,
                                                         JobLogCollector logCollector) {
        return new JobExecutionController(reporter, logCollector);
    }
}
