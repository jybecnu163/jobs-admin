package com.example.jobadmin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.jobadmin.entity.ExecutorInstance;
import com.example.jobadmin.mapper.ExecutorInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行器实例管理：处理心跳注册，定时（30 秒超时）检测离线实例，并联动 Job 可达状态。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InstanceManager {
    private final ExecutorInstanceMapper instanceMapper;
    private final JobDefinitionService jobDefinitionService;

    // 接收心跳，标记在线
    public void heartbeat(String systemCode, String host, int port) {
        ExecutorInstance inst = instanceMapper.selectOne(
                new LambdaQueryWrapper<ExecutorInstance>()
                        .eq(ExecutorInstance::getSystemCode, systemCode)
                        .eq(ExecutorInstance::getHost, host)
                        .eq(ExecutorInstance::getPort, port));
        if (inst == null) {
            inst = new ExecutorInstance();
            inst.setSystemCode(systemCode);
            inst.setHost(host);
            inst.setPort(port);
        }
        inst.setLastHeartbeat(LocalDateTime.now());
        inst.setStatus("ONLINE");
        // todo
        insertOrUpdate(inst); // 使用 MyBatis-Plus 的 saveOrUpdate // insertOrUpdate
        // 如果原来该系统的 Job 是 UNREACHABLE，恢复为 ENABLED（保留之前是否禁用的状态，这里简化处理）
        jobDefinitionService.markOnlineBySystem(systemCode);
    }

    private void insertOrUpdate(ExecutorInstance inst) {
        if (null == inst.getId()) {
            instanceMapper.insert(inst);
        } else {
            instanceMapper.updateById(inst);
        }
    }

    /**
     * 每 15 秒检测一次：心跳超过 30 秒未更新的实例标记为 OFFLINE；
     * 当某系统所有实例均离线时，将该系统下任务标记为 UNREACHABLE。
     */
    @Scheduled(fixedRate = 15000)
    public void checkOffline() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
        List<ExecutorInstance> onlineList = instanceMapper.selectList(
                new LambdaQueryWrapper<ExecutorInstance>().eq(ExecutorInstance::getStatus, "ONLINE"));
        for (ExecutorInstance inst : onlineList) {
            if (inst.getLastHeartbeat() != null && inst.getLastHeartbeat().isBefore(threshold)) {
                inst.setStatus("OFFLINE");
                instanceMapper.updateById(inst);
                log.warn("实例离线: {}:{}", inst.getHost(), inst.getPort());
                // 如果该系统所有实例都离线，标记所有 Job 为 UNREACHABLE
                if (isAllInstanceOffline(inst.getSystemCode())) {
                    jobDefinitionService.markUnreachableBySystem(inst.getSystemCode());
                }
            }
        }
    }

    private boolean isAllInstanceOffline(String systemCode) {
        Long onlineCount = instanceMapper.selectCount(
                new LambdaQueryWrapper<ExecutorInstance>()
                        .eq(ExecutorInstance::getSystemCode, systemCode)
                        .eq(ExecutorInstance::getStatus, "ONLINE"));
        return onlineCount == 0;
    }

    // 选择一个在线实例（简单轮询可后续扩展）
    public ExecutorInstance pickOnlineInstance(String systemCode) {
        List<ExecutorInstance> instances = instanceMapper.selectList(
                new LambdaQueryWrapper<ExecutorInstance>()
                        .eq(ExecutorInstance::getSystemCode, systemCode)
                        .eq(ExecutorInstance::getStatus, "ONLINE"));
        if (instances.isEmpty()) return null;
        return instances.get(0); // 简单取第一个
    }

    // 所有在线实例，用于汇总各执行器的运行中任务
    public List<ExecutorInstance> listOnlineInstances() {
        return instanceMapper.selectList(
                new LambdaQueryWrapper<ExecutorInstance>()
                        .eq(ExecutorInstance::getStatus, "ONLINE"));
    }
}
