## sourceOrderingField

Field within source record to decide how to break ties between records with same key in input data.
 
Default: 'ts' holding unix timestamp of record Default: ts

## recordField

详细说明：[hoodie.datasource.write.recordkey.field](https://hudi.apache.org/docs/configurations/#hoodiedatasourcewriterecordkeyfield-1)

## keyGenerator

Every record in Hudi is uniquely identified by a primary key, which is a pair of record key and partition path where the record belongs to. Using primary keys, Hudi can impose a) partition level uniqueness integrity constraint b) enable fast updates and deletes on records. One should choose the partitioning scheme wisely as it could be a determining factor for your ingestion and query latency.

详细说明：[https://hudi.apache.org/docs/key_generation/#complexkeygenerator](https://hudi.apache.org/docs/key_generation/#complexkeygenerator)



