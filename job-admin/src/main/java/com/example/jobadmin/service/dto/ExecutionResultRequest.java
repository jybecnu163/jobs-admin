package com.example.jobadmin.service.dto;

import lombok.Data;

/**
 * 执行器上报的任务执行结果。
 */
@Data
public class ExecutionResultRequest {
    private String jobName;
    private String systemCode;
    private String status;
    private String startTime;
    private String endTime;
    private Long durationMs;
    private String errorMessage;
}
