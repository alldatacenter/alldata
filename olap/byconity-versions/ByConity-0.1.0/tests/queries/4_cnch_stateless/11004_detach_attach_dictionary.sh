#!/usr/bin/env bash
CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh

$CLICKHOUSE_CLIENT --query="DROP TABLE IF EXISTS table_for_detach_dict;"
$CLICKHOUSE_CLIENT --query="CREATE TABLE table_for_detach_dict(id UInt64, a UInt64, b Int32, c String) ENGINE = CnchMergeTree() ORDER BY id;"
$CLICKHOUSE_CLIENT --query="INSERT INTO table_for_detach_dict VALUES (1, 100, -100, 'clickhouse'), (2, 3, 4, 'database'), (5, 6, 7, 'columns'), (10, 9, 8, '');"
$CLICKHOUSE_CLIENT --query="INSERT INTO table_for_detach_dict SELECT number, 0, -1, 'a' FROM system.numbers WHERE number NOT IN (1, 2, 5, 10) LIMIT 370;"
$CLICKHOUSE_CLIENT --query="INSERT INTO table_for_detach_dict SELECT number, 0, -1, 'b' FROM system.numbers WHERE number NOT IN (1, 2, 5, 10) LIMIT 370, 370;"
$CLICKHOUSE_CLIENT --query="INSERT INTO table_for_detach_dict SELECT number, 0, -1, 'c' FROM system.numbers WHERE number NOT IN (1, 2, 5, 10) LIMIT 700, 370;"

$CLICKHOUSE_CLIENT --query="CREATE DATABASE IF NOT EXISTS test_dict;"
$CLICKHOUSE_CLIENT --query="CREATE DICTIONARY IF NOT EXISTS test_dict.dict1(id UInt64, a UInt64 DEFAULT 0, b Int32 DEFAULT -1, c String DEFAULT 'none') PRIMARY KEY id SOURCE(CLICKHOUSE(HOST '$CLICKHOUSE_HOST' PORT '$CLICKHOUSE_PORT_TCP' USER 'default' TABLE 'table_for_detach_dict' PASSWORD '' DB 'test')) LIFETIME(MIN 1000 MAX 2000) LAYOUT(FLAT());"

$CLICKHOUSE_CLIENT --query="SHOW DICTIONARIES FROM test_dict;"
$CLICKHOUSE_CLIENT --query="DETACH DICTIONARY test_dict.dict1 PERMANENTLY;"
$CLICKHOUSE_CLIENT --query="SHOW DICTIONARIES FROM test_dict;"
$CLICKHOUSE_CLIENT --query="ATTACH DICTIONARY test_dict.dict1;"
$CLICKHOUSE_CLIENT --query="SHOW DICTIONARIES FROM test_dict;"

$CLICKHOUSE_CLIENT --query="DROP DICTIONARY IF EXISTS test_dict.dict1;"
$CLICKHOUSE_CLIENT --query="DROP DATABASE IF EXISTS test_dict;"

$CLICKHOUSE_CLIENT --query="DROP TABLE IF EXISTS table_for_detach_dict;"
