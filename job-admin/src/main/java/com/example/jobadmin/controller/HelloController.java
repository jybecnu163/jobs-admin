package com.example.jobadmin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查示例接口，用于快速验证 Admin 服务是否启动。
 */
@RestController
@RequestMapping("/api/hello")
@RequiredArgsConstructor
public class HelloController {

    // http://localhost:8080/api/hello
    @GetMapping()
    public String sayHello() {
        return "say hello from job admin";
    }
}
