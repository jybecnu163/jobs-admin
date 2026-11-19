package com.example.jobadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.jobadmin.entity.ConfigChangeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 配置变更日志表 Mapper，提供 MyBatis-Plus 基础 CRUD 能力。
 */
@Mapper
public interface ConfigChangeLogMapper extends BaseMapper<ConfigChangeLog> {
}
