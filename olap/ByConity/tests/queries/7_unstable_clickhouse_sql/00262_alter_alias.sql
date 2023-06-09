USE test;
drop table if exists test.aliases_test;

create table test.aliases_test (date default today(), id default rand(), array default [0, 1, 2]) engine=CnchMergeTree() PARTITION BY toYYYYMM(date) ORDER BY id SETTINGS index_granularity=1;

insert into test.aliases_test (id) values (0);
select array from test.aliases_test;

alter table test.aliases_test modify column array alias [0, 1, 2];
select array from test.aliases_test;

alter table test.aliases_test modify column array default [0, 1, 2];
select array from test.aliases_test;

alter table test.aliases_test add column struct.k Array(UInt8) default [0, 1, 2], add column struct.v Array(UInt8) default array;
select struct.k, struct.v from test.aliases_test;

alter table test.aliases_test modify column struct.v alias array;
select struct.k, struct.v from test.aliases_test;

select struct.k, struct.v from test.aliases_test array join struct;
select struct.k, struct.v from test.aliases_test array join struct as struct;
select class.k, class.v from test.aliases_test array join struct as class;

drop table test.aliases_test;
