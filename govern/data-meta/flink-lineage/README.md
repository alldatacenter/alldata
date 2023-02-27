# FLINK LINEAGE FOR ALL DATA PLATFORM

## 1 创建FlinkDDL

### 参考Resource/FlinkDDLSQL.sql

> CREATE TABLE data_gen (
> 
> amount BIGINT
> 
> ) WITH (
> 
> 'connector' = 'datagen',
> 
> 'rows-per-second' = '1',
> 
> 'number-of-rows' = '3',
> 
> 'fields.amount.kind' = 'random',
> 
> 'fields.amount.min' = '10',
> 
> 'fields.amount.max' = '11');
> 
> CREATE TABLE mysql_sink (
> 
> amount BIGINT,
> 
> PRIMARY KEY (amount) NOT ENFORCED
> 
> ) WITH (
> 
> 'connector' = 'jdbc',
> 
> 'url' = 'jdbc:mysql://localhost:3306/test_db',
> 
> 'table-name' = 'test_table',
> 
> 'username' = 'root',
> 
> 'password' = '123456',
> 
> 'lookup.cache.max-rows' = '5000',
> 
> 'lookup.cache.ttl' = '10min'
> 
> );
> 
> INSERT INTO mysql_sink SELECT amount as amount FROM data_gen;

## 2 执行com.platform.FlinkLineageBuild 

### 获取结果

> 1、Flink血缘构建结果-表:
> 
> [LineageTable{id='4', name='data_gen', columns=[LineageColumn{name='amount', title='amount'}]}, 
> 
> LineageTable{id='6', name='mysql_sink', columns=[LineageColumn{name='amount', title='amount'}]}]
> 
> 表ID: 4
> 
> 表Namedata_gen
> 
> 表ID: 4
> 
> 表Namedata_gen
> 
> 表-列LineageColumn{name='amount', title='amount'}
> 
> 表ID: 6
> 
> 表Namemysql_sink
> 
> 表ID: 6
> 
> 表Namemysql_sink
> 
> 表-列LineageColumn{name='amount', title='amount'}
> 
> 2、Flink血缘构建结果-边:
> 
> [LineageRelation{id='1', srcTableId='4', tgtTableId='6', srcTableColName='amount', tgtTableColName='amount'}]
> 
> 表-边: LineageRelation{id='1', srcTableId='4', tgtTableId='6', srcTableColName='amount', tgtTableColName='amount'}