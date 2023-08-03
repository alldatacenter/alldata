drop table if exists test.test_multi_join;
drop table if exists test.test_multi_join_local;

create table test.test_multi_join_local (p_date Date, id Int32, event String) engine = MergeTree partition by p_date order by id;
create table test.test_multi_join as test.test_multi_join_local engine = Distributed(test_shard_localhost, test, test_multi_join_local, rand());

set enable_distributed_stages = 1;
set exchange_enable_force_remote_mode = 1;
set send_plan_segment_by_brpc = 1;

select id from test.test_multi_join limit 10;
select id from test.test_multi_join order by id limit 10;

insert into test.test_multi_join_local select '2022-01-01', number, 'a' from numbers(3);

select a.id from test.test_multi_join as a join test.test_multi_join as b on a.id = b.id join test.test_multi_join as c on a.id = c.id order by a.id;
select * from test.test_multi_join as a join test.test_multi_join as b on a.id = b.id join test.test_multi_join as c on a.id = c.id order by a.id;

select a.id from (select * from test.test_multi_join) as a
    join
    (select * from test.test_multi_join) as b on a.id = b.id
    join
    (select * from test.test_multi_join) as c on a.id = c.id order by a.id;

select * from (select * from test.test_multi_join) as a
    join
    (select * from test.test_multi_join) as b on a.id = b.id
    join
    (select * from test.test_multi_join) as c on a.id = c.id order by a.id;

select i, j, k from (select id as i from test.test_multi_join) as a
    join
    (select id as j from test.test_multi_join) as b on a.i = b.j
    join
    (select id as k from test.test_multi_join) as c on a.i = c.k order by i;

drop table if exists test.test_multi_join;
drop table if exists test.test_multi_join_local;