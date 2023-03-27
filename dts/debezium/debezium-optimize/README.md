> 1、从 Debezium Server Consumer 读取 Debezium Metrics。
> 
> 2、通过访问指标，消费者可以实现许多功能，比如动态控制消费速度。
> 
> 3、基于debezium-storage二开增加debezium-storage-mysql
> 
> 表描述的数据库模式的历史。 可以记录对数据库模式的更改，并且可以将数据库模式恢复到该历史记录中的各个点。
> 
> 3.1 新增一个 SchemaHistory 实现，它记录模式更改作为指定主题上的正常 SourceRecords
> 
> 3.2 并通过建立 Kafka 消费者重新处理该主题上的所有消息来恢复历史记录
> 