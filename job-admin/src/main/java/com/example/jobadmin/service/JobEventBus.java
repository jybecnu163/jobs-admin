package com.example.jobadmin.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 向浏览器推送调度事件的 SSE 事件总线。
 */
@Service
public class JobEventBus {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * 新建永不超时的 SSE 连接；完成、超时或出错时自动从连接列表移除，避免连接泄漏。
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    /**
     * 广播事件；单个连接失败不影响其他连接。
     */
    public void publish(String event, Object data) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(event).data(data));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }
}
