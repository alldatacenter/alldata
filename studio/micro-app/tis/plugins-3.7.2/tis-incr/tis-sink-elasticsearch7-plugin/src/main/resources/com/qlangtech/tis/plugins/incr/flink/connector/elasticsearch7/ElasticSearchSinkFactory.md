## bulkFlushMaxActions

设置使 sink 在接收每个元素之后立即提交，否则这些元素将被缓存起来，官方文档：
[https://nightlies.apache.org/flink/flink-docs-master/zh/docs/connectors/datastream/elasticsearch/#%e9%85%8d%e7%bd%ae%e5%86%85%e9%83%a8%e6%89%b9%e9%87%8f%e5%a4%84%e7%90%86%e5%99%a8](https://nightlies.apache.org/flink/flink-docs-master/zh/docs/connectors/datastream/elasticsearch/#%e9%85%8d%e7%bd%ae%e5%86%85%e9%83%a8%e6%89%b9%e9%87%8f%e5%a4%84%e7%90%86%e5%99%a8)

## bulkFlushMaxSizeMb

刷新前最大缓存的数据量（以兆字节为单位）

## bulkFlushIntervalMs

刷新的时间间隔（不论缓存操作的数量或大小如何），默认10秒自动提交一次
