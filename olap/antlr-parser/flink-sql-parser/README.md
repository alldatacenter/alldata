### 介绍

### 整库同步

```sql
-- 分布表，合并写入目标库中
CREATE DATABASE IF NOT EXISTS flink_cdc_demos 
WITH (
    'connector' = 'jdbc',
    'url' = 'jdbc:mysql://172.18.1.55:3306/flink_cdc_demos?characterEncoding=utf-8&useSSL=false',
    'username' = 'root',
    'password' = 'Datac@123',
    'table-name' = '${'$'}{tableName}',
    'sink.buffer-flush.interval' = '2s',
    'sink.buffer-flush.max-rows' = '100',
    'sink.max-retries' = '5'
) 
AS DATABASE `cdc_demos_[0-9]+` INCLUDING ALL TABLES  
OPTIONS(
    'connector' = 'mysql-cdc',
    'hostname' = '172.18.5.44',
    'port' = '3306',
    'username' = 'root',
    'password' = 'root2023',
    'checkpoint' = '10000',
    'scan.startup.mode' = 'initial',
    'parallelism' = '1'
)
```

### 分库分表同步

```sql
CREATE TABLE IF NOT EXISTS flink_cdc_demos.account
WITH (
    'connector' = 'jdbc',
    'url' = 'jdbc:mysql://172.18.1.55:3306/flink_cdc_demos?characterEncoding=utf-8&useSSL=false',
    'username' = 'root',
    'password' = 'Datac@123',
    'table-name' = 'account',
    'sink.buffer-flush.interval' = '2s',
    'sink.buffer-flush.max-rows' = '100',
    'sink.max-retries' = '5'
) 
AS TABLE `cdc_demos_[0-9]*`.`account_[0-9]*`
OPTIONS(
    'connector' = 'mysql-cdc',
    'hostname' = '172.18.5.44',
    'port' = '3306',
    'username' = 'root',
    'password' = 'root2023',
    'checkpoint' = '10000',
    'scan.startup.mode' = 'initial',
    'parallelism' = '2'
)
```

### 参考
1. [Dinky在Doris实时整库同步和模式演变的探索实践](https://mp.weixin.qq.com/s/OVsbRbfLEIgSNurlP6woAg)
2. [Dinky 数据集成](http://www.dlink.top/docs/next/data_integration_guide/cdcsource_statements)
3. [CREATE DATABASE AS（CDAS）语句](https://help.aliyun.com/document_detail/374304.html)
4. [数据库类型转换](https://www.sqlines.com/oracle-to-mysql)