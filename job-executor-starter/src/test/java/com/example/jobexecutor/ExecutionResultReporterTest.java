package com.example.jobexecutor;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 执行结果上报器测试：校验回调 URL 与请求体字段。
 */
class ExecutionResultReporterTest {

    private ExecutorProperties properties() {
        ExecutorProperties properties = new ExecutorProperties();
        properties.setAdminUrl("http://localhost:8080");
        properties.setSystemCode("job-demo");
        return properties;
    }

    @Test
    void reportPostsExpectedFieldsToAdmin() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://localhost:8080/api/callback/execution-result"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.jobName").value("test.job"))
                .andExpect(jsonPath("$.systemCode").value("job-demo"))
                .andExpect(jsonPath("$.executorHost").exists())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.errorMessage").doesNotExist())
                .andRespond(withSuccess());

        LocalDateTime now = LocalDateTime.now();
        new ExecutionResultReporter(properties(), restTemplate)
                .report("test.job", "SUCCESS", now, now.plusSeconds(1), null);
        server.verify();
    }

    @Test
    void disabledReporterSkipsCallback() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ExecutorProperties properties = properties();
        properties.setResultCallbackEnabled(false);

        new ExecutionResultReporter(properties, restTemplate)
                .report("test.job", "SUCCESS", LocalDateTime.now(), LocalDateTime.now(), null);
        server.verify();
    }
}
