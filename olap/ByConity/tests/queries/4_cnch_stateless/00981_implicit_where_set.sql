
drop table if exists test_eliminate;

create table test_eliminate (date Date, id Int32, event String) engine = CnchMergeTree partition by toYYYYMM(date) order by id;

insert into test_eliminate values ('2020-01-01', 1, 'a');
insert into test_eliminate values ('2020-01-01', 1, 'b');
insert into test_eliminate values ('2020-01-02', 2, 'c');

select count() from test_eliminate where date in '2020-01-01' settings enable_partition_prune = 0;
select count() from test_eliminate where date in '2020-01-01' settings enable_partition_prune = 1;
select id, count() from test_eliminate where date in '2020-01-01' group by id order by id settings enable_partition_prune = 0;
select id, count() from test_eliminate where date in '2020-01-01' group by id order by id settings enable_partition_prune = 1;
select id from test_eliminate where date in '2020-01-01' order by id settings enable_partition_prune = 0;
select id from test_eliminate where date in '2020-01-01' order by id settings enable_partition_prune = 1;

drop table if exists test_eliminate;
