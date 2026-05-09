package com.example.jobexecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 作用：
 * <p>
 * 暴露两个 REST 接口，供 Admin 回调：
 * <p>
 * POST /executor/execute：接收 { "jobName": "..." }，通过反射调用对应的 @Job 方法。
 * <p>
 * 对于普通任务同步执行返回结果；对于长任务，会在线程池中异步执行，并将 Future 存入 runningTasks Map。
 * <p>
 * POST /executor/stop：接收任务名，从 runningTasks 中取出 Future 并执行 cancel(true) 尝试中断线程。
 * <p>
 * 实现了 Admin 下发执行指令与停止指令的核心通路。
 */
@RestController
@RequestMapping("/executor")
public class JobExecutionController {
    @Autowired
    private JobAnnotationScanner scanner;

    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Map<String, Future<?>> runningTasks = new ConcurrentHashMap<>();

    @PostMapping("/execute")
    public String execute(@RequestBody Map<String, String> request) {
        String jobName = request.get("jobName");
        Method method = scanner.getMethod(jobName);
        Object bean = scanner.getBean(jobName);
        if (method == null || bean == null) {
            return "Job not found: " + jobName;
        }
        // 对于长任务，异步执行并保存 Future
        Future<?> future = threadPool.submit(() -> {
            try {
                method.invoke(bean);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runningTasks.put(jobName, future);
        return "executed";
    }

    @PostMapping("/stop")
    public String stop(@RequestBody Map<String, String> request) {
        String jobName = request.get("jobName");
        Future<?> future = runningTasks.remove(jobName);
        if (future != null) {
            future.cancel(true);
            return "stopped";
        }
        return "no running task";
    }
}
