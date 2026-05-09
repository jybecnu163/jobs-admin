package com.example.jobadmin.controller;

import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.JobDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
