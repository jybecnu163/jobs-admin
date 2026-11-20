package com.example.jobexecutor;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.time.Duration;

/**
 * 作用：
 * <p>
 * 使用 ThreadPoolTaskScheduler 定时（默认 10 秒）向 Admin 发送心跳请求。
 * <p>
 * 心跳包携带 systemCode、host、port。
 * <p>
 * Admin 端通过对比最后心跳时间判断实例是否存活，若超时则标记 OFFLINE，联动标记该系统的所有 Job 为 UNREACHABLE。
 * <p>
 * 保证了业务系统宕机或网络断开后，Admin 能及时感知，禁止调度
 */
@Slf4j
@RequiredArgsConstructor
public class HeartbeatSender {
    private final ExecutorProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * 启动后按 heartbeat-interval 定时上报心跳；发送失败仅告警，Admin 侧据此判定实例在线与任务可达性。
     */
    @PostConstruct
    public void startHeartbeat() {
        scheduler.setPoolSize(1);
        scheduler.initialize();
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                String host = InetAddress.getLocalHost().getHostAddress();
                restTemplate.postForObject(
                        properties.getAdminUrl() + "/api/callback/heartbeat?systemCode={1}&host={2}&port={3}&namespace={4}",
                        null, Void.class,
                        properties.getSystemCode(), host, serverPort, properties.getNamespace());
            } catch (Exception e) {
                log.warn("心跳发送失败: {}", e.getMessage());
            }
        }, Duration.ofSeconds(properties.getHeartbeatInterval()));
    }
}
