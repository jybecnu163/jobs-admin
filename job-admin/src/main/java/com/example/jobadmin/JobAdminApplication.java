package com.example.jobadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 调度中心启动入口，开启定时任务以支持 FIXED_RATE 自动调度与实例离线检测。
 */
@SpringBootApplication
@EnableScheduling
public class JobAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobAdminApplication.class, args);
    }
}
