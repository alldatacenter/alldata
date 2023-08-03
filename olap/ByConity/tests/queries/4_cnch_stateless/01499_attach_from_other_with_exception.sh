#!/usr/bin/env bash

set -e

CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh

source ${CURDIR}/attach_common.func

echo "TEST ATTACH FROM OTHER TABLE'S ACTIVE"

# We only cover partial of test, since testing all will takes a lot of time,
# and easily lead to ci timeout
check_list=("before_collect_parts" "rows_assert_fail" "move_part_fail" "mid_commit_fail" "after_commit_fail")
for exception_name in ${check_list[*]}; do
    echo "TEST ATTACH FROM OTHER TABLE'S ACTIVE WITH EXCEPTION ${exception_name}"

    src_table_name="afo_active_with_except_src_${exception_name}"
    tgt_table_name="afo_active_with_except_tgt_${exception_name}"
    prepare_query=`cat <<EOF
        drop table if exists ${src_table_name};
        drop table if exists ${tgt_table_name};

        create table ${src_table_name} (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);
        create table ${tgt_table_name} (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);

        insert into ${src_table_name} values (1, 1)(1, 2)(2, 3)(2, 4);
        insert into ${tgt_table_name} values (1, 5)(2, 6)(3, 7);

        select '(${exception_name})---src_init_data';
        select * from ${src_table_name} order by(pt, key);
        select '(${exception_name})---tgt_init_data';
        select * from ${tgt_table_name} order by(pt, key);
EOF
`
    ${CLICKHOUSE_CLIENT} --multiquery --query "${prepare_query}"

    set +e

    result=$(${CLICKHOUSE_CLIENT} --query "alter table ${tgt_table_name} attach partition 2 from ${src_table_name} settings attach_failure_injection_knob=${all_exception_points[${exception_name}]}, async_post_commit=0" 2>&1)

    echo $([ $? -eq 0 ] && echo "attach success" || echo "attach fail")
    injected_exception_count=$(echo "${result}" | grep -c "Injected exception")
    echo "Has injected exception: $([ ${injected_exception_count} -gt 0 ] && echo "True" || echo "False")"

    set -e

    verify_query=`cat <<EOF
        select '(${exception_name})---src after attach';
        select * from ${src_table_name} order by(pt, key);
        select '(${exception_name})---tgt after attach';
        select * from ${tgt_table_name} order by(pt, key);

        alter table ${src_table_name} attach partition 2;
        alter table ${tgt_table_name} attach partition 2;

        select '(${exception_name})---src verify';
        select * from ${src_table_name} order by(pt, key);
        select '(${exception_name})---tgt verify';
        select * from ${tgt_table_name} order by(pt, key);
EOF
`
    ${CLICKHOUSE_CLIENT} --multiquery --query "${verify_query}"

done

echo ""
echo "TEST ATTACH FROM OTHER TABLE'S DETACHED"

check_list=("load_part" "before_commit_fail" "mid_commit_fail")
for exception_name in ${check_list[*]}; do
    echo "TEST ATTACH FROM OTHER TABLE'S DETACHED PARTITION WITH EXCEPTION ${exception_name}"

    src_table_name="afo_detached_with_except_src_${exception_name}"
    tgt_table_name="afo_detached_with_except_tgt_${exception_name}"
    prepare_query=`cat <<EOF
        drop table if exists ${src_table_name};
        drop table if exists ${tgt_table_name};    

        create table ${src_table_name} (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);
        create table ${tgt_table_name} (pt Int, key Int) engine = CnchMergeTree partition by pt order by (pt, key);

        insert into ${src_table_name} values (3, 1)(3, 2)(4, 3)(4, 4);
        insert into ${tgt_table_name} values (3, 5)(4, 6)(5, 7);

        select '(${exception_name})---src_init_data';
        select * from ${src_table_name} order by (pt, key);
        select '(${exception_name})---tgt_init_data';
        select * from ${tgt_table_name} order by (pt, key);

        alter table ${src_table_name} detach partition 3;
        alter table ${src_table_name} detach partition 4;

        select '(${exception_name})---src after detach';
        select * from ${src_table_name} order by (pt, key);
        select '(${exception_name})---tgt after detach';
        select * from ${tgt_table_name} order by (pt, key);
EOF
`
    ${CLICKHOUSE_CLIENT} --multiquery --query "${prepare_query}"

    set +e

    result=$(${CLICKHOUSE_CLIENT} --query "alter table ${tgt_table_name} attach detached partition 4 from ${src_table_name} settings attach_failure_injection_knob=${all_exception_points[${exception_name}]}, async_post_commit=0" 2>&1)

    echo $([ $? -eq 0 ] && echo "attach success" || echo "attach fail")
    injected_exception_count=$(echo "${result}" | grep -c "Injected exception")
    echo "Has injected exception: $([ ${injected_exception_count} -gt 0 ] && echo "True" || echo "False")"
    
    set -e

    verify_query=`cat <<EOF
        select '(${exception_name})---src after attach';
        select * from ${src_table_name} order by (pt, key);
        select '(${exception_name})---tgt after attach';
        select * from ${tgt_table_name} order by (pt, key);

        alter table ${src_table_name} attach partition 3;
        alter table ${src_table_name} attach partition 4;
        alter table ${tgt_table_name} attach partition 3;
        alter table ${tgt_table_name} attach partition 4;

        select '(${exception_name})---src verify';
        select * from ${src_table_name} order by (pt, key);
        select '(${exception_name})---tgt verify';
        select * from ${tgt_table_name} order by (pt, key);
EOF
`
    ${CLICKHOUSE_CLIENT} --multiquery --query "${verify_query}"

done
