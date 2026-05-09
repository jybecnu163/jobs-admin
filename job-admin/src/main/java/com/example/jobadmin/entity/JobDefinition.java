package com.example.jobadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

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
    private String scheduleType; // CRON, FIXED_RATE, NONE
    private String scheduleExpr;
    private Boolean isBaselineLocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
