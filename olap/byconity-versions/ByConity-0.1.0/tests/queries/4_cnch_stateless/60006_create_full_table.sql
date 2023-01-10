DROP TABLE IF EXISTS hive_external_table_3_04;
CREATE TABLE hive_external_table_3_04
(
    app_id Nullable(Bigint),
    action_type Nullable(String),
    commodity_id Nullable(int),
    device_id Nullable(String),
    platfrom_name Nullable(String),
    date String,
    live_id Bigint,
    app_name String
)
ENGINE = CnchHive(`data.olap.cnch_hms.service.lf`, `cnch_hive_external_table`, `hive_external_table_test`)
PARTITION BY (date, live_id, app_name);

SELECT *  FROM hive_external_table_3_04 order by app_id, commodity_id, app_name;

DROP TABLE IF EXISTS hive_external_table_3_04;
