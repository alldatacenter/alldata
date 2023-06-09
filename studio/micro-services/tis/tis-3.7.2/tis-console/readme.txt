集群日志收集：
 启动类：ClusterStateCollectManager
 配置文件：/src/main/resources/cluster-status-collect-context.xml

定时任务:
 启动类： com.qlangtech.tis.trigger.TisTriggerJobManage
 配置文件：com/qlangtech/tis/trigger/trigger.context.xml

 配置发布:
 发送配置：Savefilecontentaction 方法：doSyncDailyConfig
 接收配置：AppSynAction 方法：doInitAppFromDaily
