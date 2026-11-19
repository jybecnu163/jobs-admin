package com.example.jobadmin.service;

import com.example.jobadmin.entity.ExecutorInstance;
import com.example.jobadmin.entity.JobDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 指令下发服务：通过 HTTP REST 调用执行器的 execute/stop/running 接口，并透传执行器返回的错误信息。
 */
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
        try {
            restTemplate.postForObject(url, Map.of("jobName", job.getJobName()), String.class);
        } catch (HttpStatusCodeException e) {
            // 执行器返回 4xx/5xx 时（如重复执行 409），把原始错误信息抛给上层
            throw new RuntimeException(e.getResponseBodyAsString());
        }
    }

    public void stop(JobDefinition job) {
        ExecutorInstance target = instanceManager.pickOnlineInstance(job.getSystemCode());
        if (target == null) {
            throw new RuntimeException("无在线实例可停止任务: " + job.getJobName());
        }
        String url = String.format("http://%s:%d/executor/stop", target.getHost(), target.getPort());
        restTemplate.postForObject(url, Map.of("jobName", job.getJobName()), String.class);
    }

    // 查询指定执行器实例上仍在运行中的任务名列表
    public List<String> queryRunningJobs(ExecutorInstance target) {
        String url = String.format("http://%s:%d/executor/running", target.getHost(), target.getPort());
        String[] arr = restTemplate.getForObject(url, String[].class);
        return arr == null ? List.of() : Arrays.asList(arr);
    }
}
