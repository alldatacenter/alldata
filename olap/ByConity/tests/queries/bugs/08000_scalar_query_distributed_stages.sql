drop table if exists test.test_scalar;
drop table if exists test.test_scalar_local;

create table test.test_scalar_local (p_date Date, id Int32, event String) engine = MergeTree partition by p_date order by id;
create table test.test_scalar as test.test_scalar_local engine = Distributed(test_shard_localhost, test, test_scalar_local, rand());

set enable_distributed_stages = 1;

select id from test.test_scalar limit 10;
select id from test.test_scalar order by id limit 10;

insert into test.test_scalar_local select '2022-01-01', number, 'a' from numbers(3);

select id - 1 from test.test_scalar order by id;
select id * 1 from test.test_scalar order by id;
select toString(id * 1) from test.test_scalar order by id;
select toString(id * 1) as a from test.test_scalar order by id;

select id as a, a + 1 as b, b * 1 as c from test.test_scalar order by id;

drop table if exists test.test_scalar;
drop table if exists test.test_scalar_local;