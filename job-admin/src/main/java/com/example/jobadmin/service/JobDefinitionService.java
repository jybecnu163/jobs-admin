package com.example.jobadmin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.mapper.JobDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务定义服务：维护 Job 注册去重、可达状态流转（UNREACHABLE/ENABLED）与调度配置持久化。
 */
@Service
@RequiredArgsConstructor
public class JobDefinitionService {
    private final JobDefinitionMapper jobMapper;

    /**
     * 系统所有实例离线时，把该系统下任务批量标记为 UNREACHABLE，使其退出调度扫描。
     */
    public void markUnreachableBySystem(String systemCode) {
        jobMapper.update(null, new LambdaUpdateWrapper<JobDefinition>()
                .eq(JobDefinition::getSystemCode, systemCode)
                .set(JobDefinition::getStatus, "UNREACHABLE"));
    }

    /**
     * 实例恢复在线时仅把 UNREACHABLE 任务恢复可达性；最终 ENABLED/DISABLED 由 enabled 字段决定，保留管理员意图。
     */
    public void markOnlineBySystem(String systemCode) {
        // 仅恢复可达性，具体状态由管理员的 enabled 意图决定
        jobMapper.update(null, new LambdaUpdateWrapper<JobDefinition>()
                .eq(JobDefinition::getSystemCode, systemCode)
                .eq(JobDefinition::getStatus, "UNREACHABLE")
                .setSql("status = CASE WHEN enabled = 1 THEN 'ENABLED' ELSE 'DISABLED' END"));
    }

    /**
     * 调度扫描入口：返回 ENABLED 且非 NONE 的任务；不在此处关联实例表，在线校验由调度引擎完成。
     */
    public List<JobDefinition> getEnabledJobsWithOnlineInstance() {
        // 需要关联实例表？这里简化：状态为 ENABLED 的即参与调度（调度引擎会再检查实例在线）
        return jobMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<JobDefinition>()
                        .eq(JobDefinition::getStatus, "ENABLED")
                        .ne(JobDefinition::getScheduleType, "NONE")); // 排除手动触发任务
    }

    public List<JobDefinition> listAll() {
        return jobMapper.selectList(null);
    }

    /**
     * 按 id 或 (systemCode, jobName) 去重注册/更新任务；执行器注册未带 enabled 时保留管理员原有意图。
     */
    public void saveOrUpdate(JobDefinition job) {
        if (null != job.getId()) {
            resolveEnabled(job, jobMapper.selectById(job.getId()));
            jobMapper.updateById(job);
        } else {
            JobDefinition old = jobMapper.selectOne(new LambdaQueryWrapper<JobDefinition>()
                    .eq(JobDefinition::getSystemCode, job.getSystemCode())
                    .eq(JobDefinition::getJobName, job.getJobName()));
            if (null == old) {
                resolveEnabled(job, null);
                jobMapper.insert(job);
            } else {
                job.setId(old.getId());
                resolveEnabled(job, old);
                jobMapper.updateById(job);
            }
        }
    }

    /**
     * 执行器注册不带 enabled 字段时保留原有管理员意图；只有页面显式传入才更新。
     */
    private void resolveEnabled(JobDefinition job, JobDefinition old) {
        if (job.getEnabled() != null) {
            return;
        }
        if (old != null && old.getEnabled() != null) {
            job.setEnabled(old.getEnabled());
        } else {
            job.setEnabled(!"DISABLED".equals(job.getStatus()));
        }
    }

    public JobDefinition getById(Long jobId) {
        return jobMapper.selectById(jobId);
    }

    /**
     * 持久化调度引擎计算出的下次触发时间，供 CRON 任务跨重启保持调度节奏。
     */
    public void updateNextFireTime(Long jobId, LocalDateTime nextFireTime) {
        jobMapper.update(null, new LambdaUpdateWrapper<JobDefinition>()
                .eq(JobDefinition::getId, jobId)
                .set(JobDefinition::getNextFireTime, nextFireTime));
    }
}
