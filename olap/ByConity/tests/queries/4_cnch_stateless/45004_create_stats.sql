set create_stats_time_output=0;
set statistics_enable_sample=0;
set statistics_accurate_sample_ndv='ALWAYS';
drop database if exists test_stats_45004_full;
create database test_stats_45004_full;
use test_stats_45004_full;
create table tb (
   `id` UInt64,
   `i8` Int8,
   `i16` Int16,
   `i32` Int32,
   `i64` Int64,
   `u8` UInt8,
   `u16` UInt16,
   `u32` UInt32,
   `u64` UInt64,
   `f32` Float32,
   `f64` Float64,
   `date` Date,
   `date32` Date32,
   `datetime` DateTime,
   `datetime64` DateTime64(3),
   `str` String,
   `fxstr` FixedString(20),
   `strlowcard` LowCardinality(String),
   `fxstrlowcard` LowCardinality(FixedString(20)),
   `decimal32` Decimal32(5),
   `decimal64` Decimal64(10),
   `decimal128` Decimal128(20)
) ENGINE = CnchMergeTree PARTITION BY id ORDER BY id;

create table tbnull (
   `id` UInt64,
   `i8null` Nullable(Int8),
   `i16null` Nullable(Int16),
   `i32null` Nullable(Int32),
   `i64null` Nullable(Int64),
   `u8null`  Nullable(UInt8),
   `u16null` Nullable(UInt16),
   `u32null` Nullable(UInt32),
   `u64null` Nullable(UInt64),
   `f32null` Nullable(Float32),
   `f64null` Nullable(Float64),
   `datenull` Nullable(Date),
   `date32null` Nullable(Date32),
   `datetimenulll` Nullable(DateTime),
   `datetime64nulll` Nullable(DateTime64(3)),
   `strnull` Nullable(String),
   `fxstrnull` Nullable(FixedString(20)),
   `strlowcardnull` LowCardinality(Nullable(String)),
   `fxstrlowcardnull` LowCardinality(Nullable(FixedString(20))),
   `decimal32null` Nullable(Decimal32(5)),
   `decimal64null` Nullable(Decimal64(10)),
   `decimal128null` Nullable(Decimal128(20))
) ENGINE = CnchMergeTree PARTITION BY id ORDER BY id;
select '---------create empty stats';
create stats test_stats_45004_full.*;
select '---------show empty stats';
show stats test_stats_45004_full.*;
show column_stats test_stats_45004_full.*;
insert into tb values (1, -1, -10, -100, -1000, 1, 10, 100, 1000, 0.1, 0.01, '2022-01-01', '2022-01-01', '2022-01-01 00:00:01', '2022-01-01 00:00:01.11', 'str1', 'str1', 'str1', 'str1', 0.1, 0.01, 0.001)(2, -2, -20, -200, -2000, 2, 20, 200, 2000, 0.2, 0.02, '2022-02-02', '2022-02-02','2022-02-02 00:00:02', '2022-02-02 00:00:02.22', 'str2', 'str2', 'str2', 'str2', 0.2, 0.02, 0.002);

insert into tbnull values (1, -1, -10, -100, -1000, 1, 10, 100, 1000, 0.1, 0.01, '2022-01-01', '2022-01-01','2022-01-01 00:00:01', '2022-01-01 00:00:01.11', 'str1', 'str1', 'str1', 'str1', 0.1, 0.01, 0.001)(2, -2, -20, -200, -2000, 2, 20, 200, 2000, 0.2, 0.02, '2022-02-02', '2022-02-02','2022-02-02 00:00:02', '2022-02-02 00:00:02.22', 'str2', 'str2', 'str2', 'str2', 0.2, 0.02, 0.002)(3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
select '---------drop single stats';
drop stats tb;
select '---------show remaining stats';
show stats test_stats_45004_full.*;
select '---------create partial stats';
create stats if not exists test_stats_45004_full.*;
select '---------show partial stats';
show stats test_stats_45004_full.*;
show column_stats test_stats_45004_full.*;
select '---------create stats override';
create stats all;
select '---------show stats override';
show stats all;
show column_stats all;
select '---------drop stats all';
drop stats all;
select '---------show empty stats';
show stats all;
drop table tb;
drop table tbnull;
drop database test_stats_45004_full;
