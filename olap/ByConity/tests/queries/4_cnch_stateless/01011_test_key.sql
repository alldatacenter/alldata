drop table if exists test.test_key;

CREATE TABLE test.test_key (`p_date` Date, `id` Int32, `did` Int32, `cid` Int32, `event` String, `name` String) ENGINE = CnchMergeTree PARTITION BY p_date ORDER BY id SETTINGS index_granularity = 8192;

insert into test.test_key values ('2021-01-01', 1, 1, 1, 'a', 'a')
insert into test.test_key values ('2021-01-01', 2, 2, 2, 'a', 'a')
insert into test.test_key values ('2021-01-01', 3, 3, 3, 'a', 'a')

set enable_optimizer = 1;

select * from test.test_key as a join test.test_key as b on a.id = b.id and a.did = b.did and a.cid = b.cid order by a.id;
select * from test.test_key as a join test.test_key as b on a.id = b.id and a.did = b.did and a.id = b.cid order by a.id;
select * from test.test_key as a join test.test_key as b on a.id = b.id and a.cid = b.did and a.did = b.cid order by a.id;

select * from test.test_key as a join test.test_key as b on a.id = b.id and a.id = b.did and a.id = b.cid order by a.id;
select a.id, count() from test.test_key as a join test.test_key as b on a.id = b.id and a.id = b.did and a.id = b.cid group by a.id order by a.id;
select a.id, a.id, count() from test.test_key as a join test.test_key as b on a.id = b.id and a.id = b.did and a.id = b.cid group by a.id, a.id order by a.id;

select a.id, b.id, count() from test.test_key as a join test.test_key as b on a.id = b.id and a.id = b.did and a.id = b.cid group by a.id, b.id order by a.id;
select a.id, b.id, b.cid, count() from test.test_key as a join test.test_key as b on a.id = b.id and a.id = b.did and a.id = b.cid group by a.id, b.id, b.cid order by a.id;

drop table if exists test.test_key;