package com.example.jobadmin.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理：统一返回 {"code": 数字, "message": 文本} 结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 参数校验/业务规则错误统一映射为 HTTP 400，错误体 code=400。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(error(400, e.getMessage()));
    }

    /**
     * 未分类运行时异常兜底为 HTTP 500，错误体 code=500，并把异常信息回传给前端排障。
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException e) {
        return ResponseEntity.internalServerError().body(error(500, e.getMessage()));
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        return body;
    }
}
