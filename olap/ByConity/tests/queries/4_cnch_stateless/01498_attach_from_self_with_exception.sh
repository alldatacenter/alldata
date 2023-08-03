#!/usr/bin/env bash

set -e

CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh

source ${CURDIR}/attach_common.func

echo "TEST ATTACH FROM SELF DETACHED PARTITION"

# We only cover partial of test, since testing all will takes a lot of time,
# and easily lead to ci timeout
check_list=("load_part" "before_commit_fail" "mid_commit_fail" "after_commit_fail")
for exception_name in ${check_list[*]}; do
    echo "TEST ATTACH FROM SELF DETACHED PARTITION WITH EXCEPTION ${exception_name}"

    table_name="afs_detached_partition_except_${exception_name}"
    prepare_query=`cat <<EOF
        drop table if exists ${table_name};
        create table ${table_name} (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);

        insert into ${table_name} values(1, 2)(1, 3)(2, 4)(2, 5);

        select '(${exception_name})---init_data---';
        select * from ${table_name} order by (pt, key);

        alter table ${table_name} detach partition 1;
        alter table ${table_name} detach partition 2;

        select '(${exception_name})---after detach---';
        select * from ${table_name} order by (pt, key);

        insert into ${table_name} values(1, 6)(2, 7);

        select '(${exception_name})---reinsert---';
        select * from ${table_name} order by (pt, key);
EOF
`

    ${CLICKHOUSE_CLIENT} --multiquery --query "${prepare_query}"

    set +e

    result=$(${CLICKHOUSE_CLIENT} --multiquery --query "alter table ${table_name} attach partition 1 settings attach_failure_injection_knob=${all_exception_points[${exception_name}]}, async_post_commit=0;" 2>&1)

    echo $([ $? -eq 0 ] && echo "attach success" || echo "attach fail")
    injected_exception_count=$(echo "${result}" | grep -c "Injected exception")
    echo "Has injected exception: $([ ${injected_exception_count} -gt 0 ] && echo "True" || echo "False")"

    set -e

    verify_query=`cat <<EOF
        select '(${exception_name})---after attach---';
        select * from ${table_name} order by (pt, key);

        alter table ${table_name} attach partition 1;
        alter table ${table_name} attach partition 2;

        select '(${exception_name})---verify---';
        select * from ${table_name} order by (pt, key);
EOF
`

    ${CLICKHOUSE_CLIENT} --multiquery --query "${verify_query}"

done

echo ""
echo "TEST ATTACH FROM SELF DETACHED PART"

check_list=("load_part" "before_commit_fail" "mid_commit_fail")
for exception_name in ${check_list[*]}; do
    echo "TEST ATTACH FROM SELF DETACHED PART WITH EXCEPTION ${exception_name}"

    table_name="afs_detached_part_except_${exception_name}"
    prepare_query=`cat <<EOF
        drop table if exists ${table_name};
        create table ${table_name} (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);

        insert into ${table_name} values(3, 1);
        insert into ${table_name} values(3, 2);
        insert into ${table_name} values(4, 3);

        select '(${exception_name})---init_data---';
        select * from ${table_name} order by (pt, key);
EOF
`
    ${CLICKHOUSE_CLIENT} --multiquery --query "${prepare_query}"

    detached_part_name=`${CLICKHOUSE_CLIENT} --query "select name from system.cnch_parts where database='${CLICKHOUSE_DATABASE}' and table = '${table_name}' and partition like '%3%' order by name asc limit 1"`

    attach_query=`cat <<EOF
        alter table ${table_name} detach partition 3;
        alter table ${table_name} detach partition 4;

        select '(${exception_name})---after detach';
        select * from ${table_name} order by (pt, key);

        insert into ${table_name} values(3, 4)(4, 5)(5, 6);

        select '(${exception_name})---reinsert';
        select * from ${table_name} order by (pt, key);
EOF
`
    ${CLICKHOUSE_CLIENT} --multiquery --query "${attach_query}"

    set +e

    result=$(${CLICKHOUSE_CLIENT} --query "alter table ${table_name} attach part '${detached_part_name}' settings attach_failure_injection_knob=${all_exception_points[${exception_name}]}, async_post_commit=0" 2>&1)

    echo $([ $? -eq 0 ] && echo "attach success" || echo "attach fail")
    injected_exception_count=$(echo "${result}" | grep -c "Injected exception")
    echo "Has injected exception: $([ ${injected_exception_count} -gt 0 ] && echo "True" || echo "False")"

    set -e

    verify_query=`cat <<EOF
        select '(${exception_name})---after attach';
        select * from ${table_name} order by (pt, key);

        alter table ${table_name} attach partition 3;
        alter table ${table_name} attach partition 4; 

        select '(${exception_name})---verify';
        select * from ${table_name} order by (pt, key);
EOF
`

    ${CLICKHOUSE_CLIENT} --multiquery --query "${verify_query}"

done