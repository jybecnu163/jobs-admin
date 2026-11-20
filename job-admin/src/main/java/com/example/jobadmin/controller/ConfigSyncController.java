package com.example.jobadmin.controller;

import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.ConfigSyncService;
import com.example.jobadmin.service.JobDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 配置同步接口：导出全部任务定义，导入时校验审批 Token 后由 ConfigSyncService 落库并记录变更日志。
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigSyncController {
    private final JobDefinitionService jobDefinitionService;
    private final ConfigSyncService configSyncService;

    @Value("${job.admin.import.approval-token}")
    private String approvalToken;

    // http://localhost:8080/api/config/export
    @GetMapping("/export")
    public List<JobDefinition> exportAll() {
        // 导出所有非基线锁定的 Job（简化：导出全部）
        return jobDefinitionService.listAll();
    }

    /**
     * 配置导入：校验 X-Approval-Token 后覆盖写入任务定义，Token 不匹配直接抛异常由全局处理器转 500。
     */
    @PostMapping("/import")
    public void importConfig(@RequestBody List<JobDefinition> jobs,
                             @RequestHeader("X-Approval-Token") String token) {
        if (!approvalToken.equals(token)) {
            throw new RuntimeException("审批Token无效");
        }
        configSyncService.applyExportedPackage(jobs, "admin");
    }
}
