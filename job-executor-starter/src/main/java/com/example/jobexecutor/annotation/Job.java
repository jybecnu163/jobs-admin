package com.example.jobexecutor.annotation;

import java.lang.annotation.*;

/**
 * 标记在方法上，声明该方法是一个可被管理中心调度/手动触发的任务。
 *
 * 是业务代码与框架的唯一耦合点。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Job {
    String value();                    // 全局唯一名称
    String description() default "";
    boolean longRunning() default false;
    String system() default "";        // 所属系统，可在方法上覆盖（一般全局统一）
}
