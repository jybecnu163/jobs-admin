package com.example.jobadmin.controller;

import com.example.jobadmin.entity.JobExecutionLog;
import com.example.jobadmin.service.JobExecutionLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 执行历史查询控制器测试。
 */
@ExtendWith(MockitoExtension.class)
class ExecutionLogControllerTest {

    @Mock
    private JobExecutionLogService logService;
    @InjectMocks
    private ExecutionLogController controller;

    @Test
    void recentReturnsLogsWithJobFilter() {
        when(logService.recent("job.a", 100)).thenReturn(List.of(new JobExecutionLog()));

        assertEquals(1, controller.recent("job.a", 100).getBody().size());
        verify(logService).recent("job.a", 100);
    }

    @Test
    void recentAllowsEmptyJobName() {
        when(logService.recent(null, 100)).thenReturn(List.of());

        assertTrue(controller.recent(null, 100).getBody().isEmpty());
        verify(logService).recent(null, 100);
    }
}
