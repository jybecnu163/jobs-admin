# 系统扩展任务清单

本清单基于 `doc/architecture.md` 与当前代码现状整理，按编号顺序执行即可。每项包含目标、证据路径和验收标准。

## 执行顺序总览

| 阶段 | 任务编号 | 主题 | 前置说明 |
|---|---|---|---|
| A · 正确性修复（P0） | 1–5 | 调度核心、状态恢复、配置与切面风险 | 无 |
| B · 执行闭环（P1） | 6–10 | 执行记录、日志、状态推送、告警、重试超时 | 9、10 依赖 6 |
| C · 安全与前端（P2） | 11–17 | 鉴权、异常处理、审计、前端修复与工程化 | 17 建议在 14、15 之后 |
| D · 工程化（P2） | 18–21 | 版本统一、容器化、CI、集成测试 | 可与 C 并行 |
| E · 规模化（P3） | 22–25 | 分片、DAG、多集群、心跳升级 | 按业务需要排期 |

---

## 阶段 A · 正确性修复（P0）

- [x] **1. 实现 CRON 调度**
  - 目标：在 `JobSchedulerEngine` 中解析 CRON 表达式并计算下次触发时间；任务定义新增 `next_fire_time` 字段。
  - 证据：`job-admin/src/main/java/com/example/jobadmin/scheduler/JobSchedulerEngine.java:53`（CRON 分支仍是注释）；`job-admin/src/main/java/com/example/jobadmin/entity/JobDefinition.java`。
  - 验收：页面配置 CRON 后按表达式触发；补充单测覆盖解析与到期判断；FIXED_RATE 行为不变。

- [x] **2. 分布式锁防重复触发**
  - 目标：将 `lastExecution` 内存 `ConcurrentHashMap` 替换为数据库行锁/乐观锁或 ShedLock，保证多 Admin 实例下单周期只触发一次。
  - 证据：`job-admin/src/main/java/com/example/jobadmin/scheduler/JobSchedulerEngine.java:30`。
  - 验收：启动两个 Admin 实例，同一 FIXED_RATE 任务单周期只下发一次。

- [x] **3. 实例选择策略扩展**
  - 目标：`InstanceManager.pickOnlineInstance()` 支持轮询、最小负载、按系统/分组路由，替代“简单取第一个”。
  - 证据：`job-admin/src/main/java/com/example/jobadmin/service/InstanceManager.java:88-90`。
  - 验收：多在线实例时请求按策略分布；补充策略单测。

- [x] **4. 状态恢复保留禁用意图**
  - 目标：新增 `enabled` 或 `preStatus` 字段；`markOnlineBySystem()` 只恢复可达性，不把手动禁用的任务改成 ENABLED。
  - 证据：`job-admin/src/main/java/com/example/jobadmin/service/JobDefinitionService.java:26-31`。
  - 验收：手动禁用的任务在实例离线再上线后仍为 DISABLED。

- [x] **5. 修复配置与切面风险**
  - 目标：统一数据源库名；将 AOP 切点中的 `||` 改为 `&&` 以真正排除心跳与列表查询，并修正与代码不一致的类注释。
  - 证据：`job-admin/src/main/resources/application.yml:6`；`job-admin/src/main/resources/schema.sql:2-3`；`job-admin/src/main/java/com/example/jobadmin/aop/ControllerLogAspect.java:8-10,22-24`。
  - 验收：应用连接的库中能查到三张表；心跳和列表查询不再打印 ACCESS/RESPONSE 日志。

---

## 阶段 B · 执行闭环（P1）

- [x] **6. 执行记录表与结果回调**
  - 目标：新增 `job_execution_log` 表；执行器执行结束后回调 Admin 写入状态、耗时、异常堆栈。
  - 证据：`job-executor-starter/src/main/java/com/example/jobexecutor/JobExecutionController.java:43-74`（异步提交后立即返回）。
  - 验收：每次执行都有记录；失败记录可查看异常堆栈。

