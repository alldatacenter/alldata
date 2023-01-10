
DROP TABLE IF EXISTS hive_type_dec_test;

CREATE TABLE hive_type_dec_test
(
    id Nullable(Bigint),
    dec1 Nullable(Decimal(10,0)),
    dec2 Nullable(Decimal(38,18)),
    date String
)
ENGINE = CnchHive(`data.olap.cnch_hms.service.lf`, `cnch_hive_external_table`, `hive_type_dec_test`)
PARTITION BY (date);

SELECT dec1, count(dec1), dec2, count(dec2) FROM hive_type_dec_test GROUP BY dec1, dec2 ORDER BY dec1;

DROP TABLE IF EXISTS hive_type_dec_test;
