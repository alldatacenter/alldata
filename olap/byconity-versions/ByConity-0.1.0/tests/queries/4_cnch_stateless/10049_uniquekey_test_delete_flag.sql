drop table if exists delete_by_unique_key;

set enable_staging_area_for_write = 0;
CREATE table delete_by_unique_key(
    `event_time` DateTime,
    `product_id` UInt64,
    `amount` UInt32,
    `revenue` UInt64)
ENGINE = CnchMergeTree()
partition by toDate(event_time)
order by (event_time, product_id)
unique key product_id;

insert into delete_by_unique_key values ('2021-07-13 18:50:00', 10001, 5, 500),('2021-07-13 18:50:00', 10002, 2, 200),('2021-07-13 18:50:00', 10003, 1, 100);
insert into delete_by_unique_key values ('2021-07-13 18:50:01', 10002, 4, 400),('2021-07-14 18:50:01', 10003, 2, 200),('2021-07-13 18:50:01', 10004, 1, 100);

select 'select unique table';
select * from delete_by_unique_key order by event_time, product_id, amount;

select '';
insert into delete_by_unique_key (event_time, product_id, amount, revenue, _delete_flag_) values ('2021-07-13 18:50:01', 10002, 5, 500, 1),('2021-07-14 18:50:00', 10003, 2, 200, 1);
select 'delete data of pair(2021-07-13, 10002) and pair(2021-07-14, 10003)';
select 'select unique table';
select * from delete_by_unique_key order by event_time, product_id, amount;

select '';
insert into delete_by_unique_key (event_time, product_id, amount, revenue, _delete_flag_) select event_time, product_id, amount, revenue, 1 as _delete_flag_ from delete_by_unique_key where revenue >= 500;
select 'delete data whose revenue is bigger than 500 using insert select, write to another replica';
select 'select unique table';
select * from delete_by_unique_key order by event_time, product_id, amount;

drop table if exists delete_by_unique_key;

select '-----------------------------------------------------';
select 'test enable staging area';
set enable_staging_area_for_write = 1;
CREATE table delete_by_unique_key(
    `event_time` DateTime,
    `product_id` UInt64,
    `amount` UInt32,
    `revenue` UInt64)
ENGINE = CnchMergeTree()
partition by toDate(event_time)
order by (event_time, product_id)
unique key product_id;

SYSTEM START DEDUP WORKER delete_by_unique_key;
SYSTEM STOP DEDUP WORKER delete_by_unique_key;

insert into delete_by_unique_key values ('2021-07-13 18:50:00', 10001, 5, 500),('2021-07-13 18:50:00', 10002, 2, 200),('2021-07-13 18:50:00', 10003, 1, 100);
insert into delete_by_unique_key values ('2021-07-13 18:50:01', 10002, 4, 400),('2021-07-14 18:50:01', 10003, 2, 200),('2021-07-13 18:50:01', 10004, 1, 100);

select 'select unique table count()';
select count() from delete_by_unique_key;

select '';
set enable_staging_area_for_write = 0;
insert into delete_by_unique_key (event_time, product_id, amount, revenue, _delete_flag_) values ('2021-07-13 18:50:01', 10002, 5, 500, 1),('2021-07-14 18:50:00', 10003, 2, 200, 1);
select 'delete data of pair(2021-07-13, 10002) and pair(2021-07-14, 10003) into staging area';
select 'select unique table';
select * from delete_by_unique_key order by event_time, product_id, amount;

select '';
insert into delete_by_unique_key (event_time, product_id, amount, revenue, _delete_flag_) select event_time, product_id, amount, revenue, 1 as _delete_flag_ from delete_by_unique_key where revenue >= 500;
select 'delete data whose revenue is bigger than 500 using insert select, write to another replica';
select 'select unique table';
select * from delete_by_unique_key order by event_time, product_id, amount;

drop table if exists delete_by_unique_key;
