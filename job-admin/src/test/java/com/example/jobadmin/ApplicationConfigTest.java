package com.example.jobadmin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 配置回归测试：确保数据源库名与 schema.sql 建库名一致。
 */
class ApplicationConfigTest {

    @Test
    void datasourceDatabaseMatchesSchema() throws IOException {
        String yaml;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            assertNotNull(in, "application.yml 应存在于测试类路径");
            yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(yaml.contains("jdbc:mysql://localhost:3306/job_admin"), yaml);
    }
}
