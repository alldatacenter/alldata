DROP TABLE IF EXISTS hive_external_table_5;
CREATE  TABLE hive_external_table_5
(
    id Nullable(Bigint),
    value Array(String),
    zset Map(String, double),
    date String
)
ENGINE = CnchHive(`data.olap.cnch_hms.service.lf`, `cnch_hive_external_table`, `hive_external_table_map`)
PARTITION BY (date);

SELECT * FROM hive_external_table_5 where date > '20211013' and date < '20211016' ORDER BY id, value;

SELECT zset{'sdbfr'} from hive_external_table_5 order by id, value;

DROP TABLE IF EXISTS hive_external_table_5;
