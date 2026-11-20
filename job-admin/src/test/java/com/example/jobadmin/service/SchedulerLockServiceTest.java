package com.example.jobadmin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.jobadmin.entity.SchedulerLock;
import com.example.jobadmin.mapper.SchedulerLockMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 分布式锁服务单元测试：覆盖续期成功、插入成功和插入冲突三条路径。
 */
@ExtendWith(MockitoExtension.class)
class SchedulerLockServiceTest {

    @Mock
    private SchedulerLockMapper lockMapper;
    @InjectMocks
    private SchedulerLockService service;

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SchedulerLock.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void tryAcquireReturnsTrueWhenExpiredLockRenewed() {
        when(lockMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        assertTrue(service.tryAcquire("job-scheduler:job-a", Duration.ofSeconds(10)));
        verify(lockMapper, never()).insert(any());
        org.mockito.ArgumentCaptor<LambdaUpdateWrapper<SchedulerLock>> captor =
                org.mockito.ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(lockMapper).update(isNull(), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("lock_name"), captor.getValue().getSqlSegment());
        assertTrue(captor.getValue().getSqlSegment().contains("locked_until"), captor.getValue().getSqlSegment());
    }

    @Test
    @SuppressWarnings("unchecked")
    void tryAcquireInsertsWhenNoLockExists() {
        when(lockMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        assertTrue(service.tryAcquire("job-scheduler:job-b", Duration.ofSeconds(10)));
        org.mockito.ArgumentCaptor<SchedulerLock> captor = org.mockito.ArgumentCaptor.forClass(SchedulerLock.class);
        verify(lockMapper).insert(captor.capture());
        assertEquals("job-scheduler:job-b", captor.getValue().getLockName());
        assertNotNull(captor.getValue().getLockedUntil());
    }

    @Test
    @SuppressWarnings("unchecked")
    void tryAcquireReturnsFalseWhenAnotherInstanceHoldsLock() {
        when(lockMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);
        when(lockMapper.insert(any(SchedulerLock.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertFalse(service.tryAcquire("job-scheduler:job-c", Duration.ofSeconds(10)));
    }
}
