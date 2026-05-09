整体协作流程
业务系统启动 → JobExecutorAutoConfiguration 激活。

JobAnnotationScanner 扫描所有 @Job 方法，建立本地映射，并向 Admin 注册。

HeartbeatSender 开始定时心跳，维持在线状态。

Admin 调度或手动触发 → Admin 调用业务系统的 /executor/execute 或 /executor/stop。

JobExecutionController 根据 jobName 找到对应方法和 Bean，执行或中断任务。

简而言之，这个 Starter 把业务系统变成了一个受管理中心远程调度的执行器，而业务代码只需关心任务逻辑本身，其余全部由 Starter
透明完成。

mvn install:install-file -Dfile=.\target\job-executor-starter-1.0.0.jar -DgroupId=com.example -DartifactId=job-executor-starter -Dversion=1.0.0 -Dpackaging=jar


创建 Maven 配置 在 IDEA 的导航栏中，点击 Run/Debug Configurations。 
点击左上角的加号，选择 Maven，并设置以下参数： Working directory：项目根目录（包含 pom.xml 的路径）。 
Command line：输入以下命令： install:install-file -Dfile=路径/到/你的.jar -DgroupId=自定义.groupId -DartifactId=自定义.artifactId -Dversion=版本号 -Dpackaging=jar 


git init
git add README.md
git commit -m "first commit"
git branch -M main
git remote add origin git@github.com:jybecnu163/jobs-admin.git
git push -u origin main