- [x] **7. 日志在线查看**
  - 目标：执行器新增 `/executor/logs?jobName=...`，Admin 提供代理接口，前端增加日志弹窗。
  - 证据：`job-demo/src/main/java/org/example/jobdemo/worker/Demo.java:23-31`（当前使用 `System.out`）。
  - 验收：可选择任务查看最近 N 行执行日志。

- [x] **8. 实时状态推送替代轮询**
  - 目标：用 WebSocket 或 SSE 推送执行/停止/离线事件，替代前端 10 秒轮询。
  - 证据：`job-admin/src/main/resources/static/index.html:253-255`。
  - 验收：状态变化后 2 秒内页面更新，无需刷新。

- [x] **9. 失败告警**
  - 目标：任务失败、实例离线、调度跳过时通过邮件/钉钉/企业微信通知；告警规则随任务定义配置。
  - 证据：当前仅有配置导入审计 `job-admin/src/main/java/com/example/jobadmin/service/ConfigSyncService.java:22-37`。
  - 验收：模拟失败能收到通知，成功场景无误报。

- [x] **10. 失败重试与超时控制**
  - 目标：`@Job` 与任务定义增加 `maxRetry`、`retryInterval`、`timeoutMs`；超时自动下发 stop，重试走现有 execute 链路。
  - 证据：`job-executor-starter/src/main/java/com/example/jobexecutor/annotation/Job.java`。
  - 验收：失败任务按配置重试；超时任务在设定时间内停止。

---

## 阶段 C · 安全与前端（P2）

- [x] **11. 接口鉴权**
  - 目标：Admin `/api/**` 与执行器 `/executor/**` 增加鉴权；替换配置中的默认审批 Token。
  - 证据：`job-admin/src/main/resources/application.yml:25-27`；`job-admin/src/main/java/com/example/jobadmin/controller/ConfigSyncController.java:32-38`。
  - 验收：无凭证请求被拒绝；默认 Token 不再出现在配置中。

- [x] **12. 统一异常处理**
  - 目标：新增 `@RestControllerAdvice` 与统一错误码/响应结构；前端统一解析。
  - 证据：项目无 `@ControllerAdvice`；`ConfigSyncController.java:35-36` 直接抛异常；`ScheduleController.java:37-46` 返回字符串。
  - 验收：所有错误响应结构一致。

- [x] **13. 操作审计扩展**
  - 目标：手动执行、停止、启用、禁用均写入审计日志（操作人、时间、任务、参数）。
  - 证据：当前仅有配置导入日志 `job-admin/src/main/java/com/example/jobadmin/service/ConfigSyncService.java:22-37`。
  - 验收：每个写操作可追溯。

- [x] **14. 去掉硬编码 API 地址**
  - 目标：改用相对路径 `/api` + 反向代理，或构建/运行时注入配置。
  - 证据：`job-admin/src/main/resources/static/index.html:142`。
  - 验收：部署到非 localhost 环境无需修改 HTML。

- [x] **15. 不可达任务禁用执行按钮**
  - 目标：给活跃的执行按钮补上 `:disabled="job.status === 'UNREACHABLE'"`。
  - 证据：`job-admin/src/main/resources/static/index.html:76`（活跃模板无禁用），对比 `:104`（注释掉的旧模板有禁用）。
  - 验收：UNREACHABLE 行的“执行”按钮不可点击。

- [x] **16. 配置导入导出页面化**
  - 目标：新增配置管理页，调用 `/api/config/export`、`/api/config/import`，支持 Diff 预览和 Token 输入。
  - 证据：`job-admin/src/main/java/com/example/jobadmin/controller/ConfigSyncController.java:26-38`（后端已有、前端未调用）。
  - 验收：可在页面完成导出、预览与导入提交。

- [x] **17. 前端工程化**
  - 目标：独立 Vue/React 工程，支持多页面、权限菜单、环境变量和构建产物替换静态页。
  - 证据：`job-admin/src/main/resources/static/index.html:7-10,136-280`（CDN + 内联 JS 单页）。
  - 验收：开发/生产 API 地址可配置；构建产物可替换 `static/index.html`。

---

## 阶段 D · 工程化（P2）

