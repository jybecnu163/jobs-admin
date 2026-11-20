package com.example.jobadmin.controller;

import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.InstanceManager;
import com.example.jobadmin.service.JobDefinitionService;
import com.example.jobadmin.service.JobExecutionDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 手动调度控制器测试：覆盖任务日志查询端点。
 */
class ScheduleControllerTest {

    private JobExecutionDispatcher dispatcher;
    private JobDefinitionService jobDefinitionService;
    private ScheduleController controller;

    @BeforeEach
    void setUp() {
        dispatcher = mock(JobExecutionDispatcher.class);
        jobDefinitionService = mock(JobDefinitionService.class);
        controller = new ScheduleController(dispatcher, jobDefinitionService);
        ReflectionTestUtils.setField(controller, "instanceManager", mock(InstanceManager.class));
    }

    @Test
    void logsReturnsLinesFromDispatcher() {
        JobDefinition job = new JobDefinition();
        job.setId(1L);
        job.setJobName("test.demo.longJob");
        job.setSystemCode("job-demo");
        when(jobDefinitionService.listAll()).thenReturn(List.of(job));
        when(dispatcher.queryLogs(job, "test.demo.longJob", 200)).thenReturn(List.of("line-1", "line-2"));

        ResponseEntity<?> response = controller.logs("test.demo.longJob", 200);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of("line-1", "line-2"), response.getBody());
    }

    @Test
    void logsRejectsUnknownJob() {
        when(jobDefinitionService.listAll()).thenReturn(List.of());

        ResponseEntity<?> response = controller.logs("unknown", 200);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Job not found: unknown", response.getBody());
        verifyNoInteractions(dispatcher);
    }
}
