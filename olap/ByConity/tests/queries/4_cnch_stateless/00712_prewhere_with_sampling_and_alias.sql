drop table if exists t;
create table t (date Date, counter UInt64, sampler UInt64, alias_col alias date + 1) engine = CnchMergeTree() PARTITION BY toYYYYMM(date) SAMPLE BY intHash32(sampler) ORDER BY (counter, date, intHash32(sampler));
insert into t values ('2018-01-01', 1, 1);
select alias_col from t sample 1 / 2 where date = '2018-01-01' and counter = 1 and sampler = 1;
drop table if exists t;

