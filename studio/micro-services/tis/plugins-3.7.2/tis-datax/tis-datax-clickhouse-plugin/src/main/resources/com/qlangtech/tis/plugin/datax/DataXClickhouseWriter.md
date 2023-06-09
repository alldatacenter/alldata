## autoCreateTable

 在开始执行DataX任务前，自动在目标数据库中创建表，目标表Engine类型为'CollapsingMergeTree' 构建原理请参考[MySQL到ClickHouse实时同步](https://www.askcug.com/topic/76/mysql%E5%88%B0clickhouse%E5%AE%9E%E6%97%B6%E5%90%8C%E6%AD%A5-cloudcanal%E5%AE%9E%E6%88%98)
 
## createDDL

 此处为TIS系统按照数据源元数据自动生成的Clickhouse建表脚本，由于Clickhouse的数据类型比较丰富，用户可以在此脚本基础上根据自身业务特点进行修正。 
