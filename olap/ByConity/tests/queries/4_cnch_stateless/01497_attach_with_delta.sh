#!/usr/bin/env bash

set -e

CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh

source ${CURDIR}/attach_common.func

# Create table and trigger mutate, limit part number under 5, other wise it may trigger merge
QUERY="
drop table if exists afo_partition_with_delta_src;
drop table if exists afo_partition_with_delta_tgt;
drop table if exists afo_detached_partition_with_delta_src;
drop table if exists afo_detached_partition_with_delta_tgt;
drop table if exists afs_partition_with_delta;
drop table if exists afs_part_with_delta;
drop table if exists afp_partition_with_delta_src;
drop table if exists afp_partition_with_delta_tgt;
drop table if exists afp_parts_with_delta_src;
drop table if exists afp_parts_with_delta_tgt;
drop table if exists afo_partition_multi_with_delta_src;
drop table if exists afo_partition_multi_with_delta_tgt;
drop table if exists afo_detached_partition_multi_with_delta_src;
drop table if exists afo_detached_partition_multi_with_delta_tgt;
drop table if exists afs_partition_multi_with_delta;
drop table if exists afp_partition_multi_with_delta_src;
drop table if exists afp_partition_multi_with_delta_tgt;

create table afo_partition_with_delta_src (pt Int, key Int, value Int) engine = CnchMergeTree partition by pt order by pt;
create table afo_partition_with_delta_tgt (pt Int, key Int) engine = CnchMergeTree partition by pt order by pt;
create table afo_detached_partition_with_delta_src (pt Int, key Int, value Int) engine = CnchMergeTree partition by pt order by pt;
create table afo_detached_partition_with_delta_tgt (pt Int, key Int) engine = CnchMergeTree partition by pt order by pt;
create table afs_partition_with_delta (pt Int, key Int) engine = CnchMergeTree partition by pt order by pt;
create table afs_part_with_delta (pt Int, key Int) engine = CnchMergeTree partition by pt order by pt;
create table afp_partition_with_delta_src (pt Int, key Int, value Int) engine = CnchMergeTree partition by pt order by pt;
create table afp_partition_with_delta_tgt (pt Int, key Int) engine = CnchMergeTree partition by pt order by pt;
create table afp_parts_with_delta_src (pt Int, key Int, value Int) engine = CnchMergeTree partition by pt order by pt;
create table afp_parts_with_delta_tgt (pt Int, key Int) engine = CnchMergeTree partition by pt order by pt;
create table afo_partition_multi_with_delta_src (pt Int, key Int, value Int) engine = CnchMergeTree partition by pt order by pt;
create table afo_partition_multi_with_delta_tgt (pt Int, key Int) engine = CnchMergeTree partition by pt order by pt;
create table afo_detached_partition_multi_with_delta_src (pt Int, key Int, value Int) engine = CnchMergeTree partition by pt order by pt;
create table afo_detached_partition_multi_with_delta_tgt (pt Int, key Int) engine = CnchMergeTree partition by pt order by pt;
create table afs_partition_multi_with_delta (pt Int, key Int) engine = CnchMergeTree partition by pt order by pt;
create table afp_partition_multi_with_delta_src (pt Int, key Int, value Int) engine = CnchMergeTree partition by pt order by pt;
create table afp_partition_multi_with_delta_tgt (pt Int, key Int) engine = CnchMergeTree partition by pt order by pt;

