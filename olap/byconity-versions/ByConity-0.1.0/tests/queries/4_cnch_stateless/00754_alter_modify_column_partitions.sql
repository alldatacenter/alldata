-- check ALTER MODIFY COLUMN with partitions

SET send_logs_level = 'none';
DROP TABLE IF EXISTS alter_column;
CREATE TABLE alter_column(x UInt32, y Int32) ENGINE CnchMergeTree PARTITION BY x ORDER BY x;
INSERT INTO alter_column (x, y) SELECT number AS x, -number AS y FROM system.numbers LIMIT 50;
SELECT '*** Check SHOW CREATE TABLE ***';
SET show_table_uuid_in_table_create_query_if_not_nil=0;
SHOW CREATE TABLE alter_column;
SELECT '*** Check parts ***';
SELECT * FROM alter_column ORDER BY _part;
ALTER TABLE alter_column MODIFY COLUMN y Int64;
SELECT '*** Check SHOW CREATE TABLE after ALTER MODIFY ***';
SET show_table_uuid_in_table_create_query_if_not_nil=0;
SHOW CREATE TABLE alter_column;
SELECT '*** Check parts after ALTER MODIFY ***';
SELECT * FROM alter_column ORDER BY _part;
DROP TABLE alter_column;
