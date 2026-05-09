package com.example.jobadmin.controller;

import com.example.jobadmin.service.InstanceManager;
import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.JobDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/callback")
@RequiredArgsConstructor
public class CallbackController {
    private final InstanceManager instanceManager;
    private final JobDefinitionService jobDefinitionService;

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
}
