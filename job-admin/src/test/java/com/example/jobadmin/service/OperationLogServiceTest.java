package com.example.jobadmin.service;

import com.example.jobadmin.entity.OperationLog;
import com.example.jobadmin.mapper.OperationLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

/**
 * 操作审计服务测试。
 */
@ExtendWith(MockitoExtension.class)
class OperationLogServiceTest {

    @Mock
    private OperationLogMapper mapper;
    @InjectMocks
    private OperationLogService service;

    @Test
    void recordPersistsOperation() {
        service.record("ScheduleController.executeJob(..)", "/api/schedule/execute/1", "[1]");

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(mapper).insert(captor.capture());
        assertEquals("admin", captor.getValue().getOperator());
        assertEquals("/api/schedule/execute/1", captor.getValue().getUri());
        assertNotNull(captor.getValue().getCreatedAt());
    }
}
