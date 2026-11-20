package com.example.jobadmin.integration;

import com.example.jobadmin.entity.JobDefinition;
import com.example.jobadmin.mapper.JobDefinitionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MyBatis-Plus 集成测试：使用 H2 覆盖真实 Mapper/SQL 链路。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MybatisPlusIntegrationTest {

    @Autowired
    private JobDefinitionMapper mapper;

    @Test
    void insertAndSelectJobDefinition() {
        JobDefinition job = new JobDefinition();
        job.setJobName("integration.job");
        job.setSystemCode("job-demo");
        job.setStatus("ENABLED");
        job.setEnabled(true);

        mapper.insert(job);

        JobDefinition loaded = mapper.selectById(job.getId());
        assertNotNull(loaded);
        assertEquals("integration.job", loaded.getJobName());
        assertEquals("job-demo", loaded.getSystemCode());
    }
}
