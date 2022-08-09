# FLINK1.13.6引入最新OGG-JSON解析

```markdown

适配Flink1.13 增加OGG Format

1、mvn clean package -DskipTests=TRUE

2、cp flink-ogg-json-1.0-SNAPSHOT /*/flink1.13.6/lib/

3、启动Flink On Yarn

./bin/yarn-session.sh -n 8 -jm 1024 -tm 1024 -s 4 -d

4、FlinkSQL开发Kafka to Hudi

USE CATALOG default_catalog;
create database mydatabase;
use mydatabase;
set execution.checkpointing.interval=3sec;
CREATE TABLE IF not exists ogg_kafka_source (
ID INT,
NAME varchar(255),
`ts` TIMESTAMP(3) METADATA FROM 'timestamp'
) WITH (
'connector' = 'kafka',
'topic' = 'ogg_kafka_topic',
'properties.bootstrap.servers' = 'localhost:9092',
'properties.group.id' = 'testGroup',
'format' = 'ogg-json'
);

-- SINK
CREATE TABLE IF NOT EXISTS ogg_kafka_hudi (
ID INT,
NAME varchar(255),
ts TIMESTAMP(3),
`partition` VARCHAR(20),
primary key(ID) not enforced
)
PARTITIONED BY (`partition`)
with (
'connector'='hudi'
, 'path'= 's3://test/ogg_kafka_test'
, 'hoodie.datasource.write.recordkey.field'= 'ID'
, 'write.precombine.field' = 'ts'
, 'write.precombine' = 'true'
, 'index.global.enabled' = 'true'
, 'index.state.ttl' = '2'
, 'changelog.enable' = 'true'
-- cow去重设置  , 'write.insert.drop.duplicates' = 'true'
, 'write.tasks'= '1'
, 'compaction.tasks'= '1'
, 'write.rate.limit'= '2000'
, 'table.type'= 'COPY_ON_WRITE'
, 'compaction.async.enabled'= 'true'
, 'compaction.trigger.strategy'= 'num_commits'
, 'compaction.delta_commits'= '1'
--, 'changelog.enabled'= 'true'
, 'read.streaming.enabled'= 'true'
, 'read.streaming.check-interval'= '3'
, 'hive_sync.enable'= 'true'
, 'hive_sync.mode'='hms'
, 'hive_sync.metastore.uris'='thrift://localhost:9083'
, 'hive_sync.table'= 'kafka_hive_table'
, 'hive_sync.db'= 'kafka_hive_db'
, 'hive_sync.support_timestamp'= 'true'
, 'hive_sync.partition_extractor_class' = 'org.apache.hudi.hive.HiveStylePartitionValueExtractor'
, 'hoodie.datasource.write.partitionpath.field' = 'dt'
, 'hoodie.datasource.write.hive_style_partitioning' = 'true');

-- -- TRANSFROM
INSERT INTO ogg_kafka_hudi SELECT *, '20220612' FROM ogg_kafka_hudi;


-- SINK
select * from ogg_kafka_hudi limit 10;

5、 s3成功写入20220612分区数据

/test/ogg_kafka_test/partition=20220612/951a5353-d458-4882-a2cb-d86054542601_0-1-0_20220617100948261.parquet


```