## Stargazers over time

[![Stargazers over time](https://starchart.cc/qlangtech/tis-solr.svg)](https://starchart.cc/qlangtech/tis-solr)
![tis](docs/tis-logo.png)

## TIS介绍

TIS快速为您提供企业级数据集成产品，**基于批(DataX)流(Flink-CDC、Chunjun)一体数据中台，提供简单易用的操作界面，降低用户实施各端（MySQL、PostgreSQL、Oracle、ElasticSearch、ClickHouse、Doris等）
之间数据同步的实施门槛，缩短任务配置时间，避免配置过程中出错，使数据同步变得简单、有趣且容易上手** [详细介绍](https://tis.pub/docs/) 
<!--
TIS平台是一套为企业级用户提供大数据多维、实时、查询的搜索中台产品。用户可以在TIS上自助快速构建搜索服务，它大大降低了搜索技术的门槛 [详细说明](http://tis.pub/docs/) 
> 视频： [>>TIS介绍](https://www.bilibili.com/video/BV11y4y1B7Mk) [>>操作实例](https://www.bilibili.com/video/BV1Uv41167SH/)
 -->

## v3.7.2发布（2023/6/01）：

https://github.com/qlangtech/tis/releases/tag/v3.7.2
 
## 安装说明

  速将TIS在分布式环境中一键安装（支持私有云、公有云等环境），方便、快捷 [详细说明](https://tis.pub/docs/install/tis/uber)

## 架构

 ![tis](docs/tis-synoptic.png)
 
 ![tis](docs/conceptual-diagram.png)

## 核心特性

* 简单易用
  
  TIS的安装还是和传统软件安装一样，只需要三个步骤，一、下载tar包，二、解压tar包，三、启动TIS。是的，就这么简单。系统借助底层的MetaData 自动生成脚本，**大部份情况下，键盘成为摆设，用户只需轻点鼠标**，借助系统给出的提示快速录入配置。
  
* 扩展性强
  
  TIS 继承了Jenkin 的设计思想，使用微前端技术，重新构建了一套前端框架，前端页面可以自动渲染。
  
  TIS 提供了良好的扩展性和SPI机制，开发者可以很轻松地开发新的插件。具体来说，第三方开发者可在不修改 TIS 底座框架代码的前提下，就能轻松构建一个新的数据端插件。大大增强了 TIS 的生态扩展性，可以让更多第三方开发者加入进来。
  
* 基于白屏化操作
  
  将传统的 ETL 工具执行以黑屏化工具包的方式（json+命令行执行）升级到白屏化 2.0的产品化方式，可以大大提高工作效率。
  ​所谓 白屏化1.0，虽然也是基于 UI 界面的，但是交互方式基本上是给一个大大的 TextArea，里面填写的 Json、XML、Yaml 需要用户自己发挥了，这对用户来说执行效率还是太低了，我们暂且称这交互方式的系统为DevOps系统，TIS需要跨越到 白屏化2.0 的DataOps系统。

* 基于 DataOps 理念

  TIS有别于传统大数据 ETL 工具，它借鉴了 DataOps、DataPipeline 理念，对大数据 ETL 各个执行流程建模。需要产品能够屏蔽底层技术细节，例如，不需要了解底层的数据导入模块的实现原理，只需要告诉系统目标源的配置信息就行。


## 支持的读写组件
|Reader|Writer|
|--|--|
|<img src="docs/logo/cassandra.svg" width="40" /><img src="docs/logo/ftp.svg" width="40" />  <img src="docs/logo/hdfs.svg" width="40" /> <img src="docs/logo/mongodb.svg" width="40" />  <img src="docs/logo/mysql.svg" width="40" /> <img src="docs/logo/oracle.svg" width="40" />  <img src="docs/logo/oss.svg" width="40" />  <img src="docs/logo/postgresql.svg" width="40" /> <img src="docs/logo/sqlserver.svg" width="40" /> <img src="docs/logo/tidb.svg" width="40" /> | <img src="docs/logo/mysql.svg" width="40" /> <img src="docs/logo/doris.svg" width="40" /> <img src="docs/logo/spark.svg" width="40" /><img src="docs/logo/starrocks.svg" width="40" /><img src="docs/logo/cassandra.svg" width="40" /> <img src="docs/logo/postgresql.svg" width="40" /><img src="docs/logo/hive.svg" width="40" /><img src="docs/logo/clickhouse.svg" width="40" /><img src="docs/logo/ftp.svg" width="40" /><img src="docs/logo/oracle.svg" width="40" /> <img src="docs/logo/hdfs.svg" width="40" /><img src="docs/logo/es.svg" width="40" /> |

[详细](https://tis.pub/docs/plugin/source-sink/)

## 功能一瞥 
- 示例                                                 
    * [基于TIS快速实现MySQL到StarRocks的实时数据同步方案](https://tis.pub/docs/example/mysql-sync-starrocks)
    * [多源同步Doris方案](https://tis.pub/docs/example/mysql-sync-doris)
    * [将数据变更同步到Kafka](https://tis.pub/docs/example/sink-2-kafka)
    * [利用TIS实现T+1离线分析](https://tis.pub/docs/example/dataflow)
- 视频示例
    * [安装示例](https://www.bilibili.com/video/BV18q4y1p73B/)
    * [启用分布式执行功能](https://www.bilibili.com/video/BV1Cq4y1D7z4?share_source=copy_web)
    * [MySQL导入ElasticSearch](https://www.bilibili.com/video/BV1G64y1B7wm?share_source=copy_web)
    * [MySQL导入Hive](https://www.bilibili.com/video/BV1Vb4y1z7DN?share_source=copy_web)
    * [MySQL导入Clickhouse](https://www.bilibili.com/video/BV1x64y1B7V8/)
    * [MySQL同步StarRocks](https://www.bilibili.com/video/BV19o4y1M7eq/)

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
- Zeppelin [https://github.com/qlangtech/zeppelin](https://github.com/qlangtech/zeppelin)
- 部分插件参数配置参考 Airbyte [https://github.com/airbytehq/airbyte](https://github.com/airbytehq/airbyte) 

## 如何开发

[https://tis.pub/docs/develop/compile-running/](https://tis.pub/docs/develop/compile-running/)
 
## 许可协议

 TIS is under the Apache2 License. See the [LICENSE](https://github.com/qlangtech/tis-solr/blob/master/LICENSE) file for details.
 
## 反馈
 
  您在使用过程中对TIS有任何不满或者批评都请不惜斧正，您提出的宝贵意见是对我们最大的支持和鼓励，[我要提建议](https://github.com/qlangtech/tis/issues/new)
