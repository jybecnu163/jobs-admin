package com.example.jobadmin.controller;

import com.example.jobadmin.entity.ExecutorInstance;
import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.InstanceManager;
import com.example.jobadmin.service.JobDefinitionService;
import com.example.jobadmin.service.JobExecutionDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {
    private final JobExecutionDispatcher dispatcher;
    private final JobDefinitionService jobDefinitionService;
    @Autowired
    private InstanceManager instanceManager;

    @PostMapping("/execute/{jobId}")
    public ResponseEntity<?> executeJob(@PathVariable Long jobId) {
        JobDefinition job = jobDefinitionService.getById(jobId);
        if (job == null) return ResponseEntity.notFound().build();

        ExecutorInstance target = instanceManager.pickOnlineInstance(job.getSystemCode());
        if (target == null) {
            return ResponseEntity.badRequest().body("该系统无在线实例，无法执行");
        }

        dispatcher.execute(job);
        return ResponseEntity.ok("执行指令已发送");
    }

    @PostMapping("/stop/{jobId}")
    public void stopJob(@PathVariable Long jobId) {
        JobDefinition job = jobDefinitionService.listAll().stream()
                .filter(j -> j.getId().equals(jobId))
                .findFirst().orElseThrow();
        dispatcher.stop(job);
    }
}
