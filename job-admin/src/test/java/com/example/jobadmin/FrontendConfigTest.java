package com.example.jobadmin;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 前端静态页回归测试：校验相对 API 地址、禁用按钮与配置管理入口。
 */
class FrontendConfigTest {

    @Test
    void frontendUsesRelativeApiAndHasExpectedControls() throws Exception {
        String html;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("static/index.html")) {
            assertNotNull(in, "static/index.html 应存在于类路径");
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(html.contains("const API_BASE = '/api'"), "API 地址应为相对路径");
        assertFalse(html.contains("http://localhost:8080/api"), "不应硬编码后端地址");
        assertTrue(html.contains(":disabled=\"job.status==='UNREACHABLE'\""), "不可达任务应禁用执行按钮");
        assertTrue(html.contains("/config/export"), "应提供配置导出调用");
        assertTrue(html.contains("/config/import"), "应提供配置导入调用");
    }

    @Test
    void independentFrontendScaffoldExists() {
        assertTrue(Files.exists(Path.of("../web/package.json")), "应存在独立前端工程");
        assertTrue(Files.exists(Path.of("../web/vite.config.js")), "应存在 Vite 配置");
    }
}
