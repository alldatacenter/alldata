DROP TABLE IF EXISTS union_test;

CREATE table union_test
(
    id int,
    dname String,
    score float,
    date String
)
ENGINE = CnchHive(`data.olap.cnch_hms.service.lf`, `cnch_hive_external_table`, `join_test_left`)
PARTITION by date;

select * from union_test where date = '20011211' order by date union all select * from union_test where date = '20011211' order by date;

DROP TABLE IF EXISTS union_test;
