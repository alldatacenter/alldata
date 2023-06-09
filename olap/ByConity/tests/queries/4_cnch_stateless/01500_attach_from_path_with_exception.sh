#!/usr/bin/env bash

set -e

CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh

source ${CURDIR}/attach_common.func

echo "ATTACH PARTITION FROM PATH WITH EXCEPTION"

# We only cover partial of test, since testing all will takes a lot of time,
# and easily lead to ci timeout
check_list=("load_part" "move_part_fail" "mid_commit_fail" "after_commit_fail")
for exception_name in ${check_list[*]}; do
    echo "TEST ATTACH PARTITION FROM PATH WIHT EXCEPTION ${exception_name}"

    src_table_name="afp_partition_src_with_except_${exception_name}"
    tgt_table_name="afp_partition_tgt_with_except_${exception_name}"
    prepare_query=`cat <<EOF
        drop table if exists ${src_table_name};
        drop table if exists ${tgt_table_name};

        create table ${src_table_name} (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);
        create table ${tgt_table_name} (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);

        insert into ${src_table_name} values(5, 5);
        insert into ${src_table_name} values(5, 6);
        insert into ${src_table_name} values(6, 7);
        insert into ${src_table_name} values(6, 8);
        insert into ${tgt_table_name} values(5, 9)(6, 10)(7, 11);

        select '(${exception_name})---src_init_data';
        select * from ${src_table_name} order by (pt, key);
        select '(${exception_name})---tgt_init_data';
        select * from ${tgt_table_name} order by (pt, key);

        alter table ${src_table_name} detach partition 5;
        alter table ${src_table_name} detach partition 6;
        
        select '(${exception_name})---src after detach';
        select * from ${src_table_name} order by (pt, key);
        select '(${exception_name})---tgt after detach';
        select * from ${tgt_table_name} order by (pt, key);
EOF
`
    ${CLICKHOUSE_CLIENT} --multiquery --query "${prepare_query}"

    set +e

    table_detached_path ${CLICKHOUSE_DATABASE} ${src_table_name}
    result=$(${CLICKHOUSE_CLIENT} --query "alter table ${tgt_table_name} attach partition 5 from '${_TABLE_DETACHED_PATH}' settings attach_failure_injection_knob=${all_exception_points[${exception_name}]}, async_post_commit=0" 2>&1)

    echo $([ $? -eq 0 ] && echo "attach success" || echo "attach fail")
    injected_exception_count=$(echo "${result}" | grep -c "Injected exception")
    echo "Has injected exception: $([ ${injected_exception_count} -gt 0 ] && echo "True" || echo "False")"

    set -e

    verify_query=`cat <<EOF
        select '(${exception_name})---src after attach';
        select * from ${src_table_name} order by (pt, key);
        select '(${exception_name})---tgt after attach';
        select * from ${tgt_table_name} order by (pt, key);

        alter table ${src_table_name} attach partition 5;
        alter table ${src_table_name} attach partition 6;
        alter table ${tgt_table_name} attach partition 5;
        alter table ${tgt_table_name} attach partition 6;

        select '(${exception_name})---src verify';
        select * from ${src_table_name} order by (pt, key);
        select '(${exception_name})---tgt verify';
        select * from ${tgt_table_name} order by (pt, key);
EOF
`
    ${CLICKHOUSE_CLIENT} --multiquery --query "${verify_query}"

done

echo "ATTACH PARTS FROM PATH WITH EXCEPTION"

check_list=("load_part" "move_part_fail" "mid_commit_fail" "after_commit_fail")
for exception_name in ${check_list[*]}; do
    echo "TEST ATTACH PARTS FROM PATH WIHT EXCEPTION ${exception_name}"

    src_table_name="afp_parts_src_with_except_${exception_name}"
    tgt_table_name="afp_parts_tgt_with_except_${exception_name}"
    prepare_query=`cat <<EOF
        drop table if exists ${src_table_name};
        drop table if exists ${tgt_table_name};

        create table ${src_table_name} (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);
        create table ${tgt_table_name} (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);

        insert into ${src_table_name} values(5, 5);
        insert into ${src_table_name} values(5, 6);
        insert into ${src_table_name} values(6, 7);
        insert into ${src_table_name} values(6, 8);
        insert into ${tgt_table_name} values(5, 9)(6, 10)(7, 11);

        select '(${exception_name})---src_init_data';
        select * from ${src_table_name} order by (pt, key);
        select '(${exception_name})---tgt_init_data';
        select * from ${tgt_table_name} order by (pt, key);

        alter table ${src_table_name} detach partition 5;
        alter table ${src_table_name} detach partition 6;
        
        select '(${exception_name})---src after detach';
        select * from ${src_table_name} order by (pt, key);
        select '(${exception_name})---tgt after detach';
        select * from ${tgt_table_name} order by (pt, key);
EOF
`
    ${CLICKHOUSE_CLIENT} --multiquery --query "${prepare_query}"

    set +e

    table_detached_path ${CLICKHOUSE_DATABASE} ${src_table_name}
    result=$(${CLICKHOUSE_CLIENT} --query "alter table ${tgt_table_name} attach parts from '${_TABLE_DETACHED_PATH}' settings attach_failure_injection_knob=${all_exception_points[${exception_name}]}, async_post_commit=0" 2>&1)

    echo $([ $? -eq 0 ] && echo "attach success" || echo "attach fail")
    injected_exception_count=$(echo "${result}" | grep -c "Injected exception")
    echo "Has injected exception: $([ ${injected_exception_count} -gt 0 ] && echo "True" || echo "False")"

    set -e

    verify_query=`cat <<EOF
        select '(${exception_name})---src after attach';
        select * from ${src_table_name} order by (pt, key);
        select '(${exception_name})---tgt after attach';
        select * from ${tgt_table_name} order by (pt, key);

        alter table ${src_table_name} attach partition 5;
        alter table ${src_table_name} attach partition 6;
        alter table ${tgt_table_name} attach partition 5;
        alter table ${tgt_table_name} attach partition 6;

        select '(${exception_name})---src verify';
        select * from ${src_table_name} order by (pt, key);
        select '(${exception_name})---tgt verify';
        select * from ${tgt_table_name} order by (pt, key);
EOF
`
    ${CLICKHOUSE_CLIENT} --multiquery --query "${verify_query}"

done