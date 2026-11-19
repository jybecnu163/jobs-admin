package com.example.jobadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 执行器实例实体，记录系统编码、地址、在线状态和最近心跳时间，用于 Admin 选择下发目标。
 * status：ONLINE 在线，OFFLINE 离线。
 */
@Data
@TableName("executor_instance")
public class ExecutorInstance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String systemCode;
    private String host;
    private Integer port;
    private String status;       // ONLINE, OFFLINE
    private LocalDateTime lastHeartbeat;
    private LocalDateTime createdAt;
}
