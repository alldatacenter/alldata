USE test;
DROP TABLE IF EXISTS numbers_10k_log;
SET max_block_size = 1000;
CREATE TABLE numbers_10k_log ENGINE = CnchMergeTree ORDER by number
AS SELECT number FROM system.numbers LIMIT 10000;
SET max_threads = 4;
SET max_rows_to_group_by = 3000, group_by_overflow_mode = 'any';
SELECT ignore(rand()), ignore(max(toString(number))) FROM numbers_10k_log GROUP BY rand() LIMIT 1;
DROP TABLE numbers_10k_log;
