DROP TABLE IF EXISTS join_test_left;

CREATE table join_test_left
(
    id int,
    dname String,
    score float,
    date String
)
ENGINE = CnchHive(`data.olap.cnch_hms.service.lf`, `cnch_hive_external_table`, `join_test_left`)
PARTITION by date;


DROP TABLE IF EXISTS join_test_right;
CREATE table join_test_right
(
    id int,
    dname String,
    web String,
    develop String,
    date    String
)
ENGINE = CnchHive(`data.olap.cnch_hms.service.lf`, `cnch_hive_external_table`, `join_test_right`)
PARTITION by date;

select * from join_test_left l  all inner join  join_test_right r  on l.id = r.id order by l.id ;


DROP TABLE IF EXISTS join_test_left;
DROP TABLE IF EXISTS join_test_right;

