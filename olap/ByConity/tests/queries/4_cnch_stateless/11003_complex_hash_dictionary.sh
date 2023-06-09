#!/usr/bin/env bash
CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh

$CLICKHOUSE_CLIENT --query="CREATE DATABASE IF NOT EXISTS test;"
$CLICKHOUSE_CLIENT --query="DROP TABLE IF EXISTS test.table_for_complex_hash_dict;"
$CLICKHOUSE_CLIENT --query="CREATE TABLE test.table_for_complex_hash_dict(k1 String, k2 Int32, a UInt64, b Int32, c String) ENGINE = CnchMergeTree() ORDER BY (k1, k2);"

$CLICKHOUSE_CLIENT --query="INSERT INTO test.table_for_complex_hash_dict VALUES (toString(1), 3, 100, -100, 'clickhouse'), (toString(2), -1, 3, 4, 'database'), (toString(5), -3, 6, 7, 'columns'), (toString(10), -20, 9, 8, '');"
$CLICKHOUSE_CLIENT --query="INSERT INTO test.table_for_complex_hash_dict SELECT toString(number), number + 1, 0, -1, 'a' FROM system.numbers WHERE number NOT IN (1, 2, 5, 10) LIMIT 370;"
$CLICKHOUSE_CLIENT --query="INSERT INTO test.table_for_complex_hash_dict SELECT toString(number), number + 10, 0, -1, 'b' FROM system.numbers WHERE number NOT IN (1, 2, 5, 10) LIMIT 370, 370;"
$CLICKHOUSE_CLIENT --query="INSERT INTO test.table_for_complex_hash_dict SELECT toString(number), number + 100, 0, -1, 'c' FROM system.numbers WHERE number NOT IN (1, 2, 5, 10) LIMIT 700, 370;"

$CLICKHOUSE_CLIENT --query="DROP DICTIONARY IF EXISTS test.dict_complex_hash;"
$CLICKHOUSE_CLIENT --query="CREATE DICTIONARY test.dict_complex_hash(k1 String, k2 Int32, a UInt64 DEFAULT 0, b Int32 DEFAULT -1, c String DEFAULT 'none') PRIMARY KEY k1, k2 SOURCE(CLICKHOUSE(HOST '$CLICKHOUSE_HOST' PORT '$CLICKHOUSE_PORT_TCP' USER 'default' TABLE 'table_for_complex_hash_dict' PASSWORD '' DB 'test')) LIFETIME(MIN 1000 MAX 2000) LAYOUT(COMPLEX_KEY_HASHED());"

$CLICKHOUSE_CLIENT --query="SELECT dictGetUInt64('test.dict_complex_hash', 'a', tuple('1', toInt32(3)));"
$CLICKHOUSE_CLIENT --query="SELECT dictGetInt32('test.dict_complex_hash', 'b', tuple('1', toInt32(3)));"
$CLICKHOUSE_CLIENT --query="SELECT dictGetString('test.dict_complex_hash', 'c', tuple('1', toInt32(3)));"

$CLICKHOUSE_CLIENT --query="SELECT dictGetUInt64('test.dict_complex_hash', 'a', tuple('1', toInt32(3)));"
$CLICKHOUSE_CLIENT --query="SELECT dictGetInt32('test.dict_complex_hash', 'b', tuple('1', toInt32(3)));"
$CLICKHOUSE_CLIENT --query="SELECT dictGetString('test.dict_complex_hash', 'c', tuple('1', toInt32(3)));"

$CLICKHOUSE_CLIENT --query="SELECT dictGetUInt64('test.dict_complex_hash', 'a', tuple('2', toInt32(-1)));"
$CLICKHOUSE_CLIENT --query="SELECT dictGetInt32('test.dict_complex_hash', 'b', tuple('2', toInt32(-1)));"
$CLICKHOUSE_CLIENT --query="SELECT dictGetString('test.dict_complex_hash', 'c', tuple('2', toInt32(-1)));"

$CLICKHOUSE_CLIENT --query="SELECT dictGetUInt64('test.dict_complex_hash', 'a', tuple('5', toInt32(-3)));"
$CLICKHOUSE_CLIENT --query="SELECT dictGetInt32('test.dict_complex_hash', 'b', tuple('5', toInt32(-3)));"
$CLICKHOUSE_CLIENT --query="SELECT dictGetString('test.dict_complex_hash', 'c', tuple('5', toInt32(-3)));"

$CLICKHOUSE_CLIENT --query="SELECT dictGetUInt64('test.dict_complex_hash', 'a', tuple('10', toInt32(-20)));"
$CLICKHOUSE_CLIENT --query="SELECT dictGetInt32('test.dict_complex_hash', 'b', tuple('10', toInt32(-20)));"
$CLICKHOUSE_CLIENT --query="SELECT dictGetString('test.dict_complex_hash', 'c', tuple('10', toInt32(-20)));"

$CLICKHOUSE_CLIENT --query="DROP DICTIONARY IF EXISTS test.dict_complex_hash;"
$CLICKHOUSE_CLIENT --query="DROP TABLE IF EXISTS test.table_for_complex_hash_dict;"
