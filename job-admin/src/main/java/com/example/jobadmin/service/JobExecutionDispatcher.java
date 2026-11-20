package com.example.jobadmin.service;

import com.example.jobadmin.entity.ExecutorInstance;
import com.example.jobadmin.entity.JobDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指令下发服务：通过 HTTP REST 调用执行器的 execute/stop/running 接口，并透传执行器返回的错误信息。
 */
@Service
public class JobExecutionDispatcher {
    @Value("${job.admin.executor-token:}")
    private String executorToken;

    private final InstanceManager instanceManager;
    private final RestTemplate restTemplate;

    @Autowired
    public JobExecutionDispatcher(InstanceManager instanceManager) {
        this(instanceManager, new RestTemplate());
    }

    public JobExecutionDispatcher(InstanceManager instanceManager, RestTemplate restTemplate) {
        this.instanceManager = instanceManager;
        this.restTemplate = restTemplate;
    }

    private ExecutorInstance pickInstance(JobDefinition job) {
        if (job.getNamespace() == null || job.getNamespace().isBlank()) {
            return instanceManager.pickOnlineInstance(job.getSystemCode());
        }
        return instanceManager.pickOnlineInstance(job.getSystemCode(), job.getNamespace());
    }

    private <T> HttpEntity<T> entity(T body) {
        HttpHeaders headers = new HttpHeaders();
        if (executorToken != null && !executorToken.isBlank()) {
            headers.set("X-API-Token", executorToken);
        }
        return new HttpEntity<>(body, headers);
    }

    /**
     * 下发执行指令，按任务配置进行失败重试；成功后若配置超时则启动定时停止。
     */
    public void execute(JobDefinition job) {
        int retries = job.getRetryCount() == null ? 0 : Math.max(0, job.getRetryCount());
        long interval = job.getRetryIntervalMs() == null ? 1000 : Math.max(0, job.getRetryIntervalMs());
        RuntimeException lastError = null;
        for (int attempt = 0; attempt <= retries; attempt++) {
            try {
                doExecute(job);
                scheduleTimeout(job);
                return;
            } catch (RuntimeException e) {
                lastError = e;
                if (attempt < retries && interval > 0) {
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw lastError;
    }

    private void doExecute(JobDefinition job) {
        ExecutorInstance target = pickInstance(job);
        if (target == null) {
            throw new RuntimeException("无在线实例可执行任务: " + job.getJobName());
        }
        String url = String.format("http://%s:%d/executor/execute", target.getHost(), target.getPort());
        Map<String, Object> body = new HashMap<>();
        body.put("jobName", job.getJobName());
        body.put("shardIndex", job.getShardIndex() == null ? 0 : job.getShardIndex());
        body.put("shardTotal", job.getShardTotal() == null ? 1 : job.getShardTotal());
        try {
            restTemplate.postForObject(url, entity(body), String.class);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException(e.getResponseBodyAsString());
        }
    }

    private void scheduleTimeout(JobDefinition job) {
        if (job.getTimeoutMs() == null || job.getTimeoutMs() <= 0) {
            return;
        }
        Thread timer = new Thread(() -> {
            try {
                Thread.sleep(job.getTimeoutMs());
                stop(job);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "job-timeout-" + job.getJobName());
        timer.setDaemon(true);
        timer.start();
    }

    /**
     * 下发停止指令：按命名空间选在线实例，无实例直接抛错；与 execute 不同，此处不做重试。
     */
    public void stop(JobDefinition job) {
        ExecutorInstance target = pickInstance(job);
        if (target == null) {
            throw new RuntimeException("无在线实例可停止任务: " + job.getJobName());
        }
        String url = String.format("http://%s:%d/executor/stop", target.getHost(), target.getPort());
        restTemplate.postForObject(url, entity(Map.of("jobName", job.getJobName())), String.class);
    }

    /**
     * 查询指定实例上仍在运行中的任务名；单个实例查询异常由调用方决定如何降级。
     */
    public List<String> queryRunningJobs(ExecutorInstance target) {
        String url = String.format("http://%s:%d/executor/running", target.getHost(), target.getPort());
        String[] arr = restTemplate.exchange(url, HttpMethod.GET, entity(null), String[].class).getBody();
        return arr == null ? List.of() : Arrays.asList(arr);
    }

    /**
     * 按任务命名空间选实例并拉取最近日志；jobName 做 URL 编码，空响应返回空列表。
     */
    public List<String> queryLogs(JobDefinition job, String jobName, int limit) {
        ExecutorInstance target = pickInstance(job);
        if (target == null) {
            throw new RuntimeException("无在线实例可查询任务日志: " + jobName);
        }
        String encodedName = URLEncoder.encode(jobName, StandardCharsets.UTF_8);
        String url = String.format("http://%s:%d/executor/logs?jobName=%s&limit=%d",
                target.getHost(), target.getPort(), encodedName, limit);
        String[] arr = restTemplate.exchange(url, HttpMethod.GET, entity(null), String[].class).getBody();
        return arr == null ? List.of() : Arrays.asList(arr);
    }
}
