# 修改日志

按 `doc/task-list.md` 的任务编号顺序记录。每项完成后记录改动文件、测试与回归结果，并执行 `git add`（不 push）。

---

## 基线

- 日期：2026-11-20
- 状态：基线测试通过（根工程 `mvn test -q` → `BASELINE_OK`）
- Git：`867fdf9 add test and comment`；工作区仅有未跟踪的 `doc/task-list.md`

---

## 任务 1 · 实现 CRON 调度

- 日期：2026-11-20
- 改动：
  - `job-admin/src/main/java/com/example/jobadmin/scheduler/JobSchedulerEngine.java`：FIXED_RATE 与 CRON 分流；使用 `CronExpression` 解析表达式；CRON 按 `nextFireTime` 判断是否到期，触发后从计划时间向后推进并持久化新的下次触发时间；下发前二次确认实例在线；非法表达式记录错误并跳过。
  - `job-admin/src/main/java/com/example/jobadmin/entity/JobDefinition.java`：新增 `nextFireTime` 字段。
  - `job-admin/src/main/java/com/example/jobadmin/service/JobDefinitionService.java`：新增 `updateNextFireTime(Long, LocalDateTime)`，按 id 更新下次触发时间。
  - `job-admin/src/main/resources/schema.sql`：`job_definition` 表新增 `next_fire_time` 列。
  - `job-admin/src/test/java/com/example/jobadmin/scheduler/JobSchedulerEngineTest.java`：新增 4 个 CRON 用例（初始化、未到期不触发、到期触发并推进、非法表达式）；重命名原 `nonFixedRateJobsAreIgnored` 为 `manualJobsAreIgnored`。
  - `job-admin/src/test/java/com/example/jobadmin/service/JobDefinitionServiceTest.java`：新增 `updateNextFireTimeSetsIdConditionAndValue`。
- 测试：根工程 `mvn test -q` → `TASK1_TEST_OK`（22 个用例，0 失败）。
- 回归：`job-demo/pom.xml mvn test -q` → `TASK1_DEMO_REGRESSION_OK`（2 个用例，0 失败）。
- 说明：现有数据库若已建表，需手动执行 `ALTER TABLE job_definition ADD COLUMN next_fire_time DATETIME COMMENT '下次触发时间';`。
- Git：已 `git add`（未 push）。

---

## 任务 2 · 分布式锁防重复触发

- 日期：2026-11-20
- 改动：
  - `job-admin/src/main/java/com/example/jobadmin/entity/SchedulerLock.java`、`mapper/SchedulerLockMapper.java`：新增调度锁实体与 Mapper。
  - `job-admin/src/main/java/com/example/jobadmin/service/SchedulerLockService.java`：基于数据库唯一键实现 `tryAcquire`：先续期已过期锁，冲突时插入新锁，插入唯一键冲突则获取失败；带 TTL 自动过期。
  - `job-admin/src/main/java/com/example/jobadmin/scheduler/JobSchedulerEngine.java`：删除内存 `lastExecution` Map，改为下发前调用 `SchedulerLockService` 获取锁；FIXED_RATE 锁 TTL 为触发间隔，CRON 锁 TTL 按下次触发间隔裁剪到 5–30 秒；`reloadAll()` 改为空操作。
  - `job-admin/src/main/resources/schema.sql`：新增 `job_scheduler_lock` 表（`lock_name` 唯一索引）。
  - `job-admin/src/test/java/com/example/jobadmin/scheduler/JobSchedulerEngineTest.java`：重写为覆盖锁获取/失败、FIXED_RATE、CRON、非法表达式等 12 个用例。
  - `job-admin/src/test/java/com/example/jobadmin/service/SchedulerLockServiceTest.java`：新增 3 个用例覆盖续期、插入、冲突。
- 测试：根工程 `mvn test -q` → `TASK2_TEST_OK`（27 个用例，0 失败）。
- 回归：`job-demo/pom.xml mvn test -q` → `TASK2_DEMO_REGRESSION_OK`。
- 说明：现有数据库需执行 `CREATE TABLE IF NOT EXISTS job_scheduler_lock (...)`（见 `schema.sql`）后多实例防护才生效。
- Git：已 `git add`（未 push）。

---

## 任务 3 · 实例选择策略扩展

- 日期：2026-11-20
- 改动：
  - `job-admin/src/main/java/com/example/jobadmin/service/InstanceSelectStrategy.java`：新增 `FIRST`、`ROUND_ROBIN`、`LEAST_LOAD` 三种策略枚举。
  - `job-admin/src/main/java/com/example/jobadmin/service/InstanceManager.java`：`pickOnlineInstance` 按配置策略选择实例；ROUND_ROBIN 按系统维护轮询计数器，LEAST_LOAD 按 `host:port` 维护累计分配计数；无在线实例仍返回 null。
  - `job-admin/src/main/resources/application.yml`：新增 `job.admin.instance-select-strategy: ROUND_ROBIN`。
  - `job-admin/src/test/java/com/example/jobadmin/service/InstanceManagerTest.java`：新增 4 个用例覆盖三种策略与空实例列表。
- 测试：根工程 `mvn test -q` → `TASK3_TEST_OK`（31 个用例，0 失败）。
- 回归：`job-demo/pom.xml mvn test -q` → `TASK3_DEMO_REGRESSION_OK`。
- 说明：可通过配置切换为 `FIRST` 恢复原有行为；LEAST_LOAD 的“负载”为进程内累计分配次数，重启后重新计数。
- Git：已 `git add`（未 push）。

---

## 任务 4 · 状态恢复保留禁用意图