insert into afo_partition_with_delta_src values(1, 1, 0);
insert into afo_partition_with_delta_src values(1, 2, 1);
insert into afo_partition_with_delta_tgt values(1, 20);
insert into afo_partition_with_delta_tgt values(2, 21);
insert into afo_detached_partition_with_delta_src values(1, 3, 2);
insert into afo_detached_partition_with_delta_src values(1, 4, 3);
insert into afo_detached_partition_with_delta_tgt values(1, 22);
insert into afo_detached_partition_with_delta_tgt values(2, 23);
insert into afs_partition_with_delta values(1, 5);
insert into afs_partition_with_delta values(1, 6);
insert into afs_part_with_delta values(1, 7);
insert into afs_part_with_delta values(1, 20);
insert into afp_partition_with_delta_src values(1, 8, 4);
insert into afp_partition_with_delta_src values(1, 9, 5);
insert into afp_partition_with_delta_tgt values(1, 23);
insert into afp_partition_with_delta_tgt values(2, 24);
insert into afp_parts_with_delta_src values(1, 10, 6);
insert into afp_parts_with_delta_src values(2, 11, 7);
insert into afp_parts_with_delta_tgt values(2, 25);
insert into afp_parts_with_delta_tgt values(3, 26);
insert into afo_partition_multi_with_delta_src values(1, 12, 8);
insert into afo_partition_multi_with_delta_src values(2, 13, 9);
insert into afo_partition_multi_with_delta_tgt values(2, 27);
insert into afo_partition_multi_with_delta_tgt values(3, 28);
insert into afo_detached_partition_multi_with_delta_src values(1, 14, 10);
insert into afo_detached_partition_multi_with_delta_src values(2, 15, 11);
insert into afo_detached_partition_multi_with_delta_tgt values(2, 29);
insert into afo_detached_partition_multi_with_delta_tgt values(3, 30);
insert into afs_partition_multi_with_delta values(1, 16);
insert into afs_partition_multi_with_delta values(2, 17);
insert into afp_partition_multi_with_delta_src values(1, 18, 14);
insert into afp_partition_multi_with_delta_src values(2, 19, 15);
insert into afp_partition_multi_with_delta_tgt values(2, 31);
insert into afp_partition_multi_with_delta_tgt values(3, 32);

select sleep(3) format Null;

alter table afo_partition_with_delta_src drop column value;
alter table afo_detached_partition_with_delta_src drop column value;
alter table afs_partition_with_delta modify column key String;
alter table afs_part_with_delta modify column key String;
alter table afp_partition_with_delta_src drop column value;
alter table afp_parts_with_delta_src drop column value;
alter table afo_partition_multi_with_delta_src drop column value;
alter table afo_detached_partition_multi_with_delta_src drop column value;
alter table afs_partition_multi_with_delta modify column key String;
alter table afp_partition_multi_with_delta_src drop column value;

system start merges ${CLICKHOUSE_DATABASE}.afo_partition_with_delta_src;
system start merges ${CLICKHOUSE_DATABASE}.afo_detached_partition_with_delta_src;
system start merges ${CLICKHOUSE_DATABASE}.afs_partition_with_delta;
system start merges ${CLICKHOUSE_DATABASE}.afs_part_with_delta;
system start merges ${CLICKHOUSE_DATABASE}.afp_partition_with_delta_src;
system start merges ${CLICKHOUSE_DATABASE}.afp_parts_with_delta_src;
system start merges ${CLICKHOUSE_DATABASE}.afo_partition_multi_with_delta_src;
system start merges ${CLICKHOUSE_DATABASE}.afo_detached_partition_multi_with_delta_src;
system start merges ${CLICKHOUSE_DATABASE}.afs_partition_multi_with_delta;
system start merges ${CLICKHOUSE_DATABASE}.afp_partition_multi_with_delta_src;

system stop merges ${CLICKHOUSE_DATABASE}.afo_partition_with_delta_tgt;
system stop merges ${CLICKHOUSE_DATABASE}.afo_detached_partition_with_delta_tgt;
system stop merges ${CLICKHOUSE_DATABASE}.afp_partition_with_delta_tgt;
system stop merges ${CLICKHOUSE_DATABASE}.afp_parts_with_delta_tgt;
system stop merges ${CLICKHOUSE_DATABASE}.afo_partition_multi_with_delta_tgt;
system stop merges ${CLICKHOUSE_DATABASE}.afo_detached_partition_multi_with_delta_tgt;
system stop merges ${CLICKHOUSE_DATABASE}.afp_partition_multi_with_delta_tgt;
"
echo "${QUERY}" | ${CLICKHOUSE_CLIENT} --multiquery 

echo "TEST attach from other table's active parition with delta"
wait_for_mutate ${CLICKHOUSE_DATABASE} afo_partition_with_delta_src 4

QUERY="
select '---(afo_partition)src before attach---';
select * from afo_partition_with_delta_src order by (pt, key);
select '---(afo_partition)tgt before attach---';
select * from afo_partition_with_delta_tgt order by (pt, key);

alter table afo_partition_with_delta_tgt attach partition 1 from afo_partition_with_delta_src;

select '---(afo_partition)src after attach---';
select * from afo_partition_with_delta_src order by (pt, key);
select '---(afo_partition)tgt after attach---';
select * from afo_partition_with_delta_tgt order by (pt, key);

select '(afo_partition)Part count of target table', count() from system.cnch_parts where database = '${CLICKHOUSE_DATABASE}' and table = 'afo_partition_with_delta_tgt';
"
echo "${QUERY}" | ${CLICKHOUSE_CLIENT} --multiquery

echo "TEST attach from other table's detached partition with delta"
wait_for_mutate ${CLICKHOUSE_DATABASE} afo_detached_partition_with_delta_src 4

QUERY="
select '(afo_detached_partition)---src before detach---';
select * from afo_detached_partition_with_delta_src order by (pt, key);
select '(afo_detached_partition)---tgt before detach---';
select * from afo_detached_partition_with_delta_tgt order by (pt, key);

alter table afo_detached_partition_with_delta_src detach partition 1;

select '(afo_detached_partition)---src after detach---';
select * from afo_detached_partition_with_delta_src order by (pt, key);
select '(afo_detached_partition)---tgt after detach---';
select * from afo_detached_partition_with_delta_tgt order by (pt, key);

alter table afo_detached_partition_with_delta_tgt attach detached partition 1 from afo_detached_partition_with_delta_src;

select '(afo_detached_partition)---src after attach---';
select * from afo_detached_partition_with_delta_src order by (pt, key);
select '(afo_detached_partition)---tgt after attach---';
select * from afo_detached_partition_with_delta_tgt order by (pt, key);

select '(afo_detached_partition)Part count of target table', count() from system.cnch_parts where database = '${CLICKHOUSE_DATABASE}' and table = 'afo_detached_partition_with_delta_tgt';
"
echo "${QUERY}" | ${CLICKHOUSE_CLIENT} --multiquery

echo "TEST attach from current table's detach partition with delta"
wait_for_mutate ${CLICKHOUSE_DATABASE} afs_partition_with_delta 4

QUERY="
select '(afs_partition)---from partition before detach---';
select * from afs_partition_with_delta order by (pt, key);

alter table afs_partition_with_delta detach partition 1;

select '(afs_partition)---from partition after detach---';
select * from afs_partition_with_delta order by (pt, key);

insert into afs_partition_with_delta values(1, '7')(2, '8');

select '(afs_partition)---from partition after reinsert---';
select * from afs_partition_with_delta order by (pt, key);

alter table afs_partition_with_delta attach partition 1;

select '(afs_partition)---from partition after attach---';
select * from afs_partition_with_delta order by (pt, key);
"
echo "${QUERY}" | ${CLICKHOUSE_CLIENT} --multiquery

echo "TEST attach from current table's detach part with delta"
wait_for_mutate ${CLICKHOUSE_DATABASE} afs_part_with_delta 4

PART_NAME=`${CLICKHOUSE_CLIENT} --query "select name from system.cnch_parts where database = '${CLICKHOUSE_DATABASE}' and table = 'afs_part_with_delta' and visible order by name asc limit 1"`

QUERY="
select '(afs_detached_part)---from part before detach---';
select * from afs_part_with_delta order by (pt, key);

alter table afs_part_with_delta detach partition 1;

select '(afs_detached_part)---from part after detach---';
select * from afs_part_with_delta order by (pt, key);

insert into afs_part_with_delta values(1, '21')(2, '22');

select '(afs_detached_part)---from part after reinsert---';
select * from afs_part_with_delta order by (pt, key);

alter table afs_part_with_delta attach part '${PART_NAME}';

select '(afs_detached_part)---from part after attach---';
select * from afs_part_with_delta order by (pt, key);

alter table afs_part_with_delta attach partition 1;

select '(afs_detached_part)---from part final---';
select * from afs_part_with_delta order by (pt, key);
"
echo "${QUERY}" | ${CLICKHOUSE_CLIENT} --multiquery

echo "TEST attach partition from path with delta"
wait_for_mutate ${CLICKHOUSE_DATABASE} afp_partition_with_delta_src 4

${CLICKHOUSE_CLIENT} --multiquery <<'EOF'
select '(afp_partition_ss)---src before detach---';
select * from afp_partition_with_delta_src order by (pt, key);
select '(afp_partition_ss)---tgt before detach---';
select * from afp_partition_with_delta_tgt order by (pt, key);

alter table afp_partition_with_delta_src detach partition 1;

