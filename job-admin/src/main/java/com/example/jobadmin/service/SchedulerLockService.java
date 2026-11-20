package com.example.jobadmin.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.jobadmin.entity.SchedulerLock;
import com.example.jobadmin.mapper.SchedulerLockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 基于数据库唯一键的分布式调度锁：先尝试续期已过期锁，失败则插入新锁；
 * 插入冲突说明其他 Admin 实例已持有未过期锁，本次获取失败。
 */
@Service
@RequiredArgsConstructor
public class SchedulerLockService {
    private final SchedulerLockMapper lockMapper;

    /**
     * 尝试获取锁，锁在 now + ttl 后自动过期，避免进程崩溃导致死锁。
     */
    public boolean tryAcquire(String lockName, Duration ttl) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockedUntil = now.plus(ttl);
        int updated = lockMapper.update(null, new LambdaUpdateWrapper<SchedulerLock>()
                .eq(SchedulerLock::getLockName, lockName)
                .lt(SchedulerLock::getLockedUntil, now)
                .set(SchedulerLock::getLockedUntil, lockedUntil));
        if (updated > 0) {
            return true;
        }
        try {
            SchedulerLock lock = new SchedulerLock();
            lock.setLockName(lockName);
            lock.setLockedUntil(lockedUntil);
            lockMapper.insert(lock);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
