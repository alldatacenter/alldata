DROP TABLE IF EXISTS bucket_int64_test;

CREATE TABLE bucket_int64_test
(
    id Bigint,
    name String,
    device_id Bigint,
    p Bigint
)ENGINE = CnchHive(`data.olap.cnch_hms.service.lf`,`cnch_hive_external_table`,`bucket_int64_test`)
PARTITION BY (p)
CLUSTER BY device_id INTO 5 BUCKETS;

select * from bucket_int64_test where p = 1 AND device_id = 4209368873053927;

DROP TABLE IF EXISTS bucket_int64_test;