select '(afp_partition_ss)---src after detach---';
select * from afp_partition_with_delta_src order by (pt, key);
select '(afp_partition_ss)---tgt after detach---';
select * from afp_partition_with_delta_tgt order by (pt, key);
EOF

table_detached_path ${CLICKHOUSE_DATABASE} afp_partition_with_delta_src

QUERY="
alter table afp_partition_with_delta_tgt attach partition 1 from '${_TABLE_DETACHED_PATH}';

select '(afp_partition_ss)---src after attach---';
select * from afp_partition_with_delta_src order by (pt, key);
select '(afp_partition_ss)---tgt after attach---';
select * from afp_partition_with_delta_tgt order by (pt, key);

select '(afp_partition_ss)Part count of target table', count() from system.cnch_parts where database = '${CLICKHOUSE_DATABASE}' and table = 'afp_partition_with_delta_tgt';
"
echo "${QUERY}" | ${CLICKHOUSE_CLIENT} --multiquery

echo "TEST attach parts from path with delta"
wait_for_mutate ${CLICKHOUSE_DATABASE} afp_parts_with_delta_src 4

${CLICKHOUSE_CLIENT} --multiquery <<'EOF'
select '(afp_parts_ss)---src before detach---';
select * from afp_parts_with_delta_src order by (pt, key);
select '(afp_parts_ss)---tgt before detach---';
select * from afp_parts_with_delta_tgt order by (pt, key);

alter table afp_parts_with_delta_src detach partition 1;
alter table afp_parts_with_delta_src detach partition 2;

select '(afp_parts_ss)---src after detach---';
select * from afp_parts_with_delta_src order by (pt, key);
select '(afp_parts_ss)---tgt after detach---';
select * from afp_parts_with_delta_tgt order by (pt, key);
EOF

table_detached_path ${CLICKHOUSE_DATABASE} afp_parts_with_delta_src

QUERY="
alter table afp_parts_with_delta_tgt attach parts from '${_TABLE_DETACHED_PATH}';

select '(afp_parts_ss)---src after attach---';
select * from afp_parts_with_delta_src order by (pt, key);
select '(afp_parts_ss)---tgt after attach---';
select * from afp_parts_with_delta_tgt order by (pt, key);

select '(afp_parts_ss)Part count of target table', count() from system.cnch_parts where database = '${CLICKHOUSE_DATABASE}' and table = 'afp_parts_with_delta_tgt';
"
echo "${QUERY}" | ${CLICKHOUSE_CLIENT} --multiquery

echo "TEST attach one partition from other table's multi"
wait_for_mutate ${CLICKHOUSE_DATABASE} afo_partition_multi_with_delta_src 4

QUERY="
select '---(afo_partition_multi)src before attach---';
select * from afo_partition_multi_with_delta_src order by (pt, key);
select '---(afo_partition_multi)tgt before attach---';
select * from afo_partition_multi_with_delta_tgt order by (pt, key);

alter table afo_partition_multi_with_delta_tgt attach partition 2 from afo_partition_multi_with_delta_src;

select '---(afo_partition_multi)src after attach---';
select * from afo_partition_multi_with_delta_src order by (pt, key);
select '---(afo_partition_multi)tgt after attach---';
select * from afo_partition_multi_with_delta_tgt order by (pt, key);

select 'Part count of target table', count() from system.cnch_parts where database = '${CLICKHOUSE_DATABASE}' and table = 'afo_partition_multi_with_delta_tgt';
"
echo "${QUERY}" | ${CLICKHOUSE_CLIENT} --multiquery

echo "TEST attach one partition form other table's detached multi"
wait_for_mutate ${CLICKHOUSE_DATABASE} afo_detached_partition_multi_with_delta_src 4

QUERY="
select '(afo_detached_partition_multi)---src before detach---';
select * from afo_detached_partition_multi_with_delta_src order by (pt, key);
select '(afo_detached_partition_multi)---tgt before detach---';
select * from afo_detached_partition_multi_with_delta_tgt order by (pt, key);

alter table afo_detached_partition_multi_with_delta_src detach partition 1;
alter table afo_detached_partition_multi_with_delta_src detach partition 2;

select '(afo_detached_partition_multi)---src after detach---';
select * from afo_detached_partition_multi_with_delta_src order by (pt, key);
select '(afo_detached_partition_multi)---tgt after detach---';
select * from afo_detached_partition_multi_with_delta_tgt order by (pt, key);

