## Stargazers over time

[![Stargazers over time](https://starchart.cc/qlangtech/tis-solr.svg)](https://starchart.cc/qlangtech/tis-solr)
![tis](docs/tis-logo.png)

## TIS介绍

TIS快速为您构建企业级实时数仓库服务，**基于批(DataX)流(Flink-CDC、Chunjun)一体数据中台，提供简单易用的操作界面，降低用户实施各端（MySQL、PostgreSQL、Oracle、ElasticSearch、ClickHouse、Doris等）
之间数据同步的实施门槛，缩短任务配置时间，避免配置过程中出错，使数据同步变得简单、有趣且容易上手** [详细介绍](http://tis.pub/docs/) 
<!--
TIS平台是一套为企业级用户提供大数据多维、实时、查询的搜索中台产品。用户可以在TIS上自助快速构建搜索服务，它大大降低了搜索技术的门槛 [详细说明](http://tis.pub/docs/) 
> 视频： [>>TIS介绍](https://www.bilibili.com/video/BV11y4y1B7Mk) [>>操作实例](https://www.bilibili.com/video/BV1Uv41167SH/)
 -->

## v3.6.0-alpha发布（2022/11/01）：

https://github.com/qlangtech/tis/releases/tag/v3.6.0-alpha
 
## 安装说明

  速将TIS在分布式环境中一键安装（支持私有云、公有云等环境），方便、快捷 [详细说明](http://tis.pub/docs/install/uber)

## 架构

 ![tis](docs/tis-synoptic.png)

## 支持的读写组件
|Reader|Writer|
|--|--|
|<img src="docs/logo/cassandra.svg" width="40" /><img src="docs/logo/ftp.svg" width="40" />  <img src="docs/logo/hdfs.svg" width="40" /> <img src="docs/logo/mongodb.svg" width="40" />  <img src="docs/logo/mysql.svg" width="40" /> <img src="docs/logo/oracle.svg" width="40" />  <img src="docs/logo/oss.svg" width="40" />  <img src="docs/logo/postgresql.svg" width="40" /> <img src="docs/logo/sqlserver.svg" width="40" /> <img src="docs/logo/tidb.svg" width="40" /> | <img src="docs/logo/mysql.svg" width="40" /> <img src="docs/logo/doris.svg" width="40" /> <img src="docs/logo/spark.svg" width="40" /><img src="docs/logo/starrocks.svg" width="40" /><img src="docs/logo/cassandra.svg" width="40" /> <img src="docs/logo/postgresql.svg" width="40" /><img src="docs/logo/hive.svg" width="40" /><img src="docs/logo/clickhouse.svg" width="40" /><img src="docs/logo/ftp.svg" width="40" /><img src="docs/logo/oracle.svg" width="40" /> <img src="docs/logo/hdfs.svg" width="40" /><img src="docs/logo/es.svg" width="40" /> |

[详细](https://tis.pub/docs/plugin/source-sink/)

## 功能一瞥 
- 示例
    * [基于TIS快速实现MySQL到StarRocks的实时数据同步方案](http://tis.pub/docs/example/mysql-syn-starrocks/)
- 视频示例
    * [安装示例](https://www.bilibili.com/video/BV18q4y1p73B/)
    * [启用分布式执行功能](https://www.bilibili.com/video/BV1Cq4y1D7z4?share_source=copy_web)
    * [MySQL导入ElasticSearch](https://www.bilibili.com/video/BV1G64y1B7wm?share_source=copy_web)
    * [MySQL导入Hive](https://www.bilibili.com/video/BV1Vb4y1z7DN?share_source=copy_web)
    * [MySQL导入Clickhouse](https://www.bilibili.com/video/BV1x64y1B7V8/)

### 批量导入流程设置 

选择Reader/Writer插件类型
  ![tis](docs/datax-add-step2.png)

添加MySqlReader
  ![tis](docs/add-mysql-reader.png)

设置MySqlReader目标表、列  
   ![tis](docs/select-tab-cols.png)
   
添加ElasticWriter,可视化设置ElasticSearch的Schema Mapping
   ![tis](docs/add-elastic-writer.png) 

执行MySql->ElasticSearch DataX实例，运行状态 
   ![tis](docs/datax-exec-status.png) 

### 开通Flink实时数据通道 

添加Flink-Cluster、设置重启策略、CheckPoint机制等
   ![tis](docs/incr_step_1.png) 
   
设置Source/Sink组件属性
   ![tis](docs/incr_step_2.png)    
   
TIS基于数据库元数据信息自动生成Flink-SQL脚本,您没看错全部脚本自动生！
   ![tis](docs/incr_step_3.png) 
   
实时数据通道创建完成！构建一个实时数仓就这么简单！！！   
   ![tis](docs/incr_step_4.png) 
   
## 相关代码 

- WEB UI [https://github.com/qlangtech/ng-tis](https://github.com/qlangtech/ng-tis)
- 基于Ansible的打包工具 [https://github.com/qlangtech/tis-ansible](https://github.com/qlangtech/tis-ansible)
- TIS 插件 [https://github.com/qlangtech/plugins](https://github.com/qlangtech/plugins)
- TIS 插件元数据生成工具 [https://github.com/qlangtech/update-center2](https://github.com/qlangtech/update-center2)
- DataX [https://github.com/qlangtech/DataX](https://github.com/qlangtech/DataX)
- tis-logback-flume-appender [https://github.com/baisui1981/tis-logback-flume-appender](https://github.com/baisui1981/tis-logback-flume-appender)
- Flink Extend [https://github.com/qlangtech/flink](https://github.com/qlangtech/flink)
- Chunjun [https://github.com/qlangtech/chunjun](https://github.com/qlangtech/chunjun)

## 如何开发

[http://tis.pub/docs/guide/develop/compile-running/](http://tis.pub/docs/guide/develop/compile-running/)
 
## 许可协议

 TIS is under the Apache2 License. See the [LICENSE](https://github.com/qlangtech/tis-solr/blob/master/LICENSE) file for details.
 
## 反馈
 
  您在使用过程中对TIS有任何不满或者批评都请不惜斧正，您提出的宝贵意见是对我们最大的支持和鼓励，[我要提建议](https://github.com/qlangtech/tis-solr/issues/new)
