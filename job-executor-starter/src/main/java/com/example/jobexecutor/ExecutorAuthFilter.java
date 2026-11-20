package com.example.jobexecutor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 执行器接口鉴权过滤器：配置 token 后要求 /executor/** 请求携带 X-API-Token。
 */
public class ExecutorAuthFilter extends OncePerRequestFilter {
    private final ExecutorProperties properties;

    public ExecutorAuthFilter(ExecutorProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = properties.getApiToken();
        if (token != null && !token.isBlank()
                && request.getRequestURI().startsWith("/executor/")
                && !token.equals(request.getHeader("X-API-Token"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
