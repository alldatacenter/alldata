-- Test attach from other table's active partition
select 'ATTACH FROM OTHER TABLE ACTIVE PARTITION WITHOUT DELTA';
drop table if exists afo_src_active_no_delta;
drop table if exists afo_tgt_active_no_delta;
create table afo_src_active_no_delta (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);
create table afo_tgt_active_no_delta (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);

insert into afo_src_active_no_delta values (1, 0)(1, 1)(1, 2)(2, 3);
insert into afo_tgt_active_no_delta values (1, 4)(2, 5);

select '(afo_partition)---src---';
select * from afo_src_active_no_delta order by (pt, key);
select '(afo_partition)---tgt---';
select * from afo_tgt_active_no_delta order by (pt, key);

alter table afo_tgt_active_no_delta attach partition 1 from afo_src_active_no_delta;

select '(afo_partition)---src after attach---';
select * from afo_src_active_no_delta order by (pt, key);
select '(afo_partition)---tgt after attach---';
select * from afo_tgt_active_no_delta order by (pt, key);

-- Test attach from other table's detached partition
select 'ATTACH FROM OTHER TABLE DETACHED PARTITION WITHOUT DELTA';
drop table if exists afo_src_detached_no_delta;
drop table if exists afo_tgt_detached_no_delta;
create table afo_src_detached_no_delta (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);
create table afo_tgt_detached_no_delta (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);

insert into afo_src_detached_no_delta values(1, 0)(1, 1)(1, 2)(2, 3)(3, 4);
insert into afo_tgt_detached_no_delta values(1, 5)(2, 6);

select '(afo_detached_partition)---src---';
select * from afo_src_detached_no_delta order by (pt, key);
select '(afo_detached_partition)---tgt---';
select * from afo_tgt_detached_no_delta order by (pt, key);

alter table afo_src_detached_no_delta detach partition 1;
alter table afo_src_detached_no_delta detach partition 2;

select '(afo_detached_partition)---src after detach---';
select * from afo_src_detached_no_delta order by (pt, key);
select '(afo_detached_partition)---tgt after detatch---';
select * from afo_tgt_detached_no_delta order by (pt, key);

alter table afo_tgt_detached_no_delta attach detached partition 1 from afo_src_detached_no_delta;

select '(afo_detached_partition)---src after attach---';
select * from afo_src_detached_no_delta order by (pt, key);
select '(afo_detached_partition)---tgt after attach---';
select * from afo_tgt_detached_no_delta order by (pt, key);

alter table afo_src_detached_no_delta attach partition 2;

select '(afo_detached_partition)---src final---';
select * from afo_src_detached_no_delta order by (pt, key);
select '(afo_detached_partition)---tgt final---';
select * from afo_tgt_detached_no_delta order by (pt, key);

-- Test attach from other table's active with replace
select 'ATTACH FROM OTHER TABLE ACTIVE PARTITION WITH REPLACE WITHOUT DELTA';
drop table if exists afo_src_active_replace_no_delta;
drop table if exists afo_tgt_active_replace_no_delta;
create table afo_src_active_replace_no_delta (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);
create table afo_tgt_active_replace_no_delta (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);

insert into afo_src_active_replace_no_delta values (1, 0)(1, 1)(1, 2)(2, 3);
insert into afo_tgt_active_replace_no_delta values (1, 3)(2, 4);

select '---src---';
select * from afo_src_active_replace_no_delta order by (pt, key);
select '---tgt---';
select * from afo_tgt_active_replace_no_delta order by (pt, key);

alter table afo_tgt_active_replace_no_delta replace partition 1 from afo_src_active_replace_no_delta;

select '---src after attach---';
select * from afo_src_active_replace_no_delta order by (pt, key);
select '---tgt after attach---';
select * from afo_tgt_active_replace_no_delta order by (pt, key);
