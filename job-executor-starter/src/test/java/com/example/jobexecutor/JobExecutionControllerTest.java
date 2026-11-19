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
    }

    @BeforeEach
    void setUp() {
        controller = new JobExecutionController();
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
