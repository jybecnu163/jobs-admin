package com.example.jobadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.jobadmin.entity.JobDefinition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务定义表 Mapper，提供 MyBatis-Plus 基础 CRUD 能力。
 */
@Mapper
public interface JobDefinitionMapper extends BaseMapper<JobDefinition> {
}
