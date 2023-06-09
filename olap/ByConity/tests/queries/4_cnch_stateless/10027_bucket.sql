SET enable_multiple_tables_for_cnch_parts = 1;
DROP TABLE IF EXISTS bucket;
DROP TABLE IF EXISTS bucket2;
DROP TABLE IF EXISTS bucket3;
DROP TABLE IF EXISTS normal;
DROP TABLE IF EXISTS bucket_with_split_number;
DROP TABLE IF EXISTS bucket_with_split_number_n_range;
DROP TABLE IF EXISTS dts_bucket_with_split_number_n_range;
DROP TABLE IF EXISTS test_optimize;
DROP TABLE IF EXISTS test_optimize_with_date_column;


CREATE TABLE bucket (name String, age Int64) ENGINE = CnchMergeTree() PARTITION BY name CLUSTER BY age INTO 1 BUCKETS ORDER BY name;
CREATE TABLE bucket2 (name String, age Int64) ENGINE = CnchMergeTree() PARTITION BY name CLUSTER BY age INTO 1 BUCKETS ORDER BY name;
CREATE TABLE bucket3 (name String, age Int64) ENGINE = CnchMergeTree() PARTITION BY name CLUSTER BY age INTO 1 BUCKETS ORDER BY name;
CREATE TABLE normal (name String, age Int64) ENGINE = CnchMergeTree() PARTITION BY name ORDER BY name;
CREATE TABLE bucket_with_split_number (name String, age Int64) ENGINE = CnchMergeTree() PARTITION BY name CLUSTER BY (name, age) INTO 1 BUCKETS SPLIT_NUMBER 60 ORDER BY name;
CREATE TABLE bucket_with_split_number_n_range (name String, age Int64) ENGINE = CnchMergeTree() PARTITION BY name CLUSTER BY (name, age) INTO 1 BUCKETS SPLIT_NUMBER 60 WITH_RANGE ORDER BY name;
CREATE TABLE dts_bucket_with_split_number_n_range (name String, age Int64) ENGINE = CnchMergeTree() PARTITION BY name CLUSTER BY (name) INTO 1 BUCKETS SPLIT_NUMBER 60 WITH_RANGE ORDER BY name;
CREATE TABLE test_optimize (`id` UInt32, `code` UInt32, `record` String) ENGINE = CnchMergeTree PARTITION BY id CLUSTER BY code INTO 4 BUCKETS ORDER BY code;
CREATE TABLE test_optimize_with_date_column (`id` UInt32, `code` UInt32, `record` Date) ENGINE = CnchMergeTree PARTITION BY id CLUSTER BY record INTO 4 BUCKETS ORDER BY record;

-- Ensure bucket number is assigned to a part in bucket table
INSERT INTO bucket VALUES ('jane', 10);
SELECT * FROM bucket ORDER BY name FORMAT CSV;
SELECT bucket_number FROM system.cnch_parts where database = currentDatabase() and table = 'bucket' FORMAT CSV;

-- Ensure join queries between bucket tables work correctly
INSERT INTO bucket2 VALUES ('bob', 10);
SELECT * FROM bucket2 ORDER BY name FORMAT CSV;
SELECT b1.name, age, b2.name FROM bucket b1 JOIN bucket2 b2 USING (age) FORMAT CSV;

-- Attach part from bucket table to another bucket table of same table definition
ALTER TABLE bucket2 ATTACH PARTITION 'jane' from bucket;
SELECT partition FROM system.cnch_parts where database = currentDatabase() and table = 'bucket2' FORMAT CSV;

-- Attach part from bucket table to normal table
ALTER TABLE normal ATTACH PARTITION 'bob' from bucket2;
SELECT partition FROM system.cnch_parts where database = currentDatabase() and table = 'normal' FORMAT CSV;


ALTER TABLE bucket MODIFY CLUSTER BY age INTO 3 BUCKETS;
INSERT INTO bucket VALUES ('jane', 10);
SELECT * FROM bucket ORDER BY name FORMAT CSV;
SELECT bucket_number FROM system.cnch_parts where database = currentDatabase() and table = 'bucket' and active FORMAT CSV;

