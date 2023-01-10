drop table if exists test.test_union;
drop table if exists test.test_union_local;

create table test.test_union_local (p_date Date, id Int32, event String) engine = MergeTree partition by p_date order by id;
create table test.test_union as test.test_union_local engine = Distributed(test_shard_localhost, test, test_union_local, rand());

set enable_distributed_stages = 1;
--set exchange_enable_force_remote_mode = 1;
set send_plan_segment_by_brpc = 1;

select id from test.test_union limit 10;
select id from test.test_union order by id limit 10;

insert into test.test_union_local select '2022-01-01', number, 'a' from numbers(3);

select * from test.test_union
    union all
select * from test.test_union;

select * from (select * from test.test_union)
    union all
    (select * from test.test_union);

select * from (select a.id from test.test_union as a join test.test_union as b on a.id = b.id)
    union all
    (select id from test.test_union);

select * from (select a.id from test.test_union as a join test.test_union as b on a.id = b.id)
    union all
    (select a.id from test.test_union as a join test.test_union as b on a.id = b.id);

select * from (select * from (select a.id from test.test_union as a join test.test_union as b on a.id = b.id)
    union all
    (select a.id from test.test_union as a join test.test_union as b on a.id = b.id)) as c join test.test_union as d on c.id = d.id;

select j from (select i as j from (select a.id as i from test.test_union as a join test.test_union as b on a.id = b.id)
    union all
    (select a.id from test.test_union as a join test.test_union as b on a.id = b.id)) as c join test.test_union as d on c.j = d.id;

drop table if exists test.test_union;
drop table if exists test.test_union_local;