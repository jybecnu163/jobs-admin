package com.example.jobadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务执行记录实体，记录每次下发后的执行结果与耗时。
 */
@Data
@TableName("job_execution_log")
public class JobExecutionLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String jobName;
    private String systemCode;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String errorMessage;
    private LocalDateTime createdAt;
}
