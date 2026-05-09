package com.example.jobadmin.service;

import com.example.jobadmin.entity.ExecutorInstance;
import com.example.jobadmin.entity.JobDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class JobExecutionDispatcher {
    private final InstanceManager instanceManager;
    private final RestTemplate restTemplate = new RestTemplate();

    public void execute(JobDefinition job) {
        ExecutorInstance target = instanceManager.pickOnlineInstance(job.getSystemCode());
        if (target == null) {
            throw new RuntimeException("无在线实例可执行任务: " + job.getJobName());
        }
        String url = String.format("http://%s:%d/executor/execute", target.getHost(), target.getPort());
        System.err.println("execute:" + job.getId() + " url:" + url);
        restTemplate.postForObject(url, Map.of("jobName", job.getJobName()), String.class);
    }

    public void stop(JobDefinition job) {
        ExecutorInstance target = instanceManager.pickOnlineInstance(job.getSystemCode());
        if (target == null) {
            throw new RuntimeException("无在线实例可停止任务: " + job.getJobName());
        }
        String url = String.format("http://%s:%d/executor/stop", target.getHost(), target.getPort());
        restTemplate.postForObject(url, Map.of("jobName", job.getJobName()), String.class);
    }
}
