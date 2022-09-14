# 组件二开，定制化或者定期升级到最新社区稳定版本

基于DataSphereStudio二开，打造实时画布开发平台

//    应用工具	描述	DSS0.X 兼容版本(推荐 DSS0.9.1)	DSS1.0 兼容版本(推荐 DSS1.1.0)
//    todo 引入Linkis	计算中间件 Apache Linkis，通过提供 REST/WebSocket/JDBC/SDK 等标准接口，上层应用可以方便地连接访问 MySQL/Spark/Hive/Presto/Flink 等底层引擎.	推荐 Linkis0.11.0（已发布）	>= Linkis1.1.1（已发布）
//    todo 移除DataApiService	（DSS已内置的第三方应用工具）数据API服务。可快速将SQL脚本发布为一个 Restful 接口，对外提供 Rest 访问能力。	不支持	推荐 DSS1.1.0（已发布）
//    todo 引入Scriptis	（DSS 已内置的第三方应用工具）支持在线写 SQL、Pyspark、HiveQL 等脚本，提交给 Linkis 执行的数据分析 Web 工具。	推荐 DSS0.9.1（已发布）	推荐 DSS1.1.0（已发布）
//    todo 移除Schedulis	基于 Azkaban 二次开发的工作流任务调度系统,具备高性能，高可用和多租户资源隔离等金融级特性。	推荐 Schedulis0.6.1（已发布）	>= Schedulis0.7.0（已发布）
//    todo 移除EventCheck	（DSS 已内置的第三方应用工具）提供跨业务、跨工程和跨工作流的信号通信能力。	推荐 DSS0.9.1（已发布）	推荐 DSS1.1.0（已发布）
//    todo 移除SendEmail	（DSS 已内置的第三方应用工具）提供数据发送能力，所有其他工作流节点的结果集，都可以通过邮件进行发送	推荐 DSS0.9.1（已发布）	推荐 DSS1.1.0（已发布）
//    todo 移除Qualitis	数据质量校验工具，提供数据完整性、正确性等数据校验能力	推荐 Qualitis0.8.0（已发布）	>= Qualitis0.9.2（已发布）
//    todo 移除Streamis	流式应用开发管理工具。支持发布 Flink Jar 和 Flink SQL ，提供流式应用的开发调试和生产管理能力，如：启停、状态监控、checkpoint 等。	不支持	>= Streamis0.2.0（已发布）
//    todo 移除Prophecis	一站式机器学习平台，集成多种开源机器学习框架。Prophecis 的 MLFlow 通过 AppConn 可以接入到 DSS 工作流中。	不支持	>= Prophecis 0.3.2（已发布）
//    todo 移除Exchangis	支持对结构化及无结构化的异构数据源之间的数据传输的数据交换平台，即将发布的 Exchangis1.0，将与 DSS 工作流打通	不支持	= Exchangis1.0.0（已发布）
//    todo 移除Visualis	基于宜信开源项目 Davinci 二次开发的数据可视化 BI 工具，为用户在数据安全方面提供金融级数据可视化能力。	推荐 Visualis0.5.0	= Visualis1.0.0（已发布）
//    DolphinScheduler	Apache DolphinScheduler，分布式易扩展的可视化工作流任务调度平台，支持一键将DSS工作流发布到 DolphinScheduler。	不支持	DolphinScheduler1.3.X（已发布）
//    todo 移除UserGuide	（DSS 将内置的第三方应用工具）包含帮助文档、新手指引、Dark模式换肤等。	不支持	>= DSS1.1.0（已发布）
//    todo 移除DataModelCenter	（DSS 将内置的第三方应用工具）主要提供数仓规划、数据模型开发和数据资产管理的能力。数仓规划包含主题域、数仓分层、修饰词等；数据模型开发包含指标、维度、度量、向导式建表等；数据资产打通 Apache Atlas，提供数据血缘能力。	不支持	规划在 DSS1.2.0（开发中）
//    todo 移除UserManager	（DSS 已内置的第三方应用工具）自动初始化一个 DSS 新用户所必须的所有用户环境，包含：创建 Linux 用户、各种用户路径、目录授权等。	推荐 DSS0.9.1（已发布）	规划中
//    todo 移除Airflow	支持将 DSS 工作流发布到 Apache Airflow 进行定时调度。	PR 尚未合并	不支持