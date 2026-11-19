package com.example.jobadmin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.mapper.JobDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 任务定义服务：维护 Job 注册去重、可达状态流转（UNREACHABLE/ENABLED）与调度配置持久化。
 */
@Service
@RequiredArgsConstructor
public class JobDefinitionService {
    private final JobDefinitionMapper jobMapper;

    public void markUnreachableBySystem(String systemCode) {
        jobMapper.update(null, new LambdaUpdateWrapper<JobDefinition>()
                .eq(JobDefinition::getSystemCode, systemCode)
                .set(JobDefinition::getStatus, "UNREACHABLE"));
    }

    public void markOnlineBySystem(String systemCode) {
        // 将 UNREACHABLE 状态恢复为 ENABLED（实际项目应保留之前的 Enabled/Disabled 状态）
        jobMapper.update(null, new LambdaUpdateWrapper<JobDefinition>()
                .eq(JobDefinition::getSystemCode, systemCode)
                .eq(JobDefinition::getStatus, "UNREACHABLE")
                .set(JobDefinition::getStatus, "ENABLED"));
    }

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

    public void saveOrUpdate(JobDefinition job) {
        if (null != job.getId()) {
            jobMapper.updateById(job);
        } else {
            JobDefinition old = jobMapper.selectOne(new LambdaQueryWrapper<JobDefinition>()
                    .eq(JobDefinition::getSystemCode, job.getSystemCode())
                    .eq(JobDefinition::getJobName, job.getJobName()));
            if (null == old) {
                jobMapper.insert(job);
            } else {
                job.setId(old.getId());

                jobMapper.updateById(job);
            }

        }
    }

    public JobDefinition getById(Long jobId) {
        return jobMapper.selectById(jobId);
    }
}
