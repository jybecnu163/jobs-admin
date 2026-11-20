package com.example.jobadmin.scheduler;

import com.example.jobadmin.entity.ExecutorInstance;
import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.JobDefinitionService;
import com.example.jobadmin.service.JobExecutionDispatcher;
import com.example.jobadmin.service.InstanceManager;
import com.example.jobadmin.service.SchedulerLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 调度引擎单元测试：覆盖数据库锁获取、FIXED_RATE/CRON 触发判断、实例在线校验与失败处理。
 */
@ExtendWith(MockitoExtension.class)
class JobSchedulerEngineTest {

    @Mock
    private JobDefinitionService jobDefinitionService;
    @Mock
    private JobExecutionDispatcher dispatcher;
    @Mock
    private InstanceManager instanceManager;
    @Mock
    private SchedulerLockService lockService;
    @InjectMocks
    private JobSchedulerEngine engine;

    @BeforeEach
    void setUp() {
        lenient().when(lockService.tryAcquire(anyString(), any(Duration.class))).thenReturn(true);
    }

    private static JobDefinition job(long id, String type, String expr) {
        JobDefinition job = new JobDefinition();
        job.setId(id);
        job.setJobName("job-" + id);
        job.setSystemCode("system-a");
        job.setScheduleType(type);
        job.setScheduleExpr(expr);
        return job;
    }

    @Test
    void jobWithUnmetDependencyIsSkipped() {
        JobDefinition downstream = job(102L, "FIXED_RATE", "1000");
        downstream.setDependsOn("job-101");
        JobDefinition upstream = job(101L, "FIXED_RATE", "1000");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance())
                .thenReturn(List.of(downstream, upstream));
        online();

        engine.scheduleFixedRateJobs();

        verify(dispatcher, never()).execute(downstream);
        verify(dispatcher, times(1)).execute(upstream);
    }

    private void online() {
        when(instanceManager.pickOnlineInstance("system-a")).thenReturn(new ExecutorInstance());
    }

    @Test
    void fixedRateJobTriggersWhenLockAcquired() {
        JobDefinition job = job(1L, "FIXED_RATE", "60000");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));
        online();

        engine.scheduleFixedRateJobs();

        verify(lockService).tryAcquire("job-scheduler:job-1", Duration.ofMillis(60000));
        verify(dispatcher, times(1)).execute(job);
        verify(jobDefinitionService).updateNextFireTime(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void fixedRateJobSkippedWhenLockNotAcquired() {
        JobDefinition job = job(2L, "FIXED_RATE", "1000");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));
        when(lockService.tryAcquire(anyString(), any(Duration.class))).thenReturn(false);

        engine.scheduleFixedRateJobs();
        verifyNoInteractions(dispatcher);
        verify(jobDefinitionService, never()).updateNextFireTime(anyLong(), any());
    }

    @Test
    void fixedRateJobTriggersAgainAfterLockExpires() {
        JobDefinition job = job(3L, "FIXED_RATE", "1000");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));
        online();

        engine.scheduleFixedRateJobs();
        engine.scheduleFixedRateJobs();

        verify(dispatcher, times(2)).execute(job);
    }

    @Test
    void jobIsNotTriggeredWithoutOnlineInstance() {
        JobDefinition job = job(4L, "FIXED_RATE", "1000");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));
        when(instanceManager.pickOnlineInstance("system-a")).thenReturn(null);

        engine.scheduleFixedRateJobs();
        verifyNoInteractions(dispatcher);
        verify(jobDefinitionService, never()).updateNextFireTime(anyLong(), any());
    }

    @Test
    void dispatchFailureDoesNotAdvanceNextFireTime() {
        JobDefinition job = job(5L, "FIXED_RATE", "1000");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));
        online();
        doThrow(new RuntimeException("下发失败")).when(dispatcher).execute(job);

        engine.scheduleFixedRateJobs();
        engine.scheduleFixedRateJobs();

        verify(dispatcher, times(2)).execute(job);
        verify(jobDefinitionService, never()).updateNextFireTime(anyLong(), any());
    }

    @Test
    void manualJobsAreIgnored() {
        JobDefinition manual = job(6L, "NONE", "");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(manual));

        engine.scheduleFixedRateJobs();
        verifyNoInteractions(dispatcher);
        verifyNoInteractions(lockService);
    }

    @Test
    void cronJobInitializesNextFireTimeWithoutTriggering() {
        JobDefinition job = job(7L, "CRON", "0 0 * * * ?");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));

        engine.scheduleFixedRateJobs();
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(jobDefinitionService).updateNextFireTime(eq(7L), captor.capture());
        assertTrue(captor.getValue().isAfter(LocalDateTime.now()));
        verifyNoInteractions(dispatcher);
        verifyNoInteractions(lockService);
    }

    @Test
    void cronJobIsNotTriggeredBeforeNextFireTime() {
        JobDefinition job = job(8L, "CRON", "0 0 * * * ?");
        job.setNextFireTime(LocalDateTime.now().plusMinutes(1));
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));

        engine.scheduleFixedRateJobs();
        verifyNoInteractions(dispatcher);
        verifyNoInteractions(lockService);
    }

    @Test
    void cronJobIsTriggeredWhenDueAndNextFireTimeAdvanced() {
        JobDefinition job = job(9L, "CRON", "0 0 * * * ?");
        job.setNextFireTime(LocalDateTime.now().minusMinutes(1));
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));
        online();

        engine.scheduleFixedRateJobs();
        verify(lockService).tryAcquire(eq("job-scheduler:job-9"), any(Duration.class));
        verify(dispatcher, times(1)).execute(job);
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(jobDefinitionService).updateNextFireTime(eq(9L), captor.capture());
        assertTrue(captor.getValue().isAfter(LocalDateTime.now()));
    }

    @Test
    void cronJobSkippedWhenLockNotAcquired() {
        JobDefinition job = job(10L, "CRON", "0 0 * * * ?");
        job.setNextFireTime(LocalDateTime.now().minusMinutes(1));
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));
        when(lockService.tryAcquire(anyString(), any(Duration.class))).thenReturn(false);

        engine.scheduleFixedRateJobs();
        verifyNoInteractions(dispatcher);
        verify(jobDefinitionService, never()).updateNextFireTime(anyLong(), any());
    }

    @Test
    void invalidCronExpressionIsIgnored() {
        JobDefinition job = job(11L, "CRON", "not-a-cron");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));

        engine.scheduleFixedRateJobs();
        verifyNoInteractions(dispatcher);
        verifyNoInteractions(lockService);
    }

    @Test
    void reloadAllDoesNotAffectDbLock() {
        JobDefinition job = job(12L, "FIXED_RATE", "60000");
        when(jobDefinitionService.getEnabledJobsWithOnlineInstance()).thenReturn(List.of(job));
        online();

        engine.reloadAll();
        engine.scheduleFixedRateJobs();

        verify(dispatcher, times(1)).execute(job);
    }
}
