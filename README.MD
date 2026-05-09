# Job 管理中心

分布式任务调度管理系统，支持 Job 自动发现、集中调度、上下线感知、环境隔离与 GitOps 配置发布。

## 技术栈

层级 技术
后端 Spring Boot 3.2、MyBatis-Plus 3.5、MySQL
前端 Vue 3 + Element Plus（CDN 单页）
客户端集成 Spring Boot Starter（注解驱动）
构建工具 Maven 多模块

## 项目结构

```text
job-manager/
├── pom.xml                           # 父 POM
├── schema.sql                        # 数据库建表语句
├── job-admin/                        # 管理端服务
│   ├── src/main/java/com/example/jobadmin/
│   │   ├── entity/                   # 实体类（JobDefinition、ExecutorInstance、ConfigChangeLog）
│   │   ├── mapper/                   # MyBatis-Plus Mapper 接口
│   │   ├── service/                  # 核心服务（实例管理、任务分发、配置同步）
│   │   ├── scheduler/                # 调度引擎（定时扫描与触发）
│   │   ├── gitops/                   # GitOps 基线加载
│   │   └── controller/               # REST API（任务CRUD、调度控制、心跳、配置导入导出）
│   └── src/main/resources/
│       ├── application.yml
│       └── job-schedules-baseline.yml # 基线配置样例
└── job-executor-starter/             # 业务系统接入包
└── src/main/java/com/example/jobexecutor/
├── annotation/Job.java       # @Job 注解定义
├── ExecutorProperties.java   # 配置绑定
├── JobExecutorAutoConfiguration.java # 自动装配
├── JobAnnotationScanner.java # 方法扫描与注册
├── HeartbeatSender.java      # 心跳上报
└── JobExecutionController.java # 接收执行/停止指令
```

## 核心特性

零侵入接入：业务系统只需引入 job-executor-starter，在方法上加 @Job，即可自动注册至管理端。

自动上下线感知：心跳维持，实例离线后自动将任务标记为「不可调度」，恢复后自动还原。

双模调度：支持 CRON 与固定频率，长任务可手动触发与停止。

环境隔离 + 审计：每环境独立部署 Admin，配合 GitOps 基线加载与带审批的配置导入导出，满足测试→生产发布要求。

轻量级核心：内置内存调度器和 HTTP 回调，可快速运行；预留 Quartz 集成接口。

## 快速开始

0、前置条件
JDK 17+
MySQL 8.0+
Maven 3.9+
浏览器（Chrome/Edge）

1. 克隆并构建项目
   ```bash
   git clone <your-repo-url>
   cd job-manager
   mvn clean install -DskipTests
   ```
2. 初始化数据库
   执行根目录下的 schema.sql（或在 Admin 中配置自动建表）：

    ```bash
    mysql -u root -p < schema.sql
    ```

3. 配置并启动管理端
   编辑 job-admin/src/main/resources/application.yml：

    ```yaml
    spring:
      datasource:
        url: jdbc:mysql://localhost:3306/job_admin?...
        username: root
        password: your-pass
    ```

启动：

```bash
cd job-admin
mvn spring-boot:run
```

管理端 API 启动在 http://localhost:8080

4. 业务系统接入示例
   在你的业务系统 pom.xml 中添加依赖：

```xml

<dependency>
    <groupId>com.example</groupId>
    <artifactId>job-executor-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

配置 application.yml：

```yaml
job:
  executor:
    admin-url: http://localhost:8080   # 指向管理端地址
    system-code: my-service            # 当前系统唯一标识
```

在需要管理的任务方法上添加 @Job：

```java

@Component
public class OrderCleanTask {
    @Job(value = "order.clean", description = "清理过期订单")
    public void clean() {
        // 业务逻辑
    }
}
```

启动业务系统，任务将自动出现在管理端页面。

5. 打开前端管理页面
   使用已生成的 job-admin-ui.html（位于项目根目录或 static 下），浏览器直接打开即可。
   确保页面中 API_BASE 指向正确的 Admin 地址。

## 环境隔离与配置发布

开发、测试、生产环境各自部署一套 Admin + 数据库，物理隔离。

每环境可配置独立的 job-schedules-baseline.yml，启动时自动加载为「基线锁定」任务（页面不可直接修改）。

测试验证通过的配置变更，通过 Admin 的 配置导入/导出 API 经审批后同步至生产环境（见 POST /api/config/export 与 POST
/api/config/import）。

生产环境的 import 接口需提供审批 Token（配置项 job.admin.import.approval-token）。

## 前端操作说明

功能 说明
任务列表 显示所有 Job，支持按系统、状态过滤
状态颜色 绿（已启用）、灰（已禁用）、红（不可调度）
启用/禁用 切换调度开关，立即生效
执行 手动触发一次（需目标系统在线）
停止 中断正在运行的长任务
调度配置 修改 CRON 或固定延迟，保存后即时更新

## REST API 概览

接口 方法 描述
/api/jobs GET 获取全部 Job 定义
/api/jobs/{id} PUT 更新 Job 配置
/api/schedule/execute/{id} POST 手动触发执行
/api/schedule/stop/{id} POST 停止长任务
/api/callback/heartbeat POST Executor 心跳上报
/api/callback/register-job POST Executor 注册 Job
/api/config/export GET 导出全部配置（可用于同步）
/api/config/import POST 导入配置（需审批 Token）

## 关键配置项

配置 说明
job.executor.admin-url 管理端地址（业务侧）
job.executor.system-code 业务系统唯一标识（业务侧）
job.admin.baseline-file GitOps 基线文件位置（管理端）
job.admin.import.approval-token 配置导入审批令牌（管理端）

## 扩展建议

调度器升级：替换 JobSchedulerEngine 为 Quartz 集群模式。

注册中心：用 Nacos/Eureka 替代心跳直连，实现更精准的上下线。

执行日志：增加执行历史表，由 Executor 回调执行结果。

告警：集成钉钉/飞书机器人，任务失败或实例离线时实时通知。

前端工程化：将当前单页重构为 Vue 项目，支持多环境切换、登录鉴权。

## 页面展示

![示例图片](system.jpg "效果图")