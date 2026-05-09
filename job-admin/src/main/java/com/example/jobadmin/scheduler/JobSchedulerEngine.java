package com.example.jobadmin.scheduler;

import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.JobDefinitionService;
import com.example.jobadmin.service.JobExecutionDispatcher;
import com.example.jobadmin.service.InstanceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobSchedulerEngine {
    private final JobDefinitionService jobDefinitionService;
    private final JobExecutionDispatcher dispatcher;
    private final InstanceManager instanceManager;

    // 记录每个任务上次执行时间，避免重复触发的简单方案（实际应使用锁）
    private final Map<Long, LocalDateTime> lastExecution = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 5000) // 每5秒扫描一次
    public void scheduleFixedRateJobs() {
        List<JobDefinition> jobs = jobDefinitionService.getEnabledJobsWithOnlineInstance();
        LocalDateTime now = LocalDateTime.now();
        for (JobDefinition job : jobs) {
            if ("FIXED_RATE".equals(job.getScheduleType())) {
                long rateMillis = Long.parseLong(job.getScheduleExpr());
                LocalDateTime last = lastExecution.get(job.getId());
                if (last == null || java.time.Duration.between(last, now).toMillis() >= rateMillis) {
                    // 检查实例在线
                    if (instanceManager.pickOnlineInstance(job.getSystemCode()) != null) {
                        try {
                            dispatcher.execute(job);
                            lastExecution.put(job.getId(), now);
                            log.debug("触发 FIXED_RATE 任务: {}", job.getJobName());
                        } catch (Exception e) {
                            log.error("任务执行失败: {}", job.getJobName(), e);
                        }
                    }
                }
            }
            // CRON 任务简化处理：此处省略，实际应使用 CronExpression 计算下次触发时间
        }
    }

    // 注册启动时从数据库加载并计算初始时间
    public void reloadAll() {
        lastExecution.clear();
        // 简单重新加载，调度循环会自然处理
    }
}
