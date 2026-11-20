package com.example.jobadmin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Admin 接口鉴权过滤器：配置 token 后要求 /api/** 请求携带 X-API-Token。
 */
@Component
public class ApiAuthFilter extends OncePerRequestFilter {
    @Value("${job.admin.api-token:}")
    private String apiToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (apiToken != null && !apiToken.isBlank()
                && request.getRequestURI().startsWith("/api/")
                && !apiToken.equals(request.getHeader("X-API-Token"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
