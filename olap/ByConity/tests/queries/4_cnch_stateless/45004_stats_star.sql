set create_stats_time_output=0;
drop table if exists stats_star_45004.test;
drop table if exists stats_star_45004.test2;
drop table if exists stats_star_45004_fake.fake;
drop database if exists stats_star_45004;
drop database if exists stats_star_45004_fake;

create database stats_star_45004;
create database stats_star_45004_fake;
create table stats_star_45004.test(x UInt32) ENGINE=CnchMergeTree() order by x;
create table stats_star_45004.test2(x UInt32) ENGINE=CnchMergeTree() order by x;
create table stats_star_45004_fake.fake(x UInt32) ENGINE=CnchMergeTree() order by x;

insert into stats_star_45004.test values(1)(2);
insert into stats_star_45004.test2 values(10)(20);

use stats_star_45004_fake;
select '--- create stats real';
create stats stats_star_45004.*;
select '--- show stats real';
show stats stats_star_45004.*;
show column_stats stats_star_45004.*;
select '--- show stats fake';
show stats all;
show stats stats_star_45004_fake.*;
show column_stats all;
show column_stats stats_star_45004_fake.*;
select '--- drop stats fake';
drop stats all;
drop stats stats_star_45004_fake.*;
select '--- show stats real';
show stats stats_star_45004.*;
show column_stats stats_star_45004.*;
select '--- drop stats real';
drop stats stats_star_45004.*;
select '--- show stats real, empty';
show stats stats_star_45004.*;
show column_stats stats_star_45004.*;
select '--- create stats fake';
create stats all; 
create stats stats_star_45004_fake.*; 
select '--- show stats real, empty';
show stats stats_star_45004.*;
show column_stats stats_star_45004.*;


drop table stats_star_45004.test;
drop table stats_star_45004.test2;
drop table stats_star_45004_fake.fake;
drop database stats_star_45004;
drop database stats_star_45004_fake;
