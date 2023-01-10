DROP TABLE IF EXISTS test.hive_external_table_3_07;
CREATE TABLE test.hive_external_table_3_07
(
    app_id Bigint,
    action_type Nullable(String),
    commodity_id int,
    date String,
    live_id Bigint,
    app_name String
)
ENGINE = CnchHive(`thrift://10.1.1.1:9301`, `cnchhive_ci`, `hive_external_table_3`)
PARTITION BY (date, live_id, app_name);

SELECT * FROM test.hive_external_table_3_07 WHERE app_name IN ('test4') ORDER BY app_id;
SELECT * FROM test.hive_external_table_3_07 WHERE app_name IN ('test2', 'test4') ORDER BY app_id;
SELECT * FROM test.hive_external_table_3_07 WHERE app_name IN ('test4') AND live_id = 50 AND date = '20211014' ORDER BY app_id;
SELECT * FROM test.hive_external_table_3_07 WHERE app_name IN ('test2', 'test4') AND live_id = 30 AND date = '20211016' ORDER BY app_id;
SELECT * FROM test.hive_external_table_3_07 WHERE app_name IN ('test2', 'test4') AND date = '20211014' AND commodity_id IN (2, 4) ORDER BY app_id;
SELECT * FROM test.hive_external_table_3_07 WHERE app_name IN ('test2') AND date = '20211016' AND commodity_id IN (2, 4) ORDER BY app_id;

SELECT 'NOT IN test';

SELECT * from test.hive_external_table_3_07 where app_name NOT IN('test', 'test1') AND live_id = 40 ORDER BY app_id;

SELECT 'Like Function test';

SELECT * from test.hive_external_table_3_07 where app_name LIKE('%test1') AND commodity_id = 2 ORDER BY app_id;
SELECT * from test.hive_external_table_3_07 where app_name NOT LIKE('%test1') AND commodity_id = 2 AND live_id = 50 ORDER BY app_id;

DROP TABLE IF EXISTS test.hive_external_table_3_07;
