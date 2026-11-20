package com.example.jobadmin.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 日志切面回归测试：确保高频接口排除条件使用 AND，避免再次退化为恒真表达式。
 */
class ControllerLogAspectTest {

    @Test
    void pointcutExcludesHeartbeatAndListAllWithAnd() throws Exception {
        Around around = ControllerLogAspect.class
                .getMethod("logInputOutput", ProceedingJoinPoint.class)
                .getAnnotation(Around.class);

        assertNotNull(around);
        String pointcut = around.value();
        assertTrue(pointcut.contains("CallbackController.heartbeat"), pointcut);
        assertTrue(pointcut.contains("JobDefinitionController.listAll"), pointcut);
        assertTrue(pointcut.contains("&&"), pointcut);
        assertFalse(pointcut.contains("||"), pointcut);
    }
}
