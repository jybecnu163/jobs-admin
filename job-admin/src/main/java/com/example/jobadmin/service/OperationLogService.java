package com.example.jobadmin.service;

import com.example.jobadmin.entity.OperationLog;
import com.example.jobadmin.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 操作审计服务。
 */
@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final OperationLogMapper mapper;

    /**
     * 写入一条操作审计日志；operator 固定为 admin，params 为方法入参的字符串表示。
     */
    public void record(String operation, String uri, String params) {
        OperationLog log = new OperationLog();
        log.setOperator("admin");
        log.setOperation(operation);
        log.setUri(uri);
        log.setParams(params);
        log.setCreatedAt(LocalDateTime.now());
        mapper.insert(log);
    }
}
