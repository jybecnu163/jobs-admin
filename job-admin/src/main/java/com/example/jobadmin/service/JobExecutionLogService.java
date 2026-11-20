package com.example.jobadmin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.jobadmin.entity.JobExecutionLog;
import com.example.jobadmin.mapper.JobExecutionLogMapper;
import com.example.jobadmin.service.dto.ExecutionResultRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务执行记录服务：把执行器上报的结果落库，供后续日志查询与告警使用。
 */
@Service
@RequiredArgsConstructor
public class JobExecutionLogService {
    private final JobExecutionLogMapper logMapper;

    /**
     * 将执行结果写入 job_execution_log；时间字段为 ISO-8601 字符串。
     */
    public void record(ExecutionResultRequest request) {
        JobExecutionLog log = new JobExecutionLog();
        log.setJobName(request.getJobName());
        log.setSystemCode(request.getSystemCode());
        log.setStatus(request.getStatus());
        log.setStartTime(parse(request.getStartTime()));
        log.setEndTime(parse(request.getEndTime()));
        log.setDurationMs(request.getDurationMs());
        log.setErrorMessage(request.getErrorMessage());
        logMapper.insert(log);
    }

    /**
     * 查询最近执行记录；jobName 为空时返回全部任务，limit 限制在 1–500。
     */
    public List<JobExecutionLog> recent(String jobName, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return logMapper.selectList(new LambdaQueryWrapper<JobExecutionLog>()
                .eq(jobName != null && !jobName.isBlank(), JobExecutionLog::getJobName, jobName)
                .orderByDesc(JobExecutionLog::getStartTime)
                .last("limit " + safeLimit));
    }

    private LocalDateTime parse(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }
}