alter table afo_detached_partition_multi_with_delta_tgt attach detached partition 2 from afo_detached_partition_multi_with_delta_src;

select '(afo_detached_partition_multi)---src after attach---';
select * from afo_detached_partition_multi_with_delta_src order by (pt, key);
select '(afo_detached_partition_multi)---tgt after attach---';
select * from afo_detached_partition_multi_with_delta_tgt order by (pt, key);

select '(afo_detached_partition_multi)Part count of target table', count() from system.cnch_parts where database = '${CLICKHOUSE_DATABASE}' and table = 'afo_detached_partition_multi_with_delta_tgt';

alter table afo_detached_partition_multi_with_delta_src attach partition 1;

select '(afo_detached_partition_multi)---src final---';
select * from afo_detached_partition_multi_with_delta_src order by (pt, key);
select '(afo_detached_partition_multi)---tgt final---';
select * from afo_detached_partition_multi_with_delta_tgt order by (pt, key);
"
echo "${QUERY}" | ${CLICKHOUSE_CLIENT} --multiquery

echo "TEST attach from self's detached partition multi"
wait_for_mutate ${CLICKHOUSE_DATABASE} afs_partition_multi_with_delta 4

${CLICKHOUSE_CLIENT} --multiquery <<'EOF'
select '(afs_partition_multi)---self before detach---';
select * from afs_partition_multi_with_delta order by (pt, key);

alter table afs_partition_multi_with_delta detach partition 1;
alter table afs_partition_multi_with_delta detach partition 2;

select '(afs_partition_multi)---self after detach---';
select * from afs_partition_multi_with_delta order by (pt, key);

insert into afs_partition_multi_with_delta values(1, '18')(2, '19');

select '(afs_partition_multi)---self after reinsert---';
select * from afs_partition_multi_with_delta order by (pt, key);

alter table afs_partition_multi_with_delta attach partition 2;

select '(afs_partition_multi)---self after attach---';
select * from afs_partition_multi_with_delta order by (pt, key);

alter table afs_partition_multi_with_delta attach partition 1;

select '(afs_partition_multi)---self final---';
select * from afs_partition_multi_with_delta order by (pt, key);
EOF


echo "TEST attach partition from path with multi"
wait_for_mutate ${CLICKHOUSE_DATABASE} afp_partition_multi_with_delta_src 4

${CLICKHOUSE_CLIENT} --multiquery <<'EOF'
select '(afp_partition_multi)---src before detach---';
select * from afp_partition_multi_with_delta_src order by (pt, key);
select '(afp_partition_multi)---tgt before detach---';
select * from afp_partition_multi_with_delta_tgt order by (pt, key);

alter table afp_partition_multi_with_delta_src detach partition 1;
alter table afp_partition_multi_with_delta_src detach partition 2;

select '(afp_partition_multi)---src after detach---';
select * from afp_partition_multi_with_delta_src order by (pt, key);
select '(afp_partition_multi)---tgt after detach---';
select * from afp_partition_multi_with_delta_tgt order by (pt, key);
EOF

table_detached_path ${CLICKHOUSE_DATABASE} afp_partition_multi_with_delta_src
QUERY="
alter table afp_partition_multi_with_delta_tgt attach partition 2 from '${_TABLE_DETACHED_PATH}';

select '(afp_partition_multi)---src after detach---';
select * from afp_partition_multi_with_delta_src order by (pt, key);
select '(afp_partition_multi)---tgt after detach---';
select * from afp_partition_multi_with_delta_tgt order by (pt, key);

select '(afp_partition_multi)Part count of target table', count() from system.cnch_parts where database = '${CLICKHOUSE_DATABASE}' and table = 'afp_partition_multi_with_delta_tgt';

alter table afp_partition_multi_with_delta_tgt attach partition 1 from '${_TABLE_DETACHED_PATH}';

select '(afp_partition_multi)---src final detach---';
select * from afp_partition_multi_with_delta_src order by (pt, key);
select '(afp_partition_multi)---tgt final detach---';
select * from afp_partition_multi_with_delta_tgt order by (pt, key);

select '(afp_partition_multi)Part count of target table', count() from system.cnch_parts where database = '${CLICKHOUSE_DATABASE}' and table = 'afp_partition_multi_with_delta_tgt';
"
echo "${QUERY}" | ${CLICKHOUSE_CLIENT} --multiquery
