package com.example.jobadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.jobadmin.entity.JobExecutionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务执行记录表 Mapper。
 */
@Mapper
public interface JobExecutionLogMapper extends BaseMapper<JobExecutionLog> {
}
