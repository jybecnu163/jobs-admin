package com.example.jobadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调度分布式锁实体，lock_name 唯一，locked_until 表示锁的过期时间。
 */
@Data
@TableName("job_scheduler_lock")
public class SchedulerLock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String lockName;
    private LocalDateTime lockedUntil;
}
