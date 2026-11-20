package com.example.jobadmin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 告警服务：配置 webhook 后把失败/离线事件以 JSON 推送到外部通知渠道。
 */
@Slf4j
@Service
public class AlertService {
    @Value("${job.admin.alert.webhook-url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void send(String title, String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            restTemplate.postForObject(webhookUrl, Map.of("text", title + "：" + message), Void.class);
        } catch (Exception e) {
            log.warn("告警发送失败: {}", e.getMessage());
        }
    }
}
