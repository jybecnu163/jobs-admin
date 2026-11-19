package com.example.jobadmin.controller;

import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.JobDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务定义管理接口：供前端页面查询全部 Job 以及修改任务的调度配置、启用/禁用状态。
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobDefinitionController {
    private final JobDefinitionService jobDefinitionService;

    @GetMapping
    public List<JobDefinition> listAll() {
        return jobDefinitionService.listAll();
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @RequestBody JobDefinition job) {
        job.setId(id);
        jobDefinitionService.saveOrUpdate(job);
    }
}
