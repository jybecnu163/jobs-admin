package org.example.jobdemo.worker;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 长任务回归测试：验证停止（中断线程）后 longJob 能真正退出循环，而不是仅打印异常后继续运行。
 */
class DemoTest {

    @Test
    void longJobExitsWhenThreadIsInterrupted() throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        Thread worker = null;
        try {
            worker = new Thread(() -> new Demo().longJob(), "long-job-test-worker");
            worker.start();

            long deadline = System.currentTimeMillis() + 3000;
            while (!captured.toString(StandardCharsets.UTF_8).contains("long job running") &&
                    System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertTrue(captured.toString(StandardCharsets.UTF_8).contains("long job running"), "任务应已启动");

            worker.interrupt();
            worker.join(3000);
            assertFalse(worker.isAlive(), "中断后任务线程应退出");
            assertTrue(captured.toString(StandardCharsets.UTF_8).contains("long job stopped."), "应打印停止日志");
        } finally {
            if (worker != null && worker.isAlive()) {
                worker.interrupt();
            }
            System.setOut(originalOut);
        }
    }
}
