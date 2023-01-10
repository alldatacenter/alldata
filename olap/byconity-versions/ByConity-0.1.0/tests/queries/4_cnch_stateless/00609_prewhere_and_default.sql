
drop table if exists prewhere_default;
create table prewhere_default (key UInt64, val UInt64) engine = CnchMergeTree order by key settings index_granularity=8192;
insert into prewhere_default select number, number / 8192 from system.numbers limit 100000;
alter table prewhere_default add column def UInt64 default val + 1;
select * from prewhere_default prewhere val > 2 format Null;

drop table if exists prewhere_default;
create table prewhere_default (key UInt64, val UInt64) engine = CnchMergeTree order by key settings index_granularity=8192;
insert into prewhere_default select number, number / 8192 from system.numbers limit 100000;
alter table prewhere_default add column def UInt64;
select * from prewhere_default prewhere val > 2 format Null;

