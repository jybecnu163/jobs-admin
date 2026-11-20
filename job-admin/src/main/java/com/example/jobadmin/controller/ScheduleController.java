package com.example.jobadmin.controller;

import com.example.jobadmin.entity.ExecutorInstance;
import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.InstanceManager;
import com.example.jobadmin.service.JobDefinitionService;
import com.example.jobadmin.service.JobExecutionDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 手动调度接口：下发执行/停止指令，并汇总所有在线执行器上正在运行的任务供前端展示。
 */
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

        try {
            dispatcher.execute(job);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok("执行指令已发送");
    }

    @PostMapping("/stop/{jobId}")
    public void stopJob(@PathVariable Long jobId) {
        JobDefinition job = jobDefinitionService.listAll().stream()
                .filter(j -> j.getId().equals(jobId))
                .findFirst().orElseThrow();
        dispatcher.stop(job);
    }

    // 汇总所有在线执行器上正在运行的任务名，供前端展示"在执行"状态
    @GetMapping("/running")
    public List<String> runningJobs() {
        List<String> running = new ArrayList<>();
        for (ExecutorInstance inst : instanceManager.listOnlineInstances()) {
            try {
                running.addAll(dispatcher.queryRunningJobs(inst));
            } catch (Exception e) {
                // 单个实例查询失败不影响整体
            }
        }
        return running;
    }

    // 查询指定任务在在线执行器上采集的最近日志
    @GetMapping("/logs")
    public ResponseEntity<?> logs(@RequestParam String jobName,
                                  @RequestParam(defaultValue = "200") int limit) {
        JobDefinition job = jobDefinitionService.listAll().stream()
                .filter(j -> jobName.equals(j.getJobName()))
                .findFirst().orElse(null);
        if (job == null) {
            return ResponseEntity.badRequest().body("Job not found: " + jobName);
        }
        try {
            return ResponseEntity.ok(dispatcher.queryLogs(job, jobName, limit));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
