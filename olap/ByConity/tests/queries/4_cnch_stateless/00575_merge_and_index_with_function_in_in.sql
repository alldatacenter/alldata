
DROP TABLE IF EXISTS t;

create table t(d Date) engine CnchMergeTree() PARTITION BY toYYYYMM(d) ORDER BY d;

insert into t values ('2018-02-20');

select count() from t where toDayOfWeek(d) in (2);

DROP TABLE t;
