set create_stats_time_output=0;
drop database if exists test_stats_45005_memory;
create database test_stats_45005_memory;
use test_stats_45005_memory;

create table memory_test (
                             `id` UInt64,
                             `i8` Int8,
                             `i16` Int16,
                             `i32` Int32
) ENGINE = CnchMergeTree PARTITION BY id ORDER BY id;
create table cnch_test (
                           `i64` Int64,
                           `u8` UInt8,
                           `u16` UInt16,
                           `u32` UInt32
) ENGINE = CnchMergeTree PARTITION BY u8 ORDER BY u8;

insert into memory_test values (1, -1, -10, -100)(2, -2, -20, -200);
insert into cnch_test values (-1000, 1, 10, 100)( 2, 20, 200, 2000);

set enable_memory_catalog=1;
select '---------create memory stats';
create stats memory_test;
select '---------show memory stats';
show stats all;


set enable_memory_catalog=0;
select '---------create cnch stats';
create stats cnch_test;
select '---------show cnch stats';
show stats all;


set enable_memory_catalog=1;
select '---------show memory stats';
show stats all;
select '---------drop memory stats';
drop stats all;
select '---------show memory stats';
show stats all;

set enable_memory_catalog=0;
select '---------show cnch stats';
show stats all;
select '---------drop cnch stats';
drop stats all;
select '---------show cnch stats';
show stats all;

drop table memory_test;
drop table cnch_test;
drop database test_stats_45005_memory;

