package com.example.jobadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.jobadmin.entity.SchedulerLock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 调度锁表 Mapper，提供 MyBatis-Plus 基础 CRUD 能力。
 */
@Mapper
public interface SchedulerLockMapper extends BaseMapper<SchedulerLock> {
}
