package com.example.jobadmin.service;

import com.example.jobadmin.entity.ExecutorInstance;
import com.example.jobadmin.entity.JobDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * 指令下发服务测试：覆盖失败重试与超时自动停止。
 */
class JobExecutionDispatcherTest {

    private InstanceManager instanceManager;
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private JobExecutionDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        instanceManager = mock(InstanceManager.class);
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        dispatcher = new JobExecutionDispatcher(instanceManager, restTemplate);
        ExecutorInstance instance = new ExecutorInstance();
        instance.setHost("10.0.0.1");
        instance.setPort(8082);
        lenient().when(instanceManager.pickOnlineInstance("job-demo")).thenReturn(instance);
    }

    private JobDefinition job() {
        JobDefinition job = new JobDefinition();
        job.setId(1L);
        job.setJobName("test.job");
        job.setSystemCode("job-demo");
        return job;
    }

    @Test
    void executeRetriesOnFailure() {
        JobDefinition job = job();
        job.setRetryCount(1);
        job.setRetryIntervalMs(0L);
        String url = "http://10.0.0.1:8082/executor/execute";
        server.expect(requestTo(url)).andExpect(method(HttpMethod.POST)).andRespond(withServerError());
        server.expect(requestTo(url)).andExpect(method(HttpMethod.POST)).andRespond(withSuccess());

        assertDoesNotThrow(() -> dispatcher.execute(job));
        server.verify();
    }

    @Test
    void executeStopsJobAfterTimeout() throws Exception {
        JobDefinition job = job();
        job.setTimeoutMs(50L);
        server.expect(requestTo("http://10.0.0.1:8082/executor/execute"))
                .andRespond(withSuccess());
        server.expect(requestTo("http://10.0.0.1:8082/executor/stop"))
                .andRespond(withSuccess());

        dispatcher.execute(job);
        Thread.sleep(400);
        server.verify();
    }

    @Test
    void executeUsesNamespaceInstance() {
        JobDefinition job = job();
        job.setNamespace("gray");
        ExecutorInstance gray = new ExecutorInstance();
        gray.setHost("10.0.0.2");
        gray.setPort(8082);
        when(instanceManager.pickOnlineInstance("job-demo", "gray")).thenReturn(gray);
        server.expect(requestTo("http://10.0.0.2:8082/executor/execute"))
                .andRespond(withSuccess());

        dispatcher.execute(job);
        server.verify();
    }

    @Test
    void executeWithoutRetryFailsImmediately() {
        JobDefinition job = job();
        server.expect(requestTo("http://10.0.0.1:8082/executor/execute"))
                .andRespond(withServerError());

        assertThrows(RuntimeException.class, () -> dispatcher.execute(job));
        server.verify();
    }
}
