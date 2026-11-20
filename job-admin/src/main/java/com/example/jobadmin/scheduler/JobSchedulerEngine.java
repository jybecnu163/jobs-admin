package com.example.jobadmin.scheduler;

import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.JobDefinitionService;
import com.example.jobadmin.service.JobExecutionDispatcher;
import com.example.jobadmin.service.InstanceManager;
import com.example.jobadmin.service.SchedulerLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 调度引擎：每 5 秒扫描启用的任务，支持 FIXED_RATE 与 CRON 两种调度类型。
 * 通过数据库分布式锁防止多 Admin 实例重复触发；CRON 的下次触发时间持久化到 job_definition.next_fire_time。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobSchedulerEngine {
    private static final Duration MAX_LOCK_TTL = Duration.ofSeconds(30);
    private static final Duration MIN_LOCK_TTL = Duration.ofSeconds(5);

    private final JobDefinitionService jobDefinitionService;
    private final JobExecutionDispatcher dispatcher;
    private final InstanceManager instanceManager;
    private final SchedulerLockService lockService;

    /**
     * 每 5 秒扫描一次：FIXED_RATE 按间隔判断，CRON 按持久化的下次触发时间判断；
     * 下发前先获取数据库锁并二次确认实例在线。
     */
    @Scheduled(fixedRate = 5000) // 每5秒扫描一次
    public void scheduleFixedRateJobs() {
        List<JobDefinition> jobs = jobDefinitionService.getEnabledJobsWithOnlineInstance();
        LocalDateTime now = LocalDateTime.now();
        for (JobDefinition job : jobs) {
            if ("FIXED_RATE".equals(job.getScheduleType())) {
                scheduleFixedRate(job, now);
            } else if ("CRON".equals(job.getScheduleType())) {
                scheduleCron(job, now);
            }
        }
    }

    private void scheduleFixedRate(JobDefinition job, LocalDateTime now) {
        long rateMillis;
        try {
            rateMillis = Long.parseLong(job.getScheduleExpr());
        } catch (NumberFormatException e) {
            log.error("FIXED_RATE 表达式非法: {} = {}", job.getJobName(), job.getScheduleExpr(), e);
            return;
        }
        Duration ttl = Duration.ofMillis(Math.max(rateMillis, 1000));
        if (!acquireLock(job, ttl) || !hasOnlineInstance(job)) {
            return;
        }
        try {
            dispatcher.execute(job);
            jobDefinitionService.updateNextFireTime(job.getId(), now.plus(Duration.ofMillis(rateMillis)));
            log.debug("触发 FIXED_RATE 任务: {}", job.getJobName());
        } catch (Exception e) {
            log.error("任务执行失败: {}", job.getJobName(), e);
        }
    }

    private void scheduleCron(JobDefinition job, LocalDateTime now) {
        CronExpression cron;
        try {
            cron = CronExpression.parse(job.getScheduleExpr());
        } catch (IllegalArgumentException e) {
            log.error("CRON 表达式非法: {} = {}", job.getJobName(), job.getScheduleExpr(), e);
            return;
        }
        LocalDateTime scheduled = job.getNextFireTime();
        if (scheduled == null) {
            LocalDateTime first = cron.next(now);
            if (first == null) {
                log.error("无法计算 CRON 下次触发时间: {}", job.getJobName());
                return;
            }
            jobDefinitionService.updateNextFireTime(job.getId(), first);
            job.setNextFireTime(first);
            scheduled = first;
        }
        if (now.isBefore(scheduled)) {
            return;
        }
        LocalDateTime next = advanceCron(cron, scheduled, now);
        Duration ttl = next == null ? MAX_LOCK_TTL : clampTtl(Duration.between(now, next));
        if (!acquireLock(job, ttl) || !hasOnlineInstance(job)) {
            return;
        }
        try {
            dispatcher.execute(job);
            if (next != null) {
                jobDefinitionService.updateNextFireTime(job.getId(), next);
                job.setNextFireTime(next);
            }
            log.debug("触发 CRON 任务: {}", job.getJobName());
        } catch (Exception e) {
            log.error("任务执行失败: {}", job.getJobName(), e);
        }
    }

    /**
     * 从原计划时间向后推进，找到严格晚于 now 的下次触发时间。
     */
    private LocalDateTime advanceCron(CronExpression cron, LocalDateTime scheduled, LocalDateTime now) {
        LocalDateTime next = scheduled;
        while (!next.isAfter(now)) {
            LocalDateTime candidate = cron.next(next);
            if (candidate == null || !candidate.isAfter(next)) {
                return cron.next(now);
            }
            next = candidate;
        }
        return next;
    }

    private Duration clampTtl(Duration interval) {
        if (interval.isNegative() || interval.compareTo(MIN_LOCK_TTL) < 0) {
            return MIN_LOCK_TTL;
        }
        return interval.compareTo(MAX_LOCK_TTL) > 0 ? MAX_LOCK_TTL : interval;
    }

    private boolean acquireLock(JobDefinition job, Duration ttl) {
        String lockName = "job-scheduler:" + job.getJobName();
        if (!lockService.tryAcquire(lockName, ttl)) {
            log.debug("未获取到调度锁，跳过任务: {}", job.getJobName());
            return false;
        }
        return true;
    }

    private boolean hasOnlineInstance(JobDefinition job) {
        return instanceManager.pickOnlineInstance(job.getSystemCode()) != null;
    }

    /**
     * 锁由数据库表管理并按 TTL 自动过期，无需清理内存状态。
     */
    public void reloadAll() {
        // 无内存状态需要重置
    }
}
