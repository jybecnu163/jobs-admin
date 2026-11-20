package com.example.jobadmin.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 告警服务测试：校验 webhook 开启与关闭时的行为。
 */
class AlertServiceTest {

    private AlertService serviceWithUrl(String url, RestTemplate restTemplate) {
        AlertService service = new AlertService();
        ReflectionTestUtils.setField(service, "webhookUrl", url);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        return service;
    }

    @Test
    void sendsWebhookWhenConfigured() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://localhost:9999/alert"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.text").value("任务执行失败：job.a - boom"))
                .andRespond(withSuccess());

        serviceWithUrl("http://localhost:9999/alert", restTemplate).send("任务执行失败", "job.a - boom");
        server.verify();
    }

    @Test
    void skipsWebhookWhenNotConfigured() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        serviceWithUrl("", restTemplate).send("任务执行失败", "job.a - boom");
        server.verify();
    }
}
