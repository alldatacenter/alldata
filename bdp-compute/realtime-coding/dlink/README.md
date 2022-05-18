# DLink FOR DATA PLATFORM

```markdown

基于开源DLink 二次开发

1、Dlink主要参考Flink的ClusterClient源码以及JobGraph源码

https://nightlies.apache.org/flink/flink-docs-master/

2、利用Yarn, Flink封装Gateway切换提交Sql方式

3、引用hudi-flink-bundle完成Flink CDC实时入湖Hudi

https://hudi.apache.org/docs/hoodie_deltastreamer#cdc-ingestion

4、支持输出告警信息到dingTalk, wechat等协同工具

5、全库同步时重新定义的统一的DataSource

6、元数据方面支持8种元数据管理，方便快手进行实时开发:

ClickHouse、Doris、Hive、Mysql、Oracle、Phoenix、PostgreSql、SqlServer

7、通过Java SPI的架构完成不同Flink11-16版本的切换部署

8、一款轻量级的IDE, 支持调试SQL，但是不具备FlinkCEP能力，已经无法做拖拉拽的实时开发

9、具备丰富的运维管理，但是缺少日志监控和运维方案建议

10、即席查询功能并不完善

11、数据地图区别于Apache Atlas

通过Flink原生的JobGraph以及ObjectNode，TranslatePlan完成图的绘制

通过返回tables数组与relations数组完成血缘关系的绘制

12、数据质量管理不完善

```
