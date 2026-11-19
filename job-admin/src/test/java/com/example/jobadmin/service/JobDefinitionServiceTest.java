package com.example.jobadmin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.mapper.JobDefinitionMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 任务定义服务单元测试：覆盖注册去重、状态流转与参与调度的任务筛选条件。
 */
@ExtendWith(MockitoExtension.class)
class JobDefinitionServiceTest {

    @Mock
    private JobDefinitionMapper jobMapper;
    @InjectMocks
    private JobDefinitionService service;

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), JobDefinition.class);
    }

    private static JobDefinition job(Long id, String systemCode, String name) {
        JobDefinition job = new JobDefinition();
        job.setId(id);
        job.setSystemCode(systemCode);
        job.setJobName(name);
        return job;
    }

    @Test
    void saveOrUpdateWithIdUpdatesDirectly() {
        JobDefinition job = job(10L, "system-a", "job.a");
        service.saveOrUpdate(job);
        verify(jobMapper).updateById(job);
        verify(jobMapper, never()).insert(any());
    }

    @Test
    void saveOrUpdateWithoutIdInsertsNewJob() {
        JobDefinition job = job(null, "system-a", "job.new");
        when(jobMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.saveOrUpdate(job);
        verify(jobMapper).insert(job);
        verify(jobMapper, never()).updateById(any());
    }

    @Test
    void saveOrUpdateWithoutIdReusesExistingJobId() {
        JobDefinition job = job(null, "system-a", "job.dup");
        when(jobMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job(99L, "system-a", "job.dup"));

        service.saveOrUpdate(job);
        assertEquals(99L, job.getId());
        verify(jobMapper).updateById(job);
        verify(jobMapper, never()).insert(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void markUnreachableBySystemUpdatesAllJobsOfSystem() {
        service.markUnreachableBySystem("system-a");

        ArgumentCaptor<LambdaUpdateWrapper<JobDefinition>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(jobMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<JobDefinition> wrapper = captor.getValue();
        assertTrue(wrapper.getSqlSegment().contains("system_code"), wrapper.getSqlSegment());
        assertTrue(wrapper.getSqlSet().contains("status"), wrapper.getSqlSet());
    }

    @Test
    @SuppressWarnings("unchecked")
    void markOnlineBySystemOnlyRestoresUnreachableJobs() {
        service.markOnlineBySystem("system-a");

        ArgumentCaptor<LambdaUpdateWrapper<JobDefinition>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(jobMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<JobDefinition> wrapper = captor.getValue();
        assertTrue(wrapper.getSqlSegment().contains("UNREACHABLE") || wrapper.getSqlSegment().contains("status"), wrapper.getSqlSegment());
        assertTrue(wrapper.getSqlSet().contains("ENABLED") || wrapper.getSqlSet().contains("status"), wrapper.getSqlSet());
    }

    @Test
    void getEnabledJobsWithOnlineInstanceFiltersStatusAndScheduleType() {
        when(jobMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<JobDefinition> result = service.getEnabledJobsWithOnlineInstance();
        assertTrue(result.isEmpty());

        ArgumentCaptor<LambdaQueryWrapper<JobDefinition>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(jobMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("status"), sql);
        assertTrue(sql.contains("schedule_type"), sql);
    }
}
