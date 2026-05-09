package com.example.jobadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobAdminApplication.class, args);
    }
}
