package com.example.jobadmin.controller;

import com.example.jobadmin.entity.ExecutorInstance;
import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.InstanceManager;
import com.example.jobadmin.service.JobDefinitionService;
import com.example.jobadmin.service.JobExecutionDispatcher;
import com.example.jobadmin.service.JobEventBus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    @Autowired
    private JobEventBus eventBus;

    /**
     * 手动执行：先校验任务与在线实例，再下发指令并广播 SSE 状态；失败以 400 + 错误体返回。
     */
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
            eventBus.publish("job-status", job.getJobName());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok("执行指令已发送");
    }

    /**
     * 手动停止：下发中断指令后广播 SSE 状态，供页面刷新“在执行”状态。
     */
    @PostMapping("/stop/{jobId}")
    public void stopJob(@PathVariable Long jobId) {
        JobDefinition job = jobDefinitionService.listAll().stream()
                .filter(j -> j.getId().equals(jobId))
                .findFirst().orElseThrow();
        dispatcher.stop(job);
        eventBus.publish("job-status", job.getJobName());
    }

    /**
     * SSE 订阅入口：浏览器通过 EventSource 接收 job-status 事件，无需轮询任务状态。
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        return eventBus.subscribe();
    }

    /**
     * 汇总所有在线实例的运行中任务名；单个实例查询失败时静默跳过，不影响其他实例结果。
     */
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

    /**
     * 查询指定任务在在线执行器上采集的最近日志；任务不存在或查询失败返回 400 + 错误信息。
     */
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
