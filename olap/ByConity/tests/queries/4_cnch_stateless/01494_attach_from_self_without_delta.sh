#!/usr/bin/env bash
CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh

set -e

# Test attach from current table's detached partition
${CLICKHOUSE_CLIENT} --multiquery <<'EOF'
select 'ATTACH FROM SELF TABLE DETACHED PARTITION';

drop table if exists afs_detached_partition;
create table afs_detached_partition (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);

insert into afs_detached_partition values(1, 0)(2, 2)(2, 3)(3, 4);
insert into afs_detached_partition values(1, 1);

select '(afs_partition)---self before detach---';
select * from afs_detached_partition order by (pt, key);

alter table afs_detached_partition detach partition 1;
alter table afs_detached_partition detach partition 2;


select '(afs_partition)---self after detach---';
select * from afs_detached_partition order by (pt, key);

insert into afs_detached_partition values(1, 5)(1, 6);

select '(afs_partition)---self after reinsert---';
select * from afs_detached_partition order by (pt, key);

alter table afs_detached_partition attach partition 1;

select '(afs_partition)---self after attach0---';
select * from afs_detached_partition order by (pt, key);

alter table afs_detached_partition attach partition 2;

select '(afs_partition)---self after attach1---';
select * from afs_detached_partition order by (pt, key);
EOF

# Test attach from current table's detached part
${CLICKHOUSE_CLIENT} --multiquery <<'EOF'
select 'ATTACH FROM SELF TABLE DETACHED PART';

drop table if exists afs_detached_part;
create table afs_detached_part (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);

insert into afs_detached_part values(1, 0)(1, 1);
insert into afs_detached_part values(1, 2);
insert into afs_detached_part values(2, 3);

select '(afs_detached_part)---self before detach---';
select * from afs_detached_part order by (pt, key);

EOF

DETACHED_PART_NAME=`${CLICKHOUSE_CLIENT} --query "select name from system.cnch_parts where database = '${CLICKHOUSE_DATABASE}' and table = 'afs_detached_part' and partition like '%1%' order by name asc limit 1"`

${CLICKHOUSE_CLIENT} --multiquery <<'EOF'
alter table afs_detached_part detach partition 1;
alter table afs_detached_part detach partition 2;

select '(afs_detached_part)---self after detach---';
select * from afs_detached_part order by (pt, key);

insert into afs_detached_part values(1, 4);

select '(afs_detached_part)---self after reinsert---';
select * from afs_detached_part order by (pt, key);

EOF

${CLICKHOUSE_CLIENT} --query "alter table afs_detached_part attach part '${DETACHED_PART_NAME}'"

${CLICKHOUSE_CLIENT} --multiquery <<'EOF'
select '(afs_detached_part)---self after attach---';
select * from afs_detached_part order by (pt, key);

alter table afs_detached_part attach partition 1;

select '(afs_detached_part)---self final0---';
select * from afs_detached_part order by (pt, key);

alter table afs_detached_part attach partition 2;

select '(afs_detached_part)---self final1---';
select * from afs_detached_part order by (pt, key);
EOF