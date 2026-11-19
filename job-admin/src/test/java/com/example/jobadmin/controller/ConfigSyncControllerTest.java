package com.example.jobadmin.controller;

import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.service.ConfigSyncService;
import com.example.jobadmin.service.JobDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 配置同步控制器单元测试：验证导入审批 Token 校验和导出行为。
 */
@ExtendWith(MockitoExtension.class)
class ConfigSyncControllerTest {

    @Mock
    private JobDefinitionService jobDefinitionService;
    @Mock
    private ConfigSyncService configSyncService;
    @InjectMocks
    private ConfigSyncController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "approvalToken", "secret-token");
    }

    @Test
    void importConfigWithInvalidTokenIsRejected() {
        List<JobDefinition> jobs = List.of(new JobDefinition());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> controller.importConfig(jobs, "wrong-token"));
        assertEquals("审批Token无效", ex.getMessage());
        verifyNoInteractions(configSyncService);
    }

    @Test
    void importConfigWithValidTokenIsApplied() {
        List<JobDefinition> jobs = List.of(new JobDefinition());
        controller.importConfig(jobs, "secret-token");
        verify(configSyncService).applyExportedPackage(jobs, "admin");
    }

    @Test
    void exportAllReturnsAllJobs() {
        List<JobDefinition> jobs = List.of(new JobDefinition());
        when(jobDefinitionService.listAll()).thenReturn(jobs);
        assertEquals(jobs, controller.exportAll());
    }
}
