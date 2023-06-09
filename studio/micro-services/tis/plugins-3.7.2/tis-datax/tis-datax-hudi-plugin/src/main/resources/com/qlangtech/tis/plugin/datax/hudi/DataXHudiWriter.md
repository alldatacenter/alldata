## fsName
分布式文件系统源引用，

`注`：实例名称必须为**hudi_hdfs**

## partitionedBy

Field in the table to use for determining hive partition columns, [详细](https://hudi.apache.org/docs/configurations#hoodiedatasourcehive_syncpartition_fields)

## hiveConn

Hive连接实例配置

## tabType

Hudi 支持以下两种表类型：

* [COPY_ON_WRITE](https://hudi.apache.org/docs/table_types#copy-on-write-table) ：Stores data using exclusively columnar file formats (e.g parquet). Updates simply version & rewrite the files by performing a synchronous merge during write.
* [MERGE_ON_READ](https://hudi.apache.org/docs/table_types#merge-on-read-table) ：Stores data using a combination of columnar (e.g parquet) + row based (e.g avro) file formats. Updates are logged to delta files & later compacted to produce new versions of columnar files synchronously or asynchronously.

详细请参考 [https://hudi.apache.org/docs/table_types](https://hudi.apache.org/docs/table_types)

**How do I choose a storage type for my workload**

A key goal of Hudi is to provide upsert functionality that is orders of magnitude faster than rewriting entire tables or partitions. 

Choose Copy-on-write storage if : 

- You are looking for a simple alternative, that replaces your existing parquet tables without any need for real-time data.
- Your current job is rewriting entire table/partition to deal with updates, while only a few files actually change in each partition.
- You are happy keeping things operationally simpler (no compaction etc), with the ingestion/write performance bound by the parquet file size and the number of such files affected/dirtied by updates
- Your workload is fairly well-understood and does not have sudden bursts of large amount of update or inserts to older partitions. COW absorbs all the merging cost on the writer side and thus these sudden changes can clog up your ingestion and interfere with meeting normal mode ingest latency targets.

Choose merge-on-read storage if :

- You want the data to be ingested as quickly & queryable as much as possible.
- Your workload can have sudden spikes/changes in pattern (e.g bulk updates to older transactions in upstream database causing lots of updates to old partitions on DFS). Asynchronous compaction helps amortize the write amplification caused by such scenarios, while normal ingestion keeps up with incoming stream of changes.
Immaterial of what you choose, Hudi provides 

Snapshot isolation and atomic write of batch of records
- Incremental pulls
- Ability to de-duplicate data

Find more [here](https://hudi.apache.org/docs/concepts/).



## shuffleParallelism

For any Hudi job using Spark, parallelism equals to the number of spark partitions that should be generated for a particular stage in the DAG
[detail](https://hudi.apache.org/docs/faq/#how-to-tune-shuffle-parallelism-of-hudi-jobs-)

## sparkConn

指定Spark服务端连接地址

## sparkSubmitParam
Spark服务端执行`HoodieDeltaStreamer`对内存有一定要求，太小容易产生OOM导致终止运行

## batchOp

* Takes one of these values : UPSERT (default), INSERT (use when input is  purely new data/inserts to gain speed)
* Default: `BULK_INSERT`
* Possible Values: `UPSERT`, `INSERT`, `BULK_INSERT`

