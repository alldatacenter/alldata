DROP TABLE IF EXISTS bucket_uint8_test;
CREATE TABLE bucket_uint8_test
(
    id Bigint,
    name String,
    device_id tinyint,
    p Bigint
)ENGINE = CnchHive(`data.olap.cnch_hms.service.lf`,`cnch_hive_external_table`,`bucket_uint8_test`)
PARTITION BY (p)
CLUSTER BY device_id INTO 5 BUCKETS;
select * from bucket_uint8_test where p = 1 AND device_id = 3;
DROP TABLE IF EXISTS bucket_uint8_test;
