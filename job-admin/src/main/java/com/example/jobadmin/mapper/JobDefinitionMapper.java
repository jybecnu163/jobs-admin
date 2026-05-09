package com.example.jobadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.jobadmin.entity.JobDefinition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobDefinitionMapper extends BaseMapper<JobDefinition> {
}
