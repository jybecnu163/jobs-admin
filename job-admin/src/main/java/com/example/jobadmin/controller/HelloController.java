package com.example.jobadmin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
