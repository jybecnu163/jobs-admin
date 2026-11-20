package com.example.jobadmin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.jobadmin.entity.ExecutorInstance;
import com.example.jobadmin.mapper.ExecutorInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 执行器实例管理：处理心跳注册，定时（30 秒超时）检测离线实例，并联动 Job 可达状态。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InstanceManager {
    private final ExecutorInstanceMapper instanceMapper;
    private final JobDefinitionService jobDefinitionService;
    private final AlertService alertService;

    @Value("${job.admin.instance-select-strategy:FIRST}")
    private InstanceSelectStrategy strategy = InstanceSelectStrategy.FIRST;

    @Value("${job.admin.offline-threshold:30}")
    private int offlineThresholdSeconds = 30;

    private final ConcurrentHashMap<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> assignedCounts = new ConcurrentHashMap<>();

    // 接收心跳，标记在线
    public void heartbeat(String systemCode, String host, int port, String namespace) {
        ExecutorInstance inst = instanceMapper.selectOne(
                new LambdaQueryWrapper<ExecutorInstance>()
                        .eq(ExecutorInstance::getSystemCode, systemCode)
                        .eq(ExecutorInstance::getNamespace, namespace)
                        .eq(ExecutorInstance::getHost, host)
                        .eq(ExecutorInstance::getPort, port));
        if (inst == null) {
            inst = new ExecutorInstance();
            inst.setSystemCode(systemCode);
            inst.setNamespace(namespace);
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
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(offlineThresholdSeconds);
        List<ExecutorInstance> onlineList = instanceMapper.selectList(
                new LambdaQueryWrapper<ExecutorInstance>().eq(ExecutorInstance::getStatus, "ONLINE"));
        for (ExecutorInstance inst : onlineList) {
            if (inst.getLastHeartbeat() != null && inst.getLastHeartbeat().isBefore(threshold)) {
                inst.setStatus("OFFLINE");
                instanceMapper.updateById(inst);
                log.warn("实例离线: {}:{}", inst.getHost(), inst.getPort());
                alertService.send("执行器实例离线", inst.getHost() + ":" + inst.getPort());
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

    /**
     * 根据配置的策略选择一个在线实例：FIRST 取第一个，ROUND_ROBIN 按系统轮询，LEAST_LOAD 取累计分配最少者。
     */
    public ExecutorInstance pickOnlineInstance(String systemCode) {
        return pickOnlineInstance(systemCode, null);
    }

    /**
     * 按系统编码与命名空间选择在线实例；namespace 为空时不限制命名空间。
     */
    public ExecutorInstance pickOnlineInstance(String systemCode, String namespace) {
        String counterKey = systemCode + ":" + (namespace == null ? "" : namespace);
        List<ExecutorInstance> instances = queryOnlineInstances(systemCode, namespace);
        if (instances.isEmpty()) return null;
        switch (strategy) {
            case ROUND_ROBIN:
                AtomicInteger counter = roundRobinCounters.computeIfAbsent(counterKey, k -> new AtomicInteger());
                int index = Math.floorMod(counter.getAndIncrement(), instances.size());
                return instances.get(index);
            case LEAST_LOAD:
                ExecutorInstance chosen = instances.get(0);
                long min = Long.MAX_VALUE;
                for (ExecutorInstance inst : instances) {
                    int count = assignedCounts.computeIfAbsent(instanceKey(inst), k -> new AtomicInteger()).get();
                    if (count < min) {
                        min = count;
                        chosen = inst;
                    }
                }
                assignedCounts.computeIfAbsent(instanceKey(chosen), k -> new AtomicInteger()).incrementAndGet();
                return chosen;
            case FIRST:
            default:
                return instances.get(0);
        }
    }

    private List<ExecutorInstance> queryOnlineInstances(String systemCode, String namespace) {
        return instanceMapper.selectList(
                new LambdaQueryWrapper<ExecutorInstance>()
                        .eq(ExecutorInstance::getSystemCode, systemCode)
                        .eq(namespace != null && !namespace.isBlank(),
                                ExecutorInstance::getNamespace, namespace)
                        .eq(ExecutorInstance::getStatus, "ONLINE"));
    }

    private String instanceKey(ExecutorInstance inst) {
        return inst.getHost() + ":" + inst.getPort();
    }

    // 所有在线实例，用于汇总各执行器的运行中任务
    public List<ExecutorInstance> listOnlineInstances() {
        return instanceMapper.selectList(
                new LambdaQueryWrapper<ExecutorInstance>()
                        .eq(ExecutorInstance::getStatus, "ONLINE"));
    }
}
