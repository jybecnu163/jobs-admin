package com.example.jobadmin.gitops;

import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.JobDefinitionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 基线配置加载器单元测试：验证 YAML 字段到 JobDefinition 的映射及基线锁定标记。
 */
@ExtendWith(MockitoExtension.class)
class ScheduleFileLoaderTest {

    private static final String YAML = """
            jobs:
              - name: test.demo.longJob
                system: job-demo
                scheduleType: FIXED_RATE
                scheduleExpr: '60000'
                longRunning: true
                description: long running demo
                enabled: true
              - name: test.demo.disabled
                system: job-demo
                scheduleType: NONE
                scheduleExpr: ''
                longRunning: false
                description: disabled demo
                enabled: false
            """;

    @Mock
    private JobDefinitionService jobDefinitionService;
    @InjectMocks
    private ScheduleFileLoader loader;

    @Test
    void loadBaselineMapsYamlFieldsAndLocksJobs() {
        ReflectionTestUtils.setField(loader, "baselineFile",
                new ByteArrayResource(YAML.getBytes(StandardCharsets.UTF_8)));

        loader.loadBaseline();

        ArgumentCaptor<JobDefinition> captor = ArgumentCaptor.forClass(JobDefinition.class);
        verify(jobDefinitionService, times(2)).saveOrUpdate(captor.capture());
        List<JobDefinition> jobs = captor.getAllValues();

        JobDefinition enabled = jobs.get(0);
        assertEquals("test.demo.longJob", enabled.getJobName());
        assertEquals("job-demo", enabled.getSystemCode());
        assertEquals("FIXED_RATE", enabled.getScheduleType());
        assertEquals("60000", enabled.getScheduleExpr());
        assertEquals("long running demo", enabled.getDescription());
        assertTrue(enabled.getLongRunning());
        assertEquals("ENABLED", enabled.getStatus());
        assertTrue(enabled.getEnabled());
        assertTrue(enabled.getIsBaselineLocked());

        JobDefinition disabled = jobs.get(1);
        assertEquals("DISABLED", disabled.getStatus());
        assertFalse(disabled.getEnabled());
        assertFalse(disabled.getLongRunning());
        assertTrue(disabled.getIsBaselineLocked());
    }

    @Test
    void missingBaselineFileIsIgnored() {
        ReflectionTestUtils.setField(loader, "baselineFile", new ClassPathResource("not-exists-baseline.yml"));

        loader.loadBaseline();
        verifyNoInteractions(jobDefinitionService);
    }
}
