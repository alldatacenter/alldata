#!/usr/bin/env bash
CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh

$CLICKHOUSE_CLIENT -q "select name from system.functions format TSV;" > ${CLICKHOUSE_TMP}/SQL_FUZZY_FILE_FUNCTIONS

$CLICKHOUSE_CLIENT -q "select name from system.table_functions format TSV;" > ${CLICKHOUSE_TMP}/SQL_FUZZY_FILE_TABLE_FUNCTIONS

# if you want long run use: python3 $CURDIR/00746_sql_fuzzy.py -temp_path ${CLICKHOUSE_TMP}/  -sql_count 1000
python3 $CURDIR/00746_sql_fuzzy.python -temp_path ${CLICKHOUSE_TMP}/  -sql_count 1000

$CLICKHOUSE_CLIENT --ignore-error --multiquery < ${CLICKHOUSE_TMP}/00746_sql_fuzzy.sql >/dev/null 2>&1
$CLICKHOUSE_CLIENT -q "SELECT 'Still alive'"

# Query replay:
# cat <temp_path>/00746_sql_fuzzy.sql