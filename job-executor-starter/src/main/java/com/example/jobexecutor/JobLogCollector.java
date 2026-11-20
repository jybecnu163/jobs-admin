package com.example.jobexecutor;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 按任务名收集最近日志行，供 Admin 在线查看；通过线程绑定区分并发任务的输出。
 */
@Slf4j
@RequiredArgsConstructor
public class JobLogCollector {
    private static final int MAX_LINES = 200;

    private final ExecutorProperties properties;
    private final Map<String, Deque<String>> logs = new ConcurrentHashMap<>();
    private final ThreadLocal<String> currentJob = new ThreadLocal<>();

    /**
     * 安装 System.out 路由流，使任务中的 println 输出同时写入内存缓冲区。
     */
    @PostConstruct
    public void install() {
        if (!properties.isLogCaptureEnabled() || System.out instanceof RoutingPrintStream) {
            return;
        }
        System.setOut(new RoutingPrintStream(System.out, this));
        log.info("已启用任务日志采集，最多保留 {} 行/任务", MAX_LINES);
    }

    public void setCurrentJob(String jobName) {
        currentJob.set(jobName);
    }

    public void clearCurrentJob() {
        currentJob.remove();
    }

    public void append(String line) {
        String jobName = currentJob.get();
        if (jobName == null || line == null) {
            return;
        }
        Deque<String> buffer = logs.computeIfAbsent(jobName, k -> new ConcurrentLinkedDeque<>());
        buffer.addLast(line);
        while (buffer.size() > MAX_LINES) {
            buffer.pollFirst();
        }
    }

    /**
     * 返回指定任务最近 limit 行日志。
     */
    public List<String> recent(String jobName, int limit) {
        Deque<String> buffer = logs.get(jobName);
        if (buffer == null || limit <= 0) {
            return List.of();
        }
        List<String> all = new ArrayList<>(buffer);
        int from = Math.max(0, all.size() - limit);
        return new ArrayList<>(all.subList(from, all.size()));
    }
}
