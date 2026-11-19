package com.example.jobadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.jobadmin.entity.ExecutorInstance;
import org.apache.ibatis.annotations.Mapper;


/**
 * 执行器实例表 Mapper，提供 MyBatis-Plus 基础 CRUD 能力。
 */
@Mapper
public interface ExecutorInstanceMapper extends BaseMapper<ExecutorInstance> {
}
