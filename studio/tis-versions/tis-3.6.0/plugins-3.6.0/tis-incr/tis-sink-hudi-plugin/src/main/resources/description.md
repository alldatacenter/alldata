封装[Apache Hudi](https://hudi.apache.org/)，为用户提供一站式、开箱即用的千表入湖的解决方案

功能：

本组件整合Hudi提供的[Stream API Demo](https://github.com/apache/hudi/blob/master/hudi-flink-datasource/hudi-flink/src/main/java/org/apache/hudi/streamer/HoodieFlinkStreamer.java)功能，
通过TIS中的元配置信息自动填充[FlinkStreamerConfig](https://github.com/apache/hudi/blob/master/hudi-flink-datasource/hudi-flink/src/main/java/org/apache/hudi/streamer/FlinkStreamerConfig.java)
实例所需要配置属性，依赖配置信息（Avro schemas，Hudi表分区信息及 [Key Generation](https://hudi.apache.org/docs/key_generation)配置），

配合TIS提供的各种Source Flink CDC 组件（MySQL，PostgreSQL，SqlServer等）用户可通过TIS控制台，快速实现各种数据源`实时增量`入湖

依赖组件:

 | 组件名称| 版本    |
 | -------- | -----  |
 | Apache Hudi     | ${hudi.version} |
 | Apache Spark   |  ${spark2.version} |
 | Apache Flink   | ${flink.version} |
 | Apache Hive     | ${hive.version}  |
 | Apache Hadoop  | ${hadoop-version} | 





