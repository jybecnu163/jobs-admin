package com.example.jobadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 任务定义实体，一条记录对应执行器上的一个 @Job 方法；调度配置由 Admin 侧管理。
 * status：ENABLED / DISABLED / UNREACHABLE；scheduleType：NONE / FIXED_RATE / CRON。
 */
@Data
@TableName("job_definition")
public class JobDefinition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String jobName;
    private String systemCode;
    private String className;
    private String methodName;
    private Boolean longRunning;
    private String description;
    private String status;       // ENABLED, DISABLED, UNREACHABLE
    private Boolean enabled;     // 管理员意图，实例离线/上线不改变该值
    private String scheduleType; // CRON, FIXED_RATE, NONE
    private String scheduleExpr;
    private Boolean isBaselineLocked;
    private LocalDateTime nextFireTime; // 下次触发时间，由调度引擎计算并持久化
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
