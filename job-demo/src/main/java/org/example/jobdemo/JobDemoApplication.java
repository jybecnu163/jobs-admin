package org.example.jobdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 示例业务系统启动入口，用于演示以 Starter 方式接入调度系统并执行 @Job 任务。
 */
@SpringBootApplication
public class JobDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobDemoApplication.class, args);
    }

}