- 日期：2026-11-20
- 改动：
  - `job-admin/src/main/java/com/example/jobadmin/entity/JobDefinition.java`、`job-admin/src/main/resources/schema.sql`：新增 `enabled` 字段/列，独立保存管理员启用意图。
  - `job-admin/src/main/java/com/example/jobadmin/service/JobDefinitionService.java`：`saveOrUpdate` 在执行器注册未带 `enabled` 时保留原值；`markOnlineBySystem` 按 `enabled` 用 `CASE WHEN` 将 UNREACHABLE 恢复为 ENABLED 或 DISABLED。
  - `job-admin/src/main/java/com/example/jobadmin/gitops/ScheduleFileLoader.java`：基线加载同时写入 `enabled`。
  - `job-admin/src/main/resources/static/index.html`：启用/禁用请求显式携带 `enabled: true/false`。
  - `job-admin/src/test/java/com/example/jobadmin/service/JobDefinitionServiceTest.java`：新增 3 个用例（注册保留禁用、显式 enabled、默认推导）并调整状态恢复断言。
  - `job-admin/src/test/java/com/example/jobadmin/gitops/ScheduleFileLoaderTest.java`：校验基线 `enabled` 映射。
- 测试：根工程 `mvn test -q` → `TASK4_TEST_OK`（34 个用例，0 失败）。
- 回归：`job-demo/pom.xml mvn test -q` → `TASK4_DEMO_REGRESSION_OK`。
- 说明：现有数据库需执行 `ALTER TABLE job_definition ADD COLUMN enabled TINYINT DEFAULT 1 COMMENT '管理员启用意图';` 并回填历史数据（可按 `status='DISABLED'` 置 0）。
- Git：已 `git add`（未 push）。

---

## 任务 5 · 修复配置与切面风险

- 日期：2026-11-20
- 改动：
  - `job-admin/src/main/resources/application.yml`：数据源库名由 `test` 改为 `job_admin`，与 `schema.sql` 一致。
  - `job-admin/src/main/java/com/example/jobadmin/aop/ControllerLogAspect.java`：切点排除条件由 `||` 改为 `&&`，心跳与 Job 列表查询真正被排除。
  - `job-admin/src/test/java/com/example/jobadmin/ApplicationConfigTest.java`：新增配置回归测试，校验数据源库名。
  - `job-admin/src/test/java/com/example/jobadmin/aop/ControllerLogAspectTest.java`：新增切点回归测试，校验使用 `&&` 且包含两个排除目标。
- 测试：根工程 `mvn test -q` → `TASK5_TEST_OK`（36 个用例，0 失败）。
- 回归：`job-demo/pom.xml mvn test -q` → `TASK5_DEMO_REGRESSION_OK`。
- 说明：`doc/architecture.md` 第 7 章“风险点 3、4”描述的问题在本任务中已修复，文档保留历史分析记录。
- Git：已 `git add`（未 push）。

---

## 任务 6 · 执行记录表与结果回调

- 日期：2026-11-20
- 改动：
  - Admin：`JobExecutionLog` 实体、`JobExecutionLogMapper`、`ExecutionResultRequest` DTO、`JobExecutionLogService`；`CallbackController` 新增 `POST /api/callback/execution-result`；`schema.sql` 新增 `job_execution_log` 表。
  - 执行器：`ExecutionResultReporter`（支持 `job.executor.result-callback-enabled` 开关，默认开启）；`JobExecutionController` 在 `FutureTask.done()` 中按 SUCCESS/FAILED/STOPPED 上报，附带起止时间、耗时和异常信息；`JobExecutorAutoConfiguration` 增加 Reporter Bean；`ExecutorProperties` 增加开关。
  - 测试：`ExecutionResultReporterTest`（回调 URL 与字段）、`JobExecutionControllerTest` 增加 3 个上报状态用例、`JobExecutionLogServiceTest`（解析与落库）。
- 测试：根工程 `mvn test -q` → `TASK6_TEST_OK`（45 个用例，0 失败）。
- 回归：`job-demo/pom.xml mvn test -q` → `TASK6_DEMO_REGRESSION_OK`。
- 说明：现有数据库需执行 `schema.sql` 中 `job_execution_log` 建表语句；如需关闭上报可配置 `job.executor.result-callback-enabled: false`。
- Git：已 `git add`（未 push）。

---

## 任务 7 · 日志在线查看

- 日期：2026-11-20
- 改动：
  - 执行器：`JobLogCollector`（按 jobName 保存最近 200 行，ThreadLocal 区分并发任务）、`RoutingPrintStream`（代理 System.out）；`JobExecutionController` 在执行期绑定/清理任务名并新增 `GET /executor/logs`；`ExecutionProperties` 增加 `log-capture-enabled`；自动配置增加 Collector Bean。
  - Admin：`JobExecutionDispatcher.queryLogs`；`ScheduleController` 新增 `GET /api/schedule/logs`（参数 jobName、limit）。
  - 前端：`index.html` 增加“日志”按钮、日志对话框与 `openLogDialog()`，调用 `/api/schedule/logs`。
  - 测试：`JobLogCollectorTest`、`JobExecutionControllerTest.logsEndpointReturnsCollectorResult`、`ScheduleControllerTest` 2 个用例。
- 测试：根工程 `mvn test -q` → `TASK7_TEST_OK`（51 个用例，0 失败）。
- 回归：`job-demo/pom.xml mvn test -q` → `TASK7_DEMO_REGRESSION_OK`。
- 说明：日志采集通过替换 `System.out` 实现，可通过 `job.executor.log-capture-enabled: false` 关闭；日志仅保存在执行器进程内存中，重启后丢失。
- Git：已 `git add`（未 push）。
