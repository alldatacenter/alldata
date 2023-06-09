set create_stats_time_output=0;
drop database if exists `1.2.3`;

create database `1.2.3`;
create table `1.2.3`.`4.5.6`(
    id UInt64,
    `7.8.9` UInt64
) Engine=CnchMergeTree() order by id;

insert into `1.2.3`.`4.5.6` values (1, 2)(3, 4);

set statistics_enable_sample=0;
select '*** create stats';
create stats `1.2.3`.`4.5.6`;
select '*** show stats';
show stats `1.2.3`.`4.5.6`;
select '*** show column stats';
show column_stats `1.2.3`.`4.5.6`;
drop stats `1.2.3`.`4.5.6`;
select '*** show stats';
show stats `1.2.3`.`4.5.6`;

set statistics_enable_sample=1;
set statistics_accurate_sample_ndv='NEVER';
select '*** create stats';
create stats `1.2.3`.`4.5.6`;
select '*** show stats';
show stats `1.2.3`.`4.5.6`;
select '*** show column stats';
show column_stats `1.2.3`.`4.5.6`;
drop stats `1.2.3`.`4.5.6`;
select '*** show stats';
show stats `1.2.3`.`4.5.6`;

set statistics_enable_sample=1;
set statistics_accurate_sample_ndv='ALWAYS';
select '*** create stats';
create stats `1.2.3`.`4.5.6`;
select '*** show stats';
show stats `1.2.3`.`4.5.6`;
select '*** show column stats';
show column_stats `1.2.3`.`4.5.6`;
drop stats `1.2.3`.`4.5.6`;
select '*** show stats';
show stats `1.2.3`.`4.5.6`;

drop database if exists `1.2.3`;
