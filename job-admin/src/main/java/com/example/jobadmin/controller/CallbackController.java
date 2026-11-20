package com.example.jobadmin.controller;

import com.example.jobadmin.service.InstanceManager;
import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.JobDefinitionService;
import com.example.jobadmin.service.JobExecutionLogService;
import com.example.jobadmin.service.dto.ExecutionResultRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 执行器回调入口：接收执行器定时心跳与 @Job 方法注册请求，是执行器向 Admin 上报信息的通道。
 */
@RestController
@RequestMapping("/api/callback")
@RequiredArgsConstructor
public class CallbackController {
    private final InstanceManager instanceManager;
    private final JobDefinitionService jobDefinitionService;
    private final JobExecutionLogService executionLogService;

    // Executor 心跳
    @PostMapping("/heartbeat")
    public void heartbeat(@RequestParam String systemCode, @RequestParam String host, @RequestParam int port) {
        instanceManager.heartbeat(systemCode, host, port);
    }

    // Executor 注册 Job 定义
    @PostMapping("/register-job")
    public void registerJob(@RequestBody JobDefinition jobDef) {
        jobDefinitionService.saveOrUpdate(jobDef);
    }

    // Executor 执行结果上报
    @PostMapping("/execution-result")
    public void reportExecutionResult(@RequestBody ExecutionResultRequest request) {
        executionLogService.record(request);
    }
}
