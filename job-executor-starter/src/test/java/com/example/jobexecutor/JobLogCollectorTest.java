package com.example.jobexecutor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 日志采集器测试：校验按任务隔离、最近 N 行与容量上限。
 */
class JobLogCollectorTest {

    private JobLogCollector collector() {
        ExecutorProperties properties = new ExecutorProperties();
        properties.setLogCaptureEnabled(false);
        return new JobLogCollector(properties);
    }

    @Test
    void recentReturnsLastNLinesForCurrentJob() {
        JobLogCollector collector = collector();
        collector.setCurrentJob("job-a");
        collector.append("line-1");
        collector.append("line-2");
        collector.append("line-3");
        collector.clearCurrentJob();

        assertEquals(List.of("line-2", "line-3"), collector.recent("job-a", 2));
        assertTrue(collector.recent("job-b", 10).isEmpty());
    }

    @Test
    void bufferKeepsAtMost200Lines() {
        JobLogCollector collector = collector();
        collector.setCurrentJob("job-a");
        for (int i = 1; i <= 250; i++) {
            collector.append("line-" + i);
        }

        List<String> lines = collector.recent("job-a", 500);
        assertEquals(200, lines.size());
        assertEquals("line-51", lines.get(0));
        assertEquals("line-250", lines.get(199));
    }
}
