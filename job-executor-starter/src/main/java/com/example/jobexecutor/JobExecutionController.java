package com.example.jobexecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

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

    private final ExecutionResultReporter reporter;
    private final JobLogCollector logCollector;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Map<String, Future<?>> runningTasks = new ConcurrentHashMap<>();

    public JobExecutionController(ExecutionResultReporter reporter, JobLogCollector logCollector) {
        this.reporter = reporter;
        this.logCollector = logCollector;
    }

    /**
     * 执行任务：同名任务仍在运行时返回 409 拒绝提交，避免旧 Future 被覆盖后无法停止。
     */
    @PostMapping("/execute")
    public ResponseEntity<String> execute(@RequestBody Map<String, String> request) {
        String jobName = request.get("jobName");
        Method method = scanner.getMethod(jobName);
        Object bean = scanner.getBean(jobName);
        if (method == null || bean == null) {
            return ResponseEntity.badRequest().body("Job not found: " + jobName);
        }
        // 先创建任务，再通过 putIfAbsent 原子地登记，避免并发提交绕过重复检查
        AtomicReference<LocalDateTime> startedAt = new AtomicReference<>();
        AtomicReference<LocalDateTime> finishedAt = new AtomicReference<>();
        AtomicReference<String> errorMessage = new AtomicReference<>();
        FutureTask<Void> task = new FutureTask<>(() -> {
            startedAt.set(LocalDateTime.now());
            logCollector.setCurrentJob(jobName);
            try {
                method.invoke(bean);
            } catch (Exception e) {
                Throwable root = e;
                while (root.getCause() != null) {
                    root = root.getCause();
                }
                errorMessage.set(root.toString());
                throw new RuntimeException(e);
            } finally {
                finishedAt.set(LocalDateTime.now());
                logCollector.clearCurrentJob();
            }
            return null;
        }) {
            @Override
            protected void done() {
                String status = "SUCCESS";
                if (isCancelled()) {
                    status = "STOPPED";
                } else {
                    try {
                        get();
                    } catch (ExecutionException e) {
                        status = "FAILED";
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        status = "FAILED";
                    }
                }
                reporter.report(jobName, status, startedAt.get(), finishedAt.get(), errorMessage.get());
            }
        };
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

    /**
     * 停止任务：移除对应 Future 并通过中断方式尝试终止任务线程，依赖任务代码正确响应中断。
     */
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

    /**
     * 返回当前仍在运行中的任务名列表，供 Admin 查询展示"在执行"状态；顺带清理已结束的残留记录。
     */
    @GetMapping("/running")
    public List<String> running() {
        List<String> running = new ArrayList<>();
        runningTasks.forEach((jobName, future) -> {
            if (!future.isDone()) {
                running.add(jobName);
            } else {
                runningTasks.remove(jobName, future);
            }
        });
        return running;
    }

    /**
     * 返回指定任务最近采集的日志行。
     */
    @GetMapping("/logs")
    public List<String> logs(@RequestParam String jobName,
                             @RequestParam(defaultValue = "200") int limit) {
        return logCollector.recent(jobName, limit);
    }
}
