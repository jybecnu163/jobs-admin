package com.example.jobadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 配置变更审计日志实体，记录每次配置导入的操作人、审批状态和变更内容。
 * approved：0 待审批，1 已批准，2 已拒绝。
 */
@Data
@TableName("config_change_log")
public class ConfigChangeLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String jobName;
    private String changeType;
    private String oldValue;
    private String newValue;
    private String operator;
    private Integer approved;    // 0待审批,1已批准,2已拒绝
    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