-- DROP bucket table definition, INSERT, ensure new part's bucket number is -1
ALTER TABLE bucket3 DROP CLUSTER;
INSERT INTO bucket3 VALUES ('jack', 15);
SELECT * FROM bucket3 ORDER BY name FORMAT CSV;
SELECT bucket_number FROM system.cnch_parts where database = currentDatabase() and table = 'bucket3' FORMAT CSV;

-- Ensure bucket number is assigned to a part in bucket table with shard ratio
INSERT INTO bucket_with_split_number VALUES ('vivek', 10);
SELECT * FROM bucket_with_split_number ORDER BY name FORMAT CSV;
SELECT bucket_number FROM system.cnch_parts where database = currentDatabase() and table = 'bucket_with_split_number' FORMAT CSV;
SELECT split_number, with_range FROM system.cnch_tables where database = currentDatabase() and name = 'bucket_with_split_number' FORMAT CSV;

-- Ensure bucket number is assigned to a part in bucket table with shard ratio and range
INSERT INTO bucket_with_split_number_n_range VALUES ('vivek', 20);
SELECT * FROM bucket_with_split_number_n_range ORDER BY name FORMAT CSV;
SELECT bucket_number FROM system.cnch_parts where database = currentDatabase() and table = 'bucket_with_split_number_n_range' FORMAT CSV;
SELECT split_number, with_range FROM system.cnch_tables where database = currentDatabase() and name = 'bucket_with_split_number_n_range' FORMAT CSV;

-- Ensure bucket number is assigned using DTSPartition with shard ratio and range
INSERT INTO dts_bucket_with_split_number_n_range VALUES ('vivek', 30);
SELECT * FROM dts_bucket_with_split_number_n_range ORDER BY name FORMAT CSV;
SELECT bucket_number FROM system.cnch_parts where database = currentDatabase() and table = 'dts_bucket_with_split_number_n_range' FORMAT CSV;
SELECT split_number, with_range FROM system.cnch_tables where database = currentDatabase() and name = 'dts_bucket_with_split_number_n_range' FORMAT CSV;

-- Ensure optimize_skip_unused_workers
INSERT INTO TABLE test_optimize select toUInt32(number/10), toUInt32(number/10), concat('record', toString(number)) from system.numbers limit 30;
SELECT * FROM  test_optimize where code = 2 ORDER BY record LIMIT 3 FORMAT CSV;
-- Apply optimization, note here will only check correctness. Integeration test framework should evaluate optimization in future.
SET optimize_skip_unused_shards = 1;
SELECT * FROM  test_optimize where code = 2 ORDER BY record LIMIT 3 FORMAT CSV;
-- Ensure that if we apply expression, the result is again same
SELECT * FROM test_optimize where code = toUInt32('2') ORDER BY record LIMIT 3 FORMAT CSV;
SELECT * FROM test_optimize where code in (0,2) ORDER BY record LIMIT 3 FORMAT CSV;
SET optimize_skip_unused_shards_limit = 1;
SELECT * FROM test_optimize where code in (0,2) ORDER BY record LIMIT 3 FORMAT CSV;
SET optimize_skip_unused_shards_limit = 1000;

-- Ensure other data type returns correct result with optimize_skip_unused_workers
INSERT INTO TABLE test_optimize_with_date_column SELECT toUInt32(number/10), (toUInt32(number/10)), toDate(number/10) FROM system.numbers LIMIT 30;
SET optimize_skip_unused_shards = 0;
SELECT * FROM test_optimize_with_date_column  WHERE record = '1970-01-02' ORDER BY id LIMIT 2 FORMAT CSV;
SET optimize_skip_unused_shards = 1;
SELECT * FROM test_optimize_with_date_column  WHERE record = '1970-01-02' ORDER BY id LIMIT 2 FORMAT CSV;

SET optimize_skip_unused_shards = 0;
SET enable_multiple_tables_for_cnch_parts = 0;
DROP TABLE bucket;
DROP TABLE bucket2;
DROP TABLE bucket3;
DROP TABLE normal;
DROP TABLE bucket_with_split_number;
DROP TABLE bucket_with_split_number_n_range;
DROP TABLE dts_bucket_with_split_number_n_range;
DROP TABLE test_optimize;
DROP TABLE test_optimize_with_date_column;
