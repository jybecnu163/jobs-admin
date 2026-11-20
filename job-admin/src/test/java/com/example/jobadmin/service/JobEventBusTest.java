package com.example.jobadmin.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SSE 事件总线测试：订阅与广播不抛异常。
 */
class JobEventBusTest {

    @Test
    void subscribeAndPublish() {
        JobEventBus bus = new JobEventBus();
        SseEmitter emitter = bus.subscribe();
        assertNotNull(emitter);

        assertDoesNotThrow(() -> bus.publish("job-status", "test.demo.longJob"));
    }

    @Test
    void publishWithoutSubscribersIsIgnored() {
        assertDoesNotThrow(() -> new JobEventBus().publish("job-status", "job.a"));
    }
}
