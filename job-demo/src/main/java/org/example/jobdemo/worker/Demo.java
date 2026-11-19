package org.example.jobdemo.worker;

import com.example.jobexecutor.annotation.Job;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 示例任务类：提供短任务和长任务两个 @Job 方法。
 * 注意：JobAnnotationScanner 只扫描 Spring 管理的 Bean，因此该类必须标注 @Component。
 */
@Component
public class Demo {

    @Job(value = "test.demo.sayHello", description = "say hello with time", system = "job-demo", longRunning = false)
    public void sayHello() {
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " say hello to you!");
        try {
            TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " say hello to you again!");
    }

    @Job(value = "test.demo.longJob", description = "a long running job", system = "job-demo", longRunning = true)
    public void longJob() {
        while (!Thread.currentThread().isInterrupted()) {
            System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " long job running...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " long job stopped.");
    }
}
