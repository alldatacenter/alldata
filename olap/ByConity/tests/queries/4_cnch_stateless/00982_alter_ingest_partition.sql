DROP TABLE IF EXISTS test_ingest_partition_target;
DROP TABLE IF EXISTS test_ingest_partition_source;

CREATE TABLE test_ingest_partition_target (`p_date` Date, `id` Int32, `c1` String, `c2` String) ENGINE = CnchMergeTree PARTITION BY p_date ORDER BY id SETTINGS index_granularity = 8192;
CREATE TABLE test_ingest_partition_source (`p_date` Date, `id` Int32, `c1` String, `c2` String) ENGINE = CnchMergeTree PARTITION BY p_date ORDER BY id SETTINGS index_granularity = 8192;

SYSTEM STOP MERGES test_ingest_partition_target;

INSERT INTO test_ingest_partition_target SELECT '2021-01-01', number, 'a', 'b' from numbers(10);
INSERT INTO test_ingest_partition_source SELECT '2021-01-01', number, 'e', 'f' from numbers(10);
SELECT * FROM test_ingest_partition_target ORDER BY id;

ALTER TABLE test_ingest_partition_target ingest partition '2021-01-01' columns c1 key id from test_ingest_partition_source;
SELECT sleep(1.1) format Null;

SELECT * FROM test_ingest_partition_target order by id;

DROP TABLE IF EXISTS test_ingest_partition_target;
DROP TABLE IF EXISTS test_ingest_partition_source;


CREATE TABLE test_ingest_partition_target (p_date Date, id1 Int32, id2 Int32, c1 String, c2 String, c3 String) ENGINE = CnchMergeTree PARTITION BY p_date ORDER BY (id1, id2) SETTINGS index_granularity = 8192;
CREATE TABLE test_ingest_partition_source (p_date Date, id1 Int32, id2 Int32, c1 String, c2 String) ENGINE = CnchMergeTree PARTITION BY p_date ORDER BY (id1, id2) SETTINGS index_granularity = 8192;

SYSTEM STOP MERGES test_ingest_partition_target;
INSERT INTO test_ingest_partition_target VALUES ('2010-01-01', 1, 2, 'a', 'b', 'c');
INSERT INTO test_ingest_partition_source VALUES ('2010-01-01', 1, 2, 'e', 'h'), ('2010-01-01', 1, 3, 'e', 'h');
ALTER TABLE test_ingest_partition_target INGEST PARTITION '2010-01-01'     COLUMNS c1, c2     KEY id1, id2 FROM test_ingest_partition_source;

SELECT sleep(1.1) format Null;

SELECT * FROM test_ingest_partition_target ORDER BY id1 ASC, id2 ASC;

DROP TABLE IF EXISTS test_ingest_partition_target;
DROP TABLE IF EXISTS test_ingest_partition_source;

-- Test the case when target table has no part
CREATE TABLE test_ingest_partition_target (p_date Date, id1 Int32, id2 Int32, c1 String, c2 String) ENGINE = CnchMergeTree PARTITION BY p_date ORDER BY (id1, id2) SETTINGS index_granularity = 8192;
CREATE TABLE test_ingest_partition_source (p_date Date, id1 Int32, id2 Int32, c1 String, c2 String) ENGINE = CnchMergeTree PARTITION BY p_date ORDER BY (id1, id2) SETTINGS index_granularity = 8192;

INSERT INTO test_ingest_partition_source VALUES ('2010-01-01', 1, 2, 'e', 'h'), ('2010-01-01', 1, 3, 'e', 'h');
ALTER TABLE test_ingest_partition_target INGEST PARTITION '2010-01-01'     COLUMNS c1, c2     KEY id1, id2 FROM test_ingest_partition_source;
SELECT sleep(1.1) format Null;

SELECT '---';
SELECT * FROM test_ingest_partition_source ORDER BY id1 ASC, id2 ASC;
SELECT '---';
SELECT * FROM test_ingest_partition_target ORDER BY id1 ASC, id2 ASC;
SELECT '---';
DROP TABLE IF EXISTS test_ingest_partition_target;
DROP TABLE IF EXISTS test_ingest_partition_source;

CREATE TABLE test_ingest_partition_target (`date` Date, `id` Int32, `name` Map(String, String)) ENGINE = CnchMergeTree PARTITION BY date ORDER BY id;
CREATE TABLE test_ingest_partition_source (`date` Date, `id` Int32, `name` Map(String, String)) ENGINE = CnchMergeTree PARTITION BY date ORDER BY id;

SYSTEM STOP MERGES test_ingest_partition_target;

INSERT INTO test_ingest_partition_source VALUES ('2020-01-01', 1, {'key1': 'val1', 'key2': 'val2', 'key3': 'val3'}), ('2020-01-01', 2, {'key4': 'val4', 'key5': 'val5', 'key6': 'val6'}), ('2020-01-01', 3, {'key7': 'val7', 'key8': 'val8', 'key9': 'val9'}), ('2020-01-01', 4, {'key10': 'val10', 'key11': 'val11', 'key12': 'val12'});

INSERT INTO test_ingest_partition_target VALUES ('2020-01-01', 1, {'key13': 'val13'});
ALTER TABLE test_ingest_partition_target INGEST PARTITION '2020-01-01'  COLUMNS name{'key1'}, name{'key2'}, name{'key3'}, name{'key4'}, name{'key5'}, name{'key6'}, name{'key7'}, name{'key8'}, name{'key9'}, name{'key10'} FROM test_ingest_partition_source;
SELECT sleep(1.1) format Null;
SELECT * FROM test_ingest_partition_target ORDER BY id;

DROP TABLE IF EXISTS test_ingest_partition_target;
DROP TABLE IF EXISTS test_ingest_partition_source;
