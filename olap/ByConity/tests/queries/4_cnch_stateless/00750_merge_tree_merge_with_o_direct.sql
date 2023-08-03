
DROP TABLE IF EXISTS sample_merge_tree;
CREATE TABLE sample_merge_tree (dt DateTime, x UInt64) ENGINE = CnchMergeTree PARTITION BY toYYYYMMDD(dt) ORDER BY x SETTINGS min_merge_bytes_to_use_direct_io=1, index_granularity = 8192;
INSERT INTO sample_merge_tree VALUES (toDateTime('2018-10-31 05:05:00'), 0), (toDateTime('2018-10-31 06:06:00'), 10), (toDateTime('2018-10-28 10:00:00'), 20);
SYSTEM START MERGES sample_merge_tree;
OPTIMIZE TABLE sample_merge_tree SETTINGS mutations_sync = 1;
SELECT * FROM sample_merge_tree ORDER BY x;
DROP TABLE IF EXISTS sample_merge_tree;
