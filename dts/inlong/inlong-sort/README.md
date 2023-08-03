# Description

## Overview
InLong Sort is used to extract data from different source systems, then transforms the data and finally loads the data into different storage systems.

InLong Sort can be used together with the Manager to manage metadata, or it can run independently in the Flink environment.

## Features
### Supports a variety of data nodes

| Type         | Service                                    |
|--------------|--------------------------------------------|
| Extract Node | Pulsar                                     | 
|              | MySQL                                      | 
|              | Kafka                                      | 
|              | MongoDB                                    | 
|              | PostgreSQL                                 | 
| Transform    | String Split                               | 
|              | String Regular Replace                     | 
|              | String Regular Replace First Matched Value | 
|              | Data Filter                                |
|              | Data Distinct                              | 
|              | Regular Join                               | 
| Load Node    | Hive                                       | 
|              | Kafka                                      | 
|              | HBase                                      | 
|              | ClickHouse                                 | 
|              | Iceberg                                    | 
|              | PostgreSQL                                 | 
|              | HDFS                                       | 
|              | TDSQL Postgres                             | 
|              | Hudi                                       | 

## Build
### For Apache Flink 1.13 (default)
```
mvn clean install -DskipTests
```

### For Apache Flink 1.15
Modify root pom `<sort.flink.version>v1.15</sort.flink.version>`, then execute:
```
mvn clean install -DskipTests -P v1.15
```
