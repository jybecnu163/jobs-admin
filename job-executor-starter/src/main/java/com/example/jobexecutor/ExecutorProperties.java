package com.example.jobexecutor;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 接收 application.yml 中以 job.executor 为前缀的配置
 * <p>
 * 被自动配置类注入到各个组件中使用。
 */
@ConfigurationProperties(prefix = "job.executor")
@Data
public class ExecutorProperties {
    private boolean enabled = true;
    // 不同环境不同地址
    private String adminUrl = "http://localhost:8080";
    /**
     * 当前业务系统的标识
     */
    private String systemCode;
    private int heartbeatInterval = 10;
    private boolean resultCallbackEnabled = true;
    private boolean logCaptureEnabled = true;
    private String apiToken = "";
    private String namespace = "default";
}
