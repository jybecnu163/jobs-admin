package com.example.jobadmin.controller;

import com.example.jobadmin.service.AlertService;
import com.example.jobadmin.service.InstanceManager;
import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.JobDefinitionService;
import com.example.jobadmin.service.JobExecutionLogService;
import com.example.jobadmin.service.JobEventBus;
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
    private final JobEventBus eventBus;
    private final AlertService alertService;

    // Executor 心跳
    @PostMapping("/heartbeat")
    public void heartbeat(@RequestParam String systemCode, @RequestParam String host, @RequestParam int port,
                          @RequestParam(defaultValue = "default") String namespace) {
        instanceManager.heartbeat(systemCode, host, port, namespace);
    }

    // Executor 注册 Job 定义
    @PostMapping("/register-job")
    public void registerJob(@RequestBody JobDefinition jobDef) {
        jobDefinitionService.saveOrUpdate(jobDef);
    }

    /**
     * 接收执行结果：写入执行历史、广播 SSE 状态事件，失败时发送 Webhook 告警。
     */
    @PostMapping("/execution-result")
    public void reportExecutionResult(@RequestBody ExecutionResultRequest request) {
        executionLogService.record(request);
        eventBus.publish("job-status", request.getJobName());
        if ("FAILED".equals(request.getStatus())) {
            alertService.send("任务执行失败", request.getJobName() + " - " + request.getErrorMessage());
        }
    }
}
