package com.example.jobadmin.aop;

import com.example.jobadmin.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 写操作审计切面：Controller 的 POST/PUT/DELETE 成功执行后记录操作日志。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {
    private final OperationLogService operationLogService;

    /**
     * 仅记录 POST/PUT/DELETE 且目标方法成功返回后的写操作；CallbackController 全部排除，失败请求不记审计。
     */
    @Around("execution(public * com.example.jobadmin.controller..*.*(..)) " +
            "&& !execution(public * com.example.jobadmin.controller.CallbackController.*(..))")
    public Object record(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())
                    || "DELETE".equals(request.getMethod())) {
                operationLogService.record(
                        joinPoint.getSignature().toShortString(),
                        request.getRequestURI(),
                        Arrays.toString(joinPoint.getArgs()));
            }
        }
        return result;
    }
}
