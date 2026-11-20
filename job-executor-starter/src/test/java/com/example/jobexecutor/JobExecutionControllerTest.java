package com.example.jobexecutor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 执行控制器单元测试：覆盖重复执行保护、停止中断和运行中状态查询等关键行为。
 */
class JobExecutionControllerTest {

    private JobExecutionController controller;
    private JobAnnotationScanner scanner;
    private ExecutionResultReporter reporter;
    private JobLogCollector logCollector;
    private CountDownLatch blockLatch;

    /** 测试任务：block() 会一直阻塞，直到 latch 释放或线程被中断；quick() 立即返回。 */
    public static class SampleJob {
        private final CountDownLatch latch;

        SampleJob(CountDownLatch latch) {
            this.latch = latch;
        }

        public void block() {
            try {
                latch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public void quick() {
        }

        public void fail() {
            throw new IllegalStateException("job failed");
        }
    }

    @BeforeEach
    void setUp() {
        reporter = mock(ExecutionResultReporter.class);
        logCollector = mock(JobLogCollector.class);
        controller = new JobExecutionController(reporter, logCollector);
        scanner = mock(JobAnnotationScanner.class);
        blockLatch = new CountDownLatch(1);
        ReflectionTestUtils.setField(controller, "scanner", scanner);
    }

    @AfterEach
    void tearDown() {
        blockLatch.countDown();
    }

    private void register(String jobName, String methodName) throws Exception {
        Method method = SampleJob.class.getMethod(methodName);
        when(scanner.getMethod(jobName)).thenReturn(method);
        when(scanner.getBean(jobName)).thenReturn(new SampleJob(blockLatch));
    }

    @Test
    void executeStartsJobAndReportsItAsRunning() throws Exception {
        register("test.job", "block");
        ResponseEntity<String> response = controller.execute(Map.of("jobName", "test.job"));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of("test.job"), controller.running());
    }

    @Test
    void duplicateExecuteIsRejectedWithConflict() throws Exception {
        register("test.job", "block");
        assertEquals(HttpStatus.OK, controller.execute(Map.of("jobName", "test.job")).getStatusCode());
        ResponseEntity<String> second = controller.execute(Map.of("jobName", "test.job"));
        assertEquals(HttpStatus.CONFLICT, second.getStatusCode());
        assertEquals("任务已在执行，提交失败", second.getBody());
        assertEquals(List.of("test.job"), controller.running());
    }

    @Test
    void executeAgainAllowedAfterPreviousJobFinished() throws Exception {
        register("test.job", "quick");
        assertEquals(HttpStatus.OK, controller.execute(Map.of("jobName", "test.job")).getStatusCode());
        awaitRunningEmpty();
        assertEquals(HttpStatus.OK, controller.execute(Map.of("jobName", "test.job")).getStatusCode());
        awaitRunningEmpty();
    }

    @Test
    void stopCancelsRunningJob() throws Exception {
        register("test.job", "block");
        controller.execute(Map.of("jobName", "test.job"));
        assertEquals("stopped", controller.stop(Map.of("jobName", "test.job")));
        assertTrue(controller.running().isEmpty());
        assertEquals("no running task", controller.stop(Map.of("jobName", "test.job")));
    }

    @Test
    void unknownJobIsRejected() {
        ResponseEntity<String> response = controller.execute(Map.of("jobName", "unknown"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Job not found"));
    }

    @Test
    void successResultIsReportedAfterJobFinishes() throws Exception {
        register("test.job", "quick");
        controller.execute(Map.of("jobName", "test.job"));
        awaitRunningEmpty();

        verify(reporter, timeout(2000)).report(eq("test.job"), eq("SUCCESS"), any(), any(), isNull());
    }

    @Test
    void stoppedResultIsReportedAfterCancel() throws Exception {
        register("test.job", "block");
        controller.execute(Map.of("jobName", "test.job"));
        controller.stop(Map.of("jobName", "test.job"));

        verify(reporter, timeout(2000)).report(eq("test.job"), eq("STOPPED"), any(), any(), any());
    }

    @Test
    void failedResultIsReportedWithErrorMessage() throws Exception {
        register("test.job", "fail");
        controller.execute(Map.of("jobName", "test.job"));
        awaitRunningEmpty();

        verify(reporter, timeout(2000)).report(eq("test.job"), eq("FAILED"), any(), any(), contains("job failed"));
    }

    @Test
    void logsEndpointReturnsCollectorResult() {
        when(logCollector.recent("test.job", 5)).thenReturn(List.of("line-1", "line-2"));

        assertEquals(List.of("line-1", "line-2"), controller.logs("test.job", 5));
    }

    private void awaitRunningEmpty() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!controller.running().isEmpty() && System.nanoTime() < deadline) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertTrue(controller.running().isEmpty(), "任务结束后应从运行列表中清除");
    }
}
