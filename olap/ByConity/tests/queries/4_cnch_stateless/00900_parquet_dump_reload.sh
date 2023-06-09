#!/usr/bin/env bash

# Test for nested data type like array and map. Dump selected data as parquet format and reload.


CUR_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CUR_DIR/../shell_config.sh

${CLICKHOUSE_CLIENT} --query="DROP TABLE IF EXISTS dump_parquet"
${CLICKHOUSE_CLIENT} --query="DROP TABLE IF EXISTS reload_parquet"
${CLICKHOUSE_CLIENT} --query="CREATE TABLE dump_parquet(id UInt32, arr_data Array(Int32), map_data Map(String, String)) ENGINE=CnchMergeTree PARTITION BY id ORDER BY tuple()"
${CLICKHOUSE_CLIENT} --query="CREATE TABLE reload_parquet AS dump_parquet"

${CLICKHOUSE_CLIENT} --query="INSERT INTO dump_parquet VALUES (1001, [1,2,3,4], {'a1':'va1', 'a2':'va2'}), (1002, [5,6], {'b1':'vb1','b2':'vb2','b3':'vb3'})"

${CLICKHOUSE_CLIENT} --query="SELECT * FROM dump_parquet FORMAT Parquet" > ${CLICKHOUSE_TMP}/parquet_dump_reload.parquet
cat ${CLICKHOUSE_TMP}/parquet_dump_reload.parquet | ${CLICKHOUSE_CLIENT} --query="INSERT INTO reload_parquet FORMAT Parquet"
${CLICKHOUSE_CLIENT} --query="SELECT * FROM reload_parquet ORDER BY id"

rm -f ${CLICKHOUSE_TMP}/parquet_dump_reload.parquet
