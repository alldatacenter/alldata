DROP TABLE IF EXISTS hive_bucket_test;
CREATE TABLE hive_bucket_test
(
    id int,
    name String,
    date String
)ENGINE = CnchHive(`data.olap.cnch_hms.service.lf`, `cnch_hive_external_table`, `hive_bucket_test`)
PARTITION BY (date)
CLUSTER BY id INTO 4 BUCKETS
ORDER BY name;

/* id = 1 will purn useless parts */
SELECT * FROM hive_bucket_test WHERE id = 1 ORDER BY name;

/* this case cann't purn parts */
SELECT * FROM hive_bucket_test WHERE id >= 1 AND id <= 3 ORDER BY name;

DROP TABLE IF EXISTS hive_bucket_test;
