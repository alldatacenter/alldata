# 数据平台介绍 
数据平台主要包含多个子系统的开发，项目采用Dubbo微服务架构，使用Altas作为服务治理，包括spark，storm，flink，scala，python等技术栈
# 数据平台展示地址：
http://120.77.155.220:8081/BigDataPlatForm/index.html
# 数据来源：
	商城前台：
		mall-shopping-android: 商城app
		mall-shopping-wc: 商城小程序
		mall-shopping-mobile: 商城前台
		mall-shopping-pc: 商城pc端
		mall-shopping-service: 商城前台服务
	商城后台：
		mall-admin-web: 商城后台
		mall-admin-service: 商城后台服务
# 数据收集：
	log-collect-server: 
		服务端日志收集系统
	log-collect-client: 
		支持各app集成的客户端SDK，负责收集app客户端数据；
	data-import-export: 
		基于DataX实现数据集成(导入导出)
	data-spider:
		爬虫平台支持可配置的爬取公网数据的任务开发；
# 数据存储：
	分布式文件系统：hdfs
	分布式数据库：hbase、mongodb、elasticsearch
	分布式内存存储系统：redis
# 数据计算：
	compute-mr（离线计算）: Hive、MR
	compute-realtime（流计算）: storm、flink
	multi-dimension-analysis（多维度分析）: kylin, spark
# 数据开发：
	task-schedular: 任务调度
	task-ops: 任务运维
# 数据产品：
	data-face: 数据可视化
	data-insight: 用户画像分析
# 数据应用：
	system-recommender: 推荐
	system-ad: 广告
	system-search: 搜索
	system-anti-cheating: 反作弊
	system-report-analysis: 报表分析
	system-elk: ELK日志系统，实现日志搜索平台
	system-apm: skywalking监控平台
	system-deploy: k8s，scala，playframework，docker打包平台。
	system-tasksubmit: 任务提交平台
