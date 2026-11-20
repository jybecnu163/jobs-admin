package com.example.jobadmin.gitops;

import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.JobDefinitionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * GitOps 基线加载器：服务启动时从 YAML 基线文件读取任务调度配置并落库，标记为基线锁定。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleFileLoader {
    private final JobDefinitionService jobDefinitionService;
    @Value("${job.admin.baseline-file}")
    private Resource baselineFile;

    /**
     * 启动时加载 YAML 基线：任务以 jobName 去重写入并标记 isBaselineLocked，文件不存在时仅告警。
     */
    @PostConstruct
    public void loadBaseline() {
        if (!baselineFile.exists()) {
            log.warn("基线配置文件不存在: {}", baselineFile.getFilename());
            return;
        }
        try (InputStream in = baselineFile.getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(in);
            List<Map<String, Object>> jobs = (List<Map<String, Object>>) root.get("jobs");
            if (jobs != null) {
                for (Map<String, Object> entry : jobs) {
                    JobDefinition job = new JobDefinition();
                    job.setJobName((String) entry.get("name"));
                    job.setSystemCode((String) entry.get("system"));
                    job.setScheduleType((String) entry.get("scheduleType"));
                    job.setScheduleExpr((String) entry.get("scheduleExpr"));
                    job.setLongRunning(Boolean.TRUE.equals(entry.get("longRunning")));
                    job.setDescription((String) entry.get("description"));
                    job.setStatus(Boolean.TRUE.equals(entry.get("enabled")) ? "ENABLED" : "DISABLED");
                    job.setEnabled(Boolean.TRUE.equals(entry.get("enabled")));
                    job.setIsBaselineLocked(true);
                    jobDefinitionService.saveOrUpdate(job);
                    log.info("基线加载 Job: {}", job.getJobName());
                }
            }
        } catch (Exception e) {
            log.error("加载基线配置失败", e);
        }
    }
}
