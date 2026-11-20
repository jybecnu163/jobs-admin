package com.example.jobadmin.service;

import com.example.jobadmin.entity.JobExecutionLog;
import com.example.jobadmin.mapper.JobExecutionLogMapper;
import com.example.jobadmin.service.dto.ExecutionResultRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

/**
 * 执行记录服务单元测试：校验上报字段到实体的映射与时间解析。
 */
@ExtendWith(MockitoExtension.class)
class JobExecutionLogServiceTest {

    @Mock
    private JobExecutionLogMapper logMapper;
    @InjectMocks
    private JobExecutionLogService service;

    @Test
    void recordParsesTimesAndPersistsLog() {
        ExecutionResultRequest request = new ExecutionResultRequest();
        request.setJobName("test.demo.longJob");
        request.setSystemCode("job-demo");
        request.setStatus("FAILED");
        request.setStartTime("2026-11-20T10:00:00");
        request.setEndTime("2026-11-20T10:00:03");
        request.setDurationMs(3000L);
        request.setErrorMessage("boom");

        service.record(request);

        ArgumentCaptor<JobExecutionLog> captor = ArgumentCaptor.forClass(JobExecutionLog.class);
        verify(logMapper).insert(captor.capture());
        JobExecutionLog log = captor.getValue();
        assertEquals("test.demo.longJob", log.getJobName());
        assertEquals("job-demo", log.getSystemCode());
        assertEquals("FAILED", log.getStatus());
        assertEquals(LocalDateTime.of(2026, 11, 20, 10, 0), log.getStartTime());
        assertEquals(LocalDateTime.of(2026, 11, 20, 10, 0, 3), log.getEndTime());
        assertEquals(3000L, log.getDurationMs());
        assertEquals("boom", log.getErrorMessage());
    }

    @Test
    void recordAllowsNullTimes() {
        ExecutionResultRequest request = new ExecutionResultRequest();
        request.setJobName("job.a");
        request.setStatus("STOPPED");

        service.record(request);

        ArgumentCaptor<JobExecutionLog> captor = ArgumentCaptor.forClass(JobExecutionLog.class);
        verify(logMapper).insert(captor.capture());
        assertNull(captor.getValue().getStartTime());
        assertNull(captor.getValue().getEndTime());
    }
}
