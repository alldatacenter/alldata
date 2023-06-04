# Description

# Overview

InLong-Sort is used to extract data from different source systems, then transforms the data and finally loads the data
into diffrent storage systems.
InLong-Sort is simply a Flink Application, and relys on InLong-Manager to manage meta data(such as the source
informations and storage informations).

# Features

## Supported Extract Node

- Pulsar
- MySQL
- Kafka
- MongoDB
- PostgreSQL
- HDFS

## Supported Transform

- String Split
- String Regular Replace
- String Regular Replace First Matched Value
- Data Filter
- Data Distinct
- Regular Join

## Supported Load Node

- Hive
- Kafka
- HBase
- ClickHouse
- Iceberg
- Hudi 
- PostgreSQL
- HDFS
- TDSQL Postgres
- Redis 

## Future Plans

### More kinds of Extract Node

Oracle, SqlServer, and etc.

### More kinds of Transform

Time window aggregation, Content extraction, Type conversion, Time format conversion, and etc.

### More kinds of Load Node

Elasticsearch, and etc.
