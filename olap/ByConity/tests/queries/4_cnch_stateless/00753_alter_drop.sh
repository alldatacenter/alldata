#!/usr/bin/env bash
set -e

CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh


${CLICKHOUSE_CLIENT} --query "DROP TABLE IF EXISTS alter_drop;"
${CLICKHOUSE_CLIENT} --query "CREATE TABLE alter_drop (c1 Date, c2 UInt32, c3 String) ENGINE = CnchMergeTree ORDER BY tuple() PARTITION BY (c1, c2);"
${CLICKHOUSE_CLIENT} --query "INSERT INTO alter_drop VALUES ('2021-10-01', 1001, 'test1'), ('2021-10-01', 1002, 'test1_2') ('2021-10-02', 1002, 'test2'), ('2021-10-03', 1003, 'test3'), ('2021-10-03', 1004, 'test4');"
${CLICKHOUSE_CLIENT} --query "SELECT * FROM alter_drop ORDER BY c3;"
echo "DROP PARTITION WHERE"
${CLICKHOUSE_CLIENT} --query "ALTER TABLE alter_drop DROP PARTITION WHERE c1 = '2021-10-01';"
${CLICKHOUSE_CLIENT} --query "SELECT * FROM alter_drop ORDER BY c3;"
echo "DROP PARTITION with value"
${CLICKHOUSE_CLIENT} --query "ALTER TABLE alter_drop DROP PARTITION ('2021-10-03', 1003);"
${CLICKHOUSE_CLIENT} --query "SELECT * FROM alter_drop ORDER BY c3;"
echo "DROP PARTITION WITH ID"
${CLICKHOUSE_CLIENT} --query "ALTER TABLE alter_drop DROP PARTITION ID '20211002-1002';"
${CLICKHOUSE_CLIENT} --query "SELECT * FROM alter_drop ORDER BY c3;"
echo "DROP PART"
PART=$(${CLICKHOUSE_CLIENT} --query "SELECT _part FROM alter_drop;") # should only have 1 row
${CLICKHOUSE_CLIENT} --query "ALTER TABLE alter_drop DROP PART '${PART}';"
${CLICKHOUSE_CLIENT} --query "SELECT * FROM alter_drop ORDER BY c3;"
