DROP TABLE IF EXISTS hive_external_table_3_01;
CREATE TABLE hive_external_table_3_01
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

SELECT * FROM hive_external_table_3_01 where date >= '20211013' order by app_id, commodity_id, app_name;

SELECT action_type AS Carrier, avg(app_id) AS c3 FROM hive_external_table_3_01 WHERE date >= '20211013' AND date <= '20211016' GROUP BY Carrier ORDER BY c3 DESC;

SELECT app_id, count(*) AS c FROM hive_external_table_3_01 WHERE date != '20211013' GROUP BY app_id ORDER BY c  DESC, app_id asc;

SELECT count(*) FROM hive_external_table_3_01 WHERE app_id != 0;

DROP TABLE IF EXISTS hive_external_table_3_01;
