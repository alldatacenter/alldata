DROP TABLE IF EXISTS test.hive_external_table_3_06;

CREATE TABLE test.hive_external_table_3_06 (
    `app_id` Int64,
    `action_type` Nullable(String),
    `commodity_id` Int32,
    `date` String,
    `live_id` Int64,
    `app_name` String
    )
ENGINE = CnchHive(`thrift://10.1.1.1:9301`, `cnchhive_ci`, `hive_external_table_3`)
PARTITION BY (date, live_id, app_name);

SELECT * FROM test.hive_external_table_3_06 WHERE (toDate(date) >= '2021-10-13') AND (toDate(date) <= '2021-10-14') AND (app_name IN ('test4')) ORDER BY app_id ASC;
SELECT * FROM test.hive_external_table_3_06 WHERE (toDate(date) >= '2021-10-13') AND (toDate(date) <= '2021-10-14') ORDER BY app_id ASC;

SELECT * FROM test.hive_external_table_3_06 WHERE (toDate(date) >= '2021-10-13') AND (toDate(date) <= '2021-10-17') AND (app_name IN ('test1','test4')) ORDER BY app_id;

DROP TABLE IF EXISTS test.hive_external_table_3_06;
