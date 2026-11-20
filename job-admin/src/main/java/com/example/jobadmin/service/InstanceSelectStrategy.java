package com.example.jobadmin.service;

/**
 * 执行器实例选择策略。
 */
public enum InstanceSelectStrategy {
    /** 取第一个在线实例，保持原有行为。 */
    FIRST,
    /** 按系统维度轮询在线实例。 */
    ROUND_ROBIN,
    /** 选择累计分配次数最少的在线实例。 */
    LEAST_LOAD
}
