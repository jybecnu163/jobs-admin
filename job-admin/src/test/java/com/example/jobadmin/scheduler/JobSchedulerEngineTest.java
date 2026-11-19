package com.example.jobadmin.scheduler;

import com.example.jobadmin.entity.ExecutorInstance;
import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.JobDefinitionService;
import com.example.jobadmin.service.JobExecutionDispatcher;
import com.example.jobadmin.service.InstanceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 调度引擎单元测试：覆盖 FIXED_RATE 触发周期、防重复触发、实例在线校验与失败不记录触发时间等行为。
 */
@ExtendWith(MockitoExtension.class)
class JobSchedulerEngineTest {

    @Mock
    private JobDefinitionService jobDefinitionService;
    @Mock
    private JobExecutionDispatcher dispatcher;
    @Mock
    private InstanceManager instanceManager;
    @InjectMocks
    private JobSchedulerEngine engine;

    private static JobDefinition job(long id, String type, String expr) {
        JobDefinition job = new JobDefinition();
        job.setId(id);
        job.setJobName("job-" + id);
        job.setSystemCode("system-a");
        job.setScheduleType(type);
        job.setScheduleExpr(expr);
        return job;
    }

    private void online() {
        when(instanceManager.pickOnlineInstance("system-a")).thenReturn(new ExecutorInstance());
    }

    @SuppressWarnings("unchecked")
    private Map<Long, LocalDateTime> lastExecution() {
        return (Map<Long, LocalDateTime>) ReflectionTestUtils.getField(engine, "lastExecution");
    }

    @Test
    void fixedRateJobIsTriggeredOnceUntilIntervalElapses() {
        JobDefinition job = job(1L, "FIXED_RATE", "60000");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));
        online();

        engine.scheduleFixedRateJobs();
        engine.scheduleFixedRateJobs();
        verify(dispatcher, times(1)).execute(job);
        assertNotNull(lastExecution().get(1L));
    }

    @Test
    void fixedRateJobTriggersAgainAfterInterval() {
        JobDefinition job = job(2L, "FIXED_RATE", "1000");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));
        online();
        lastExecution().put(2L, LocalDateTime.now().minusSeconds(2));

        engine.scheduleFixedRateJobs();
        verify(dispatcher, times(1)).execute(job);
    }

    @Test
    void jobIsNotTriggeredWithoutOnlineInstance() {
        JobDefinition job = job(3L, "FIXED_RATE", "1000");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));
        when(instanceManager.pickOnlineInstance("system-a")).thenReturn(null);

        engine.scheduleFixedRateJobs();
        verify(dispatcher, never()).execute(any());
        assertNull(lastExecution().get(3L));
    }

    @Test
    void dispatchFailureDoesNotRecordTriggerTime() {
        JobDefinition job = job(4L, "FIXED_RATE", "1000");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));
        online();
        doThrow(new RuntimeException("下发失败")).when(dispatcher).execute(job);

        engine.scheduleFixedRateJobs();
        engine.scheduleFixedRateJobs();
        verify(dispatcher, times(2)).execute(job);
        assertNull(lastExecution().get(4L));
    }

    @Test
    void nonFixedRateJobsAreIgnored() {
        JobDefinition manual = job(5L, "NONE", "");
        JobDefinition cron = job(6L, "CRON", "0 0 * * * ?");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(manual, cron));

        engine.scheduleFixedRateJobs();
        verifyNoInteractions(dispatcher);
    }

    @Test
    void reloadAllClearsTriggerHistory() {
        JobDefinition job = job(7L, "FIXED_RATE", "60000");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));
        online();

        engine.scheduleFixedRateJobs();
        engine.reloadAll();
        engine.scheduleFixedRateJobs();
        verify(dispatcher, times(2)).execute(job);
    }
}
