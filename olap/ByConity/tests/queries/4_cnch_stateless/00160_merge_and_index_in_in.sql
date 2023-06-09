DROP TABLE IF EXISTS test.mt;

CREATE TABLE test.mt (d Date DEFAULT toDate('2015-05-01'), x UInt64) ENGINE = CnchMergeTree() PARTITION BY toYYYYMM(d) ORDER BY x SETTINGS index_granularity = 1;

SET min_insert_block_size_rows = 0, min_insert_block_size_bytes = 0;
SET max_block_size = 1000000;
INSERT INTO test.mt (x) SELECT number AS x FROM system.numbers LIMIT 100000;

SELECT *, blockSize() < 10 FROM test.mt WHERE x IN (12345, 67890) AND NOT ignore(blockSize() < 10) ORDER BY x;

DROP TABLE test.mt;

CREATE TABLE test.mt (d Date DEFAULT toDate('2015-05-01'), x UInt64, y UInt64, z UInt64) ENGINE = CnchMergeTree() PARTITION BY toYYYYMM(d) ORDER BY (x, z) SETTINGS index_granularity = 1;

INSERT INTO test.mt (x, y, z) SELECT number AS x, number + 10 AS y, number / 2 AS z FROM system.numbers LIMIT 100000;

SELECT *, blockSize() < 10 FROM test.mt WHERE (z, y, x) IN ((617, 1244, 1234), (2839, 5688, 5678), (1,1,1)) AND NOT ignore(blockSize() < 10) ORDER BY (x, y, z);

DROP TABLE test.mt;
