create database if not exists test;
drop table if exists test.test_bitmap;

create table if not exists test.test_bitmap (tag String, uids BitMap64, p_date Date) engine = CnchMergeTree() order by tag;

---- format Value
insert into test.test_bitmap values ('v0', [], '2021-01-01');
insert into test.test_bitmap values ('v1', [,], '2021-01-01');
insert into test.test_bitmap values ('v2', [,,], '2021-01-01');
insert into test.test_bitmap values ('v3', [0,,], '2021-01-01');
insert into test.test_bitmap values ('v4', [,0,], '2021-01-01');
insert into test.test_bitmap values ('v5', [,,0], '2021-01-01');
insert into test.test_bitmap values ('v6', [, 0 ,0 ], '2021-01-01');
insert into test.test_bitmap values ('v7', [0,1,2], '2021-01-01');
insert into test.test_bitmap values ('v8', '[0,1,2]', '2021-01-01');
insert into test.test_bitmap values ('v9', "[0,1,2]", '2021-01-01');

select * from test.test_bitmap order by tag;

---- format CSV
truncate table test.test_bitmap;
insert into test.test_bitmap format CSV 'c0', [], '2021-01-01'
insert into test.test_bitmap format CSV 'c1', [,], '2021-01-01'
insert into test.test_bitmap format CSV 'c2', [,,], '2021-01-01'
insert into test.test_bitmap format CSV 'c3', [0,,], '2021-01-01'
insert into test.test_bitmap format CSV 'c4', [,0,], '2021-01-01'
insert into test.test_bitmap format CSV 'c5', [,,0], '2021-01-01'
insert into test.test_bitmap format CSV 'c6', [, 0 ,0 ], '2021-01-01'
insert into test.test_bitmap format CSV 'c7', [0,1,2], '2021-01-01'
insert into test.test_bitmap format CSV 'c8', '[0,1,2]', '2021-01-01'
insert into test.test_bitmap format CSV 'c9', "[0,1,2]", '2021-01-01'

select * from test.test_bitmap order by tag;

---- format JSONEachRow
truncate table test.test_bitmap;
insert into test.test_bitmap format JSONEachRow {"tag":"j0","uids":[],"p_date":"2021-01-01"};
insert into test.test_bitmap format JSONEachRow {"tag":"j1","uids":[,],"p_date":"2021-01-01"};
insert into test.test_bitmap format JSONEachRow {"tag":"j2","uids":[,,],"p_date":"2021-01-01"};
insert into test.test_bitmap format JSONEachRow {"tag":"j3","uids":[0,,],"p_date":"2021-01-01"};
insert into test.test_bitmap format JSONEachRow {"tag":"j4","uids":[,0,],"p_date":"2021-01-01"};
insert into test.test_bitmap format JSONEachRow {"tag":"j5","uids":[,,0],"p_date":"2021-01-01"};
insert into test.test_bitmap format JSONEachRow {"tag":"j6","uids":[, 0 ,0 ],"p_date":"2021-01-01"};
insert into test.test_bitmap format JSONEachRow {"tag":"j7","uids":[0,1,2],"p_date":"2021-01-01"};
insert into test.test_bitmap format JSONEachRow {"tag":"j8","uids":'[0,1,2]',"p_date":"2021-01-01"};
insert into test.test_bitmap format JSONEachRow {"tag":"j9","uids":"[0,1,2]","p_date":"2021-01-01"};

select * from test.test_bitmap order by tag;

drop table if exists test.test_bitmap;