- [x] **18. 统一依赖版本与构建**
  - 目标：统一 Spring Boot 版本；将 `job-demo` 纳入父工程 `<modules>`。
  - 证据：`pom.xml:24-27`；`job-demo/pom.xml`（Spring Boot `4.0.6` 与父工程 `3.2.5` 不一致）。
  - 验收：根目录一条 Maven 命令可构建并测试全部模块。

- [x] **19. 容器化与编排**
  - 目标：为 Admin、执行器、MySQL 提供 Dockerfile 与 docker-compose，消除 `localhost` 硬编码。
  - 证据：`job-admin/src/main/resources/application.yml:6`；`job-demo/src/main/resources/application.yaml:11`。
  - 验收：`docker compose up` 后全链路可用。

- [x] **20. CI/CD 与质量门禁**
  - 目标：流水线运行 `mvn test`，接入 JaCoCo 覆盖率阈值与静态检查。
  - 证据：现有 24 个单元测试（见 `job-admin/src/test`、`job-executor-starter/src/test`、`job-demo/src/test`）。
  - 验收：PR 必须通过流水线才能合并。

- [x] **21. 集成测试**
  - 目标：引入 H2 + `@SpringBootTest`，覆盖真实 Mapper/SQL 链路。
  - 证据：现有测试以 Mockito 单元测试为主（如 `job-admin/src/test/java/com/example/jobadmin/service/JobDefinitionServiceTest.java`）。
  - 验收：关键 SQL 与事务行为有集成测试覆盖。

---

## 阶段 E · 规模化（P3）

- [x] **22. 分片任务**
  - 目标：`@Job` 支持分片参数（shardIndex/shardTotal），多执行器并行处理大数据量。
  - 证据：`job-executor-starter/src/main/java/com/example/jobexecutor/annotation/Job.java`。
  - 验收：同一任务可配置多个分片并行执行。

- [x] **23. 工作流/DAG 编排**
  - 目标：支持任务依赖、成功/失败分支、超时策略，向轻量工作流引擎演进。
  - 证据：当前 `JobSchedulerEngine` 仅按任务独立触发（证据：`job-admin/src/main/java/com/example/jobadmin/scheduler/JobSchedulerEngine.java:29-51`）。
  - 验收：可配置并在页面展示任务依赖关系。

- [x] **24. 多集群与命名空间**
  - 目标：按系统/环境隔离任务与执行器，支持灰度执行和租户维度统计。
  - 证据：`JobDefinition.systemCode` 与 `ExecutorInstance.systemCode` 仅作简单匹配（证据：`job-admin/src/main/java/com/example/jobadmin/service/InstanceManager.java:84-91`）。
  - 验收：不同命名空间的任务不会被下发到错误执行器。

- [x] **25. 心跳机制升级**
  - 目标：将 10 秒 HTTP 轮询心跳改为长连接或 gRPC 流式心跳，降低延迟与连接开销。
  - 证据：`job-executor-starter/src/main/java/com/example/jobexecutor/HeartbeatSender.java:33-48`。
  - 验收：实例离线感知时间从 30 秒级缩短到秒级。

---

## 追加任务

- [x] **26. 执行历史记录与页面展示**
  - 目标：提供执行历史查询接口，并在管理页按任务展示执行结果、起止时间、耗时与异常信息。
  - 证据：`job-admin/src/main/java/com/example/jobadmin/service/JobExecutionLogService.java`；`job-admin/src/main/java/com/example/jobadmin/controller/ExecutionLogController.java`；`job-admin/src/main/resources/static/index.html`。
  - 验收：`GET /api/execution-logs` 可按 `jobName` 过滤并按开始时间倒序返回；页面可打开执行历史弹窗并展示记录；单测与前端回归测试通过。

- [x] **27. 执行历史增加执行机器 IP**
  - 目标：执行结果上报携带执行器 IP，落入 `job_execution_log.executor_host`，并在执行历史信息栏展示。
  - 证据：`job-executor-starter/src/main/java/com/example/jobexecutor/ExecutionResultReporter.java`；`job-admin/src/main/resources/schema.sql`；`job-admin/src/main/resources/static/index.html`。
  - 验收：执行历史每条记录显示“执行机器”列；上报、落库与前端回归测试通过。
