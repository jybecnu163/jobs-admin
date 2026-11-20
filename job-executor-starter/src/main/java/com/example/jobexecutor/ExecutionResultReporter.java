package com.example.jobexecutor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 执行结果上报器：任务结束后把状态、起止时间与异常信息回调给 Admin 落库。
 */
@Slf4j
public class ExecutionResultReporter {
    private final ExecutorProperties properties;
    private final RestTemplate restTemplate;

    public ExecutionResultReporter(ExecutorProperties properties) {
        this(properties, new RestTemplate());
    }

    public ExecutionResultReporter(ExecutorProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    /**
     * 上报一次执行结果；上报失败仅记录告警，不影响任务本身。
     */
    public void report(String jobName, String status, LocalDateTime startTime,
                       LocalDateTime endTime, String errorMessage) {
        if (!properties.isResultCallbackEnabled()) {
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("jobName", jobName);
            body.put("systemCode", properties.getSystemCode());
            body.put("status", status);
            body.put("startTime", startTime == null ? null : startTime.toString());
            body.put("endTime", endTime == null ? null : endTime.toString());
            body.put("durationMs", startTime == null || endTime == null
                    ? null : Duration.between(startTime, endTime).toMillis());
            body.put("errorMessage", errorMessage);
            restTemplate.postForObject(
                    properties.getAdminUrl() + "/api/callback/execution-result", body, Void.class);
        } catch (Exception e) {
            log.warn("上报执行结果失败: {} - {}", jobName, e.getMessage());
        }
    }
}
