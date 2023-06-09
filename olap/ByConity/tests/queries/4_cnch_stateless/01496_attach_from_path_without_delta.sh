#!/usr/bin/env bash

set -e

CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh

source ${CURDIR}/attach_common.func

# Attach partition from path
${CLICKHOUSE_CLIENT} --multiquery <<'EOF'
select 'ATTACH PARTITION FROM PATH';
drop table if exists afp_partition_src_no_delta;
drop table if exists afp_partition_tgt_no_delta;

create table afp_partition_src_no_delta (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);
create table afp_partition_tgt_no_delta (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);

insert into afp_partition_src_no_delta values(1, 1);
insert into afp_partition_src_no_delta values(1, 2);
insert into afp_partition_src_no_delta values(2, 3);
insert into afp_partition_src_no_delta values(3, 4);
insert into afp_partition_tgt_no_delta values(1, 5);
insert into afp_partition_tgt_no_delta values(2, 6);
insert into afp_partition_tgt_no_delta values(3, 7);

select '(afp_partition_ss)---src before detach---';
select * from afp_partition_src_no_delta order by (pt, key);
select '(afp_partition_ss)---tgt before detach---';
select * from afp_partition_tgt_no_delta order by (pt, key);

alter table afp_partition_src_no_delta detach partition 1;
alter table afp_partition_src_no_delta detach partition 2;
alter table afp_partition_src_no_delta detach partition 3;

select '(afp_partition_ss)---src after detach---';
select * from afp_partition_src_no_delta order by (pt, key);
select '(afp_partition_ss)--tgt after detach---';
select * from afp_partition_tgt_no_delta order by (pt, key);
EOF

table_detached_path ${CLICKHOUSE_DATABASE} afp_partition_src_no_delta
${CLICKHOUSE_CLIENT} --query "alter table afp_partition_tgt_no_delta attach partition 1 from '${_TABLE_DETACHED_PATH}'"

${CLICKHOUSE_CLIENT} --multiquery <<'EOF'
select '(afp_partition_ss)---src after attach---';
select * from afp_partition_src_no_delta order by (pt, key);
select '(afp_partition_ss)---tgt after attach---';
select * from afp_partition_tgt_no_delta order by (pt, key);
EOF

${CLICKHOUSE_CLIENT} --query "alter table afp_partition_tgt_no_delta attach partition 2 from '${_TABLE_DETACHED_PATH}'"

${CLICKHOUSE_CLIENT} --multiquery <<'EOF'
select '(afp_partition_ss)---src final attach---';
select * from afp_partition_src_no_delta order by (pt, key);
select '(afp_partition_ss)---tgt final attach---';
select * from afp_partition_tgt_no_delta order by (pt, key);

alter table afp_partition_src_no_delta attach partition 3;
select '(afp_partition_ss)---src verify---';
select * from afp_partition_src_no_delta order by (pt, key);
select '(afp_partition_ss)---tgt verify---';
select * from afp_partition_tgt_no_delta order by (pt, key);
EOF

# Attach parts from path
${CLICKHOUSE_CLIENT} --multiquery <<'EOF'
select 'ATTACH PARTS FROM PATH';
drop table if exists afp_parts_src_no_delta;
drop table if exists afp_parts_tgt_no_delta;

create table afp_parts_src_no_delta (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);
create table afp_parts_tgt_no_delta (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);

insert into afp_parts_src_no_delta values(1, 5);
insert into afp_parts_src_no_delta values(1, 6);
insert into afp_parts_src_no_delta values(2, 7);
insert into afp_parts_tgt_no_delta values(1, 8);
insert into afp_parts_tgt_no_delta values(1, 9);
insert into afp_parts_tgt_no_delta values(2, 10);

select '(afp_parts_ss)---src before detach---';
select * from afp_parts_src_no_delta order by (pt, key);
select '(afp_parts_ss)---tgt before detach---';
select * from afp_parts_tgt_no_delta order by (pt, key);

alter table afp_parts_src_no_delta detach partition 1;
alter table afp_parts_src_no_delta detach partition 2;

select '(afp_parts_ss)---src after detach---';
select * from afp_parts_src_no_delta order by (pt, key);
select '(afp_parts_ss)---tgt after detach---';
select * from afp_parts_tgt_no_delta order by (pt, key);
EOF

table_detached_path ${CLICKHOUSE_DATABASE} afp_parts_src_no_delta
${CLICKHOUSE_CLIENT} --query "alter table afp_parts_tgt_no_delta attach parts from '${_TABLE_DETACHED_PATH}'"

${CLICKHOUSE_CLIENT} --multiquery <<'EOF'
select '(afp_parts_ss)---src after attach---';
select * from afp_parts_src_no_delta order by (pt, key);
select '(afp_parts_ss)---tgt after attach---';
select * from afp_parts_tgt_no_delta order by (pt, key);

alter table afp_parts_src_no_delta detach partition 1;
alter table afp_parts_src_no_delta detach partition 2;

select '(afp_parts_ss)---src verify---';
select * from afp_parts_src_no_delta order by (pt, key);
EOF