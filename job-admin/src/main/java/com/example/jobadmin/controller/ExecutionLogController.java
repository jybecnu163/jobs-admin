package com.example.jobadmin.controller;

import com.example.jobadmin.entity.JobExecutionLog;
import com.example.jobadmin.service.JobExecutionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 执行历史查询接口，供管理页面展示任务执行结果、耗时与异常信息。
 */
@RestController
@RequestMapping("/api/execution-logs")
@RequiredArgsConstructor
public class ExecutionLogController {
    private final JobExecutionLogService logService;

    /**
     * 查询最近执行记录；jobName 为空表示全部任务。
     */
    @GetMapping
    public ResponseEntity<List<JobExecutionLog>> recent(
            @RequestParam(required = false) String jobName,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(logService.recent(jobName, limit));
    }
}
