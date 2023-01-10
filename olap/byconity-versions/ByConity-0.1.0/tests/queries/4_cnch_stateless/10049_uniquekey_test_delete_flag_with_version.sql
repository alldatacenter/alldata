drop table if exists delete_by_unique_key_with_version;

set enable_staging_area_for_write = 0;
CREATE table delete_by_unique_key_with_version(
    `event_time` DateTime,
    `product_id` UInt64,
    `amount` UInt32,
    `revenue` UInt64,
    `version` UInt64)
ENGINE = CnchMergeTree(version)
partition by toDate(event_time)
order by (event_time, product_id)
unique key product_id;

insert into delete_by_unique_key_with_version values ('2021-07-13 18:50:00', 10001, 5, 500, 2),('2021-07-13 18:50:00', 10002, 2, 200, 2),('2021-07-13 18:50:00', 10003, 1, 100, 2);
insert into delete_by_unique_key_with_version values ('2021-07-13 18:50:01', 10002, 4, 400, 1),('2021-07-14 18:50:01', 10003, 2, 200, 2),('2021-07-13 18:50:01', 10004, 1, 100, 2);

select 'select unique table';
select * from delete_by_unique_key_with_version order by event_time, product_id, amount;

select '';
insert into delete_by_unique_key_with_version (event_time, product_id, amount, revenue, version, _delete_flag_) values ('2021-07-13 18:50:01', 10002, 5, 500, 1, 1),('2021-07-14 18:50:00', 10003, 2, 200, 1, 1);
select 'delete data with lower version, which will not take effect';
select 'select unique table';
select * from delete_by_unique_key_with_version order by event_time, product_id, amount;

select '';
insert into delete_by_unique_key_with_version (event_time, product_id, amount, revenue, version, _delete_flag_) values ('2021-07-13 18:50:01', 10002, 5, 500, 5, 1),('2021-07-14 18:50:00', 10003, 2, 200, 5, 1);
select 'delete data with higher version, which will take effect, delete pair(2021-07-13, 10002) and pair(2021-07-14, 10003)';
select 'select unique table';
select * from delete_by_unique_key_with_version order by event_time, product_id, amount;

select '';
insert into delete_by_unique_key_with_version (event_time, product_id, amount, revenue, version) values ('2021-07-13 19:50:01', 10002, 5, 5000, 2),('2021-07-14 19:50:00', 10003, 2, 200, 1);
select 'insert data with lower version which has just been deleted';
select 'select unique table';
select * from delete_by_unique_key_with_version order by event_time, product_id, amount;

select '';
insert into delete_by_unique_key_with_version (event_time, product_id, amount, revenue, _delete_flag_) select event_time, product_id, amount, revenue, 1 as _delete_flag_ from delete_by_unique_key_with_version where revenue >= 500;
select 'delete data with ignoring version whose revenue is bigger than 500 using insert select, write to another replica';
select 'select unique table';
select * from delete_by_unique_key_with_version order by event_time, product_id, amount;

drop table if exists delete_by_unique_key_with_version;

select '-----------------------------------------------------';
select 'test enable staging area';
set enable_staging_area_for_write = 1;
CREATE table delete_by_unique_key_with_version(
    `event_time` DateTime,
    `product_id` UInt64,
    `amount` UInt32,
    `revenue` UInt64,
    `version` UInt64)
ENGINE = CnchMergeTree(version)
partition by toDate(event_time)
order by (event_time, product_id)
unique key product_id;

SYSTEM START DEDUP WORKER delete_by_unique_key_with_version;
SYSTEM STOP DEDUP WORKER delete_by_unique_key_with_version;

insert into delete_by_unique_key_with_version values ('2021-07-13 18:50:00', 10001, 5, 500, 2),('2021-07-13 18:50:00', 10002, 2, 200, 2),('2021-07-13 18:50:00', 10003, 1, 100, 2);
insert into delete_by_unique_key_with_version values ('2021-07-13 18:50:01', 10002, 4, 400, 1),('2021-07-14 18:50:01', 10003, 2, 200, 2),('2021-07-13 18:50:01', 10004, 1, 100, 2);

select 'select unique table count()';
select count() from delete_by_unique_key_with_version;

select '';
insert into delete_by_unique_key_with_version (event_time, product_id, amount, revenue, version, _delete_flag_) values ('2021-07-13 18:50:01', 10002, 5, 500, 1, 1),('2021-07-14 18:50:00', 10003, 2, 200, 1, 1);
select 'delete data with lower version, which will not take effect';
select 'select unique table count()';
select count() from delete_by_unique_key_with_version;

select '';
insert into delete_by_unique_key_with_version (event_time, product_id, amount, revenue, version, _delete_flag_) values ('2021-07-13 18:50:01', 10002, 5, 500, 5, 1),('2021-07-14 18:50:00', 10003, 2, 200, 5, 1);
select 'delete data with higher version, which will take effect, delete pair(2021-07-13, 10002) and pair(2021-07-14, 10003)';
select 'select unique table count()';
select count() from delete_by_unique_key_with_version;

select '';
insert into delete_by_unique_key_with_version (event_time, product_id, amount, revenue, version) values ('2021-07-13 19:50:01', 10002, 5, 5000, 2),('2021-07-14 19:50:00', 10003, 2, 200, 1);
select 'insert data with lower version which has just been deleted';
SYSTEM START DEDUP WORKER delete_by_unique_key_with_version;
SYSTEM SYNC DEDUP WORKER delete_by_unique_key_with_version;
select 'start dedup worker and select unique table';
select * from delete_by_unique_key_with_version order by event_time, product_id, amount;

select '';
insert into delete_by_unique_key_with_version (event_time, product_id, amount, revenue, _delete_flag_) select event_time, product_id, amount, revenue, 1 as _delete_flag_ from delete_by_unique_key_with_version where revenue >= 500;
select 'delete data with ignoring version whose revenue is bigger than 500 using insert select, write to another replica';
SYSTEM SYNC DEDUP WORKER delete_by_unique_key_with_version;
select 'select unique table';
select * from delete_by_unique_key_with_version order by event_time, product_id, amount;

drop table delete_by_unique_key_with_version;
