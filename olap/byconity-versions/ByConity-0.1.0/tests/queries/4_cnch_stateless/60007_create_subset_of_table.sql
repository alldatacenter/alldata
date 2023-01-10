DROP TABLE IF EXISTS hive_external_table_3_05;
CREATE TABLE hive_external_table_3_05
(
    app_id Nullable(Bigint),
    date String,
    live_id Bigint,
    app_name String
)
ENGINE = CnchHive(`data.olap.cnch_hms.service.lf`, `cnch_hive_external_table`, `hive_external_table_test`)
PARTITION BY (date, live_id, app_name);

SELECT *  FROM hive_external_table_3_05 order by app_id, app_name;

DROP TABLE IF EXISTS hive_external_table_3_05;
