DROP TABLE IF EXISTS non_indexed_table;
CREATE TABLE non_indexed_table (d Date, x UInt64, y String, z String) ENGINE = CnchMergeTree ORDER BY tuple() PARTITION BY d SETTINGS index_granularity = 1;
SYSTEM START MERGES non_indexed_table;

INSERT INTO non_indexed_table SELECT toDate('2019-05-27'), number, toString(number), toString(number) FROM numbers(10);

ALTER TABLE non_indexed_table ADD INDEX minmax_x x TYPE minmax GRANULARITY 1;
ALTER TABLE non_indexed_table MATERIALIZE INDEX minmax_x;
SELECT sleep(3) FORMAT Null;
SELECT count() FROM non_indexed_table WHERE x = 1 SETTINGS max_rows_to_read = 1;
