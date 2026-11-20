package com.example.jobadmin.service;

import com.example.jobadmin.entity.ConfigChangeLog;
import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.mapper.ConfigChangeLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配置导入服务：在同一个事务内记录变更审计日志并保存导入的任务定义。
 */
@Service
@RequiredArgsConstructor
public class ConfigSyncService {
    private final JobDefinitionService jobDefinitionService;
    private final ConfigChangeLogMapper changeLogMapper;

    /**
     * 在同一事务内逐条写 IMPORT 审计并覆盖任务定义；approved 固定为 1，表示导入时已通过 Token 审批。
     */
    @Transactional
    public void applyExportedPackage(List<JobDefinition> changedJobs, String operator) {
        for (JobDefinition job : changedJobs) {
            // 记录日志（简化）
            ConfigChangeLog log = new ConfigChangeLog();
            log.setJobName(job.getJobName());
            log.setChangeType("IMPORT");
            log.setNewValue(job.getScheduleExpr());
            log.setOperator(operator);
            log.setApproved(1); // 认为已经审批
            log.setApprovedAt(LocalDateTime.now());
            changeLogMapper.insert(log);

            // 更新或插入
            jobDefinitionService.saveOrUpdate(job);
        }
    }
}
