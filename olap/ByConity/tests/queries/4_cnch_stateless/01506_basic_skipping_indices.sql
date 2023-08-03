
CREATE TABLE basic_index (
    `x` UInt64,
    `y` String,
    `z` String,
    INDEX minmax_x x TYPE minmax GRANULARITY 1,
    INDEX set_y y TYPE set(1) GRANULARITY 1,
    INDEX bloom_z z TYPE bloom_filter GRANULARITY 1
) ENGINE = CnchMergeTree ORDER BY tuple() SETTINGS index_granularity = 1;

INSERT INTO basic_index SELECT number, toString(number), toString(number) FROM numbers(10);

SELECT count() FROM basic_index WHERE x = 1 SETTINGS max_rows_to_read = 1;
SELECT count() FROM basic_index WHERE x > 5 SETTINGS max_rows_to_read = 4;
SELECT count() FROM basic_index WHERE y = '1' SETTINGS max_rows_to_read = 1;
SELECT count() FROM basic_index WHERE y IN ('1', '2', '3') SETTINGS max_rows_to_read = 3;
SELECT count() from basic_index WHERE y > '5' SETTINGS max_rows_to_read = 4;
SELECT count() From basic_index WHERE z = '1' SETTINGS force_data_skipping_indices = 'bloom_z';
SELECT count() FROM basic_index WHERE z IN ('1', '2', '3') SETTINGS force_data_skipping_indices = 'bloom_z';
