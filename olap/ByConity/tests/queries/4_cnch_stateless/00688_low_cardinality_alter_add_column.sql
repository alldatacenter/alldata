
drop table if exists cardinality;
create table cardinality (x String) engine = CnchMergeTree order by tuple();
insert into cardinality (x) select concat('v', toString(number)) from numbers(10);
alter table cardinality add column y LowCardinality(String);
select * from cardinality;
drop table if exists cardinality;
