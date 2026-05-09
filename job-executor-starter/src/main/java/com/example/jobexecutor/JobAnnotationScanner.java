package com.example.jobexecutor;

import com.example.jobexecutor.annotation.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class JobAnnotationScanner implements ApplicationListener<ApplicationReadyEvent> {
    private final ExecutorProperties properties;
    private final ApplicationContext applicationContext;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${server.port:8080}")
    private int serverPort;

    // 存储 jobName -> Method 的映射
    private final Map<String, Method> jobMethodMap = new HashMap<>();
    // 存储 jobName -> bean 实例
    private final Map<String, Object> jobBeanMap = new HashMap<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        scanAndRegister();
    }

    @Override
    public boolean supportsAsyncExecution() {
        return ApplicationListener.super.supportsAsyncExecution();
    }

    public void scanAndRegister() {
        // 扫描所有 bean
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> clazz = bean.getClass();
            for (Method method : clazz.getDeclaredMethods()) {
                Job jobAnno = method.getAnnotation(Job.class);
                if (jobAnno != null) {
                    String jobName = jobAnno.value();
                    jobMethodMap.put(jobName, method);
                    jobBeanMap.put(jobName, bean);
                    log.info("发现 @Job: {} -> {}.{}()", jobName, clazz.getSimpleName(), method.getName());
                }
            }
        }

        // 向 Admin 注册
        if (!jobMethodMap.isEmpty()) {
            try {
                String host = InetAddress.getLocalHost().getHostAddress();
                // 注册心跳
                restTemplate.postForObject(
                        properties.getAdminUrl() + "/api/callback/heartbeat?systemCode={1}&host={2}&port={3}",
                        null, Void.class,
                        properties.getSystemCode(), host, serverPort);
                // 注册每个 Job 定义
                jobMethodMap.forEach((name, method) -> {
                    Map<String, Object> jobDef = new HashMap<>();
                    jobDef.put("jobName", name);
                    jobDef.put("systemCode", properties.getSystemCode());
                    jobDef.put("className", method.getDeclaringClass().getName());
                    jobDef.put("methodName", method.getName());
                    Job anno = method.getAnnotation(Job.class);
                    jobDef.put("longRunning", anno.longRunning());
                    jobDef.put("description", anno.description());
                    jobDef.put("scheduleType", "NONE"); // 默认手动，调度配置由 Admin 管理
                    jobDef.put("status", "ENABLED");
                    restTemplate.postForObject(
                            properties.getAdminUrl() + "/api/callback/register-job",
                            jobDef, Void.class);
                });
            } catch (Exception e) {
                log.error("向 Admin 注册失败", e);
            }
        }
    }

    public Method getMethod(String jobName) {
        return jobMethodMap.get(jobName);
    }

    public Object getBean(String jobName) {
        return jobBeanMap.get(jobName);
    }


}
