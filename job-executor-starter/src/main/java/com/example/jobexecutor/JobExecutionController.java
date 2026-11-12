package com.example.jobexecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.*;

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
    public ResponseEntity<String> execute(@RequestBody Map<String, String> request) {
        String jobName = request.get("jobName");
        Method method = scanner.getMethod(jobName);
        Object bean = scanner.getBean(jobName);
        if (method == null || bean == null) {
            return ResponseEntity.badRequest().body("Job not found: " + jobName);
        }
        // 先创建任务，再通过 putIfAbsent 原子地登记，避免并发提交绕过重复检查
        FutureTask<Void> task = new FutureTask<>(() -> {
            try {
                method.invoke(bean);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        while (true) {
            Future<?> prev = runningTasks.putIfAbsent(jobName, task);
            if (prev == null) {
                break;
            }
            if (!prev.isDone()) {
                // 同名任务仍在运行，拒绝本次提交
                return ResponseEntity.status(HttpStatus.CONFLICT).body("任务已在执行，提交失败");
            }
            // 旧任务已结束，清理残留记录后重试登记
            runningTasks.remove(jobName, prev);
        }
        threadPool.execute(task);
        return ResponseEntity.ok("executed");
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
