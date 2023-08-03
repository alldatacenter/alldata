
drop table if exists test_sample;

create table if not exists test_sample (date Date, id Int32) engine = CnchMergeTree partition by date order by id sample by id;

insert into table test_sample values ('2019-01-01', 1);
insert into table test_sample values ('2019-01-01', 2);
insert into table test_sample values ('2019-01-01', 3);
insert into table test_sample values ('2019-01-01', 4);
insert into table test_sample values ('2019-01-01', 5);
insert into table test_sample values ('2019-01-01', 6);
insert into table test_sample values ('2019-01-01', 7);
insert into table test_sample values ('2019-01-01', 8);
insert into table test_sample values ('2019-01-01', 9);
insert into table test_sample values ('2019-01-01', 10);

set enable_final_sample = 1;

select count() from test_sample sample 10;
select count() from test_sample sample 10;
select count() from test_sample sample 10;
select count() from test_sample sample 10;
select count() from test_sample sample 5;

select id from test_sample sample 5 order by id limit 1;
select id from test_sample sample 5 order by id limit 1;
select id from test_sample sample 5 order by id limit 1;
select id from test_sample sample 5 order by id limit 1;
select id from test_sample sample 10 order by id limit 1;

drop table if exists test_sample;