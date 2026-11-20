package com.example.jobadmin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.jobadmin.entity.ExecutorInstance;
import com.example.jobadmin.mapper.ExecutorInstanceMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 实例管理单元测试：覆盖三种实例选择策略与无在线实例的边界。
 */
@ExtendWith(MockitoExtension.class)
class InstanceManagerTest {

    @Mock
    private ExecutorInstanceMapper instanceMapper;
    @Mock
    private JobDefinitionService jobDefinitionService;
    @Mock
    private AlertService alertService;
    @InjectMocks
    private InstanceManager manager;

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExecutorInstance.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(manager, "strategy", InstanceSelectStrategy.FIRST);
    }

    private static ExecutorInstance instance(String host, int port) {
        ExecutorInstance inst = new ExecutorInstance();
        inst.setHost(host);
        inst.setPort(port);
        inst.setStatus("ONLINE");
        return inst;
    }

    private void mockOnlineInstances(List<ExecutorInstance> instances) {
        when(instanceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(instances);
    }

    @Test
    void firstStrategyPicksFirstInstance() {
        ExecutorInstance first = instance("10.0.0.1", 8082);
        mockOnlineInstances(List.of(first, instance("10.0.0.2", 8082)));

        assertSame(first, manager.pickOnlineInstance("job-demo"));
    }

    @Test
    void roundRobinCyclesInstancesPerSystem() {
        ReflectionTestUtils.setField(manager, "strategy", InstanceSelectStrategy.ROUND_ROBIN);
        ExecutorInstance first = instance("10.0.0.1", 8082);
        ExecutorInstance second = instance("10.0.0.2", 8082);
        mockOnlineInstances(List.of(first, second));

        assertSame(first, manager.pickOnlineInstance("job-demo"));
        assertSame(second, manager.pickOnlineInstance("job-demo"));
        assertSame(first, manager.pickOnlineInstance("job-demo"));
    }

    @Test
    void leastLoadPicksLeastAssignedInstance() {
        ReflectionTestUtils.setField(manager, "strategy", InstanceSelectStrategy.LEAST_LOAD);
        ExecutorInstance first = instance("10.0.0.1", 8082);
        ExecutorInstance second = instance("10.0.0.2", 8082);
        mockOnlineInstances(List.of(first, second));

        assertSame(first, manager.pickOnlineInstance("job-demo"));
        assertSame(second, manager.pickOnlineInstance("job-demo"));
        assertSame(first, manager.pickOnlineInstance("job-demo"));
    }

    @Test
    void checkOfflineUsesConfiguredThreshold() {
        ReflectionTestUtils.setField(manager, "offlineThresholdSeconds", 1);
        ExecutorInstance stale = instance("10.0.0.1", 8082);
        stale.setId(1L);
        stale.setSystemCode("job-demo");
        stale.setLastHeartbeat(LocalDateTime.now().minusSeconds(5));
        when(instanceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(stale));
        when(instanceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        manager.checkOffline();

        assertEquals("OFFLINE", stale.getStatus());
        verify(jobDefinitionService).markUnreachableBySystem("job-demo");
        verify(alertService).send(eq("执行器实例离线"), anyString());
    }

    @Test
    void returnsNullWhenNoOnlineInstance() {
        mockOnlineInstances(List.of());

        assertNull(manager.pickOnlineInstance("job-demo"));
    }
}
