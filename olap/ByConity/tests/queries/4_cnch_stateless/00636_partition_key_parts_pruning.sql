SELECT '*** Single column partition key ***';

DROP TABLE IF EXISTS single_col_partition_key;
CREATE TABLE single_col_partition_key(x UInt32) ENGINE CnchMergeTree ORDER BY x PARTITION BY intDiv(x, 10) SETTINGS index_granularity=1;

SET force_index_by_date = 1;

INSERT INTO single_col_partition_key VALUES (1), (2), (3), (4), (11), (12), (20);

SELECT count() FROM single_col_partition_key WHERE x < 3 SETTINGS max_rows_to_read = 4;
SELECT count() FROM single_col_partition_key WHERE x >= 11 SETTINGS max_rows_to_read = 3;
SELECT count() FROM single_col_partition_key WHERE x = 20 SETTINGS max_rows_to_read = 1;

DROP TABLE single_col_partition_key;

SELECT '*** Composite partition key ***';

DROP TABLE IF EXISTS composite_partition_key;
CREATE TABLE composite_partition_key(a UInt32, b UInt32, c UInt32) ENGINE CnchMergeTree ORDER BY c PARTITION BY (intDiv(a, 100), intDiv(b, 10), c) SETTINGS index_granularity=1;

INSERT INTO composite_partition_key VALUES (1, 1, 1), (2, 2, 1), (3, 3, 1);
INSERT INTO composite_partition_key VALUES (100, 10, 2), (101, 11, 2), (102, 12, 2);
INSERT INTO composite_partition_key VALUES (200, 10, 2), (201, 11, 2), (202, 12, 2);
INSERT INTO composite_partition_key VALUES (301, 20, 3), (302, 21, 3), (303, 22, 3);

SELECT count() FROM composite_partition_key WHERE a > 400 SETTINGS max_rows_to_read = 1, max_bytes_to_read = 1;
SELECT count() FROM composite_partition_key WHERE b = 11 SETTINGS max_rows_to_read = 6;
SELECT count() FROM composite_partition_key WHERE c = 4 SETTINGS max_rows_to_read = 0, max_bytes_to_read = 1;

SELECT count() FROM composite_partition_key WHERE a < 200 AND c = 2 SETTINGS max_rows_to_read = 3;
SELECT count() FROM composite_partition_key WHERE a = 301 AND b < 20 SETTINGS max_rows_to_read = 0, max_bytes_to_read = 1;
SELECT count() FROM composite_partition_key WHERE b >= 12 AND c = 2 SETTINGS max_rows_to_read = 6;

SELECT count() FROM composite_partition_key WHERE a = 301 AND b = 21 AND c = 3 SETTINGS max_rows_to_read = 3;

DROP TABLE composite_partition_key;
