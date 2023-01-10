DROP TABLE IF EXISTS hive_type_char_test;

CREATE  TABLE hive_type_char_test
(
    id Nullable(Bigint),
    name Nullable(String),
    ch Nullable(FixedString(10)),
    date String
)
ENGINE = CnchHive(`data.olap.cnch_hms.service.lf`, `cnch_hive_external_table`, `hive_type_char_test`)
PARTITION BY (date);

SELECT * FROM hive_type_char_test ORDER BY id, name;

DROP TABLE IF EXISTS hive_type_char_test;
