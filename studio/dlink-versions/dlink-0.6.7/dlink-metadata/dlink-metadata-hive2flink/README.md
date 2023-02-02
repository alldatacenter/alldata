# DINKY SUPPORT RUNNING HIVEQL ON FLINK

## 1、新增hive2flink元数据

## 2、新增hive2flink任务类型

## 3、引入flink-sql-parser-hive解析HiveSQL

## 4、启用Flink natively support REST Endpoint and HiveServer2 Endpoint

### 4.1 ./bin/sql-gateway.sh start -Dsql-gateway.endpoint.type=hiveserver2

### 4.2 Flink CONF配置：sql-gateway.endpoint.type: hiveserver2