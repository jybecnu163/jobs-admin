package com.example.jobadmin.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全局异常处理测试。
 */
class GlobalExceptionHandlerTest {

    @Test
    void runtimeExceptionReturnsUnifiedBody() {
        ResponseEntity<Map<String, Object>> response =
                new GlobalExceptionHandler().handleRuntime(new RuntimeException("boom"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals(500, response.getBody().get("code"));
        assertEquals("boom", response.getBody().get("message"));
    }

    @Test
    void illegalArgumentReturnsBadRequest() {
        ResponseEntity<Map<String, Object>> response =
                new GlobalExceptionHandler().handleIllegalArgument(new IllegalArgumentException("bad"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("bad", response.getBody().get("message"));
    }
}
