package com.example.jobexecutor;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 执行器鉴权过滤器测试。
 */
class ExecutorAuthFilterTest {

    private ExecutorProperties properties(String token) {
        ExecutorProperties properties = new ExecutorProperties();
        properties.setApiToken(token);
        return properties;
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        ExecutorAuthFilter filter = new ExecutorAuthFilter(properties("secret"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/executor/execute");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void allowsConfiguredToken() throws Exception {
        ExecutorAuthFilter filter = new ExecutorAuthFilter(properties("secret"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/executor/execute");
        request.addHeader("X-API-Token", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
