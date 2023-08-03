CREATE DATABASE IF NOT EXISTS test;
DROP TABLE IF EXISTS test.bubble_v3;
CREATE TABLE test.bubble_v3 (`action_type` String, `group_id` Int64) ENGINE = CnchMergeTree() ORDER BY (action_type, group_id) SETTINGS index_granularity = 2;
INSERT INTO test.bubble_v3 SELECT multiIf(number < 5, '1', number < 10, '2', number < 15, '3', '4'), number FROM numbers(20);
-- read exactly 1 mark
SELECT count() FROM test.bubble_v3 WHERE (group_id = 17) AND (action_type IN ('4')) SETTINGS max_rows_to_read = 2;
DROP TABLE test.bubble_v3;