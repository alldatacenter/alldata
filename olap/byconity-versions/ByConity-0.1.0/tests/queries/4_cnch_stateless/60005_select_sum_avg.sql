DROP TABLE IF EXISTS hive_external_table_3_03;
CREATE TABLE hive_external_table_3_03
(
    app_id Bigint,
    action_type Nullable(String),
    commodity_id int,
    date String,
    live_id Bigint,
    app_name String
)
ENGINE = CnchHive(`data.olap.cnch_hms.service.lf`, `cnch_hive_external_table`, `hive_external_table_test`)
PARTITION BY (date, live_id, app_name);

SELECT sum(app_id), count(*), avg(commodity_id) FROM hive_external_table_3_03;

DROP TABLE IF EXISTS hive_external_table_3_03;
