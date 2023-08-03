#!/usr/bin/env bash
CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh

$CLICKHOUSE_CLIENT --query="CREATE DATABASE IF NOT EXISTS test;"
$CLICKHOUSE_CLIENT --query="DROP TABLE IF EXISTS test.table_for_range_hash_dict;"
$CLICKHOUSE_CLIENT --query="CREATE TABLE test.table_for_range_hash_dict(id UInt64, start Date, end Date, price Float32) ENGINE = CnchMergeTree() ORDER BY id;"
$CLICKHOUSE_CLIENT --query="INSERT INTO test.table_for_range_hash_dict VALUES (1, toDate('2016-01-01'), toDate('2017-01-10'), 100), (2, toDate('2016-02-01'), toDate('2017-02-10'), 200), (3, toDate('2016-03-01'), toDate('2017-03-10'), 300), (4, toDate('2016-04-01'), toDate('2017-04-10'), 400), (5, toDate('2018-05-01'), toDate('2019-08-10'), 500), (6, toDate('2018-06-01'), toDate('2019-06-10'), 600);"

$CLICKHOUSE_CLIENT --query="DROP DICTIONARY IF EXISTS test.dict_range_hash;"
$CLICKHOUSE_CLIENT --query="CREATE DICTIONARY test.dict_range_hash(id UInt64, start Date, end Date, price Float32) PRIMARY KEY id SOURCE(CLICKHOUSE(HOST '$CLICKHOUSE_HOST' PORT '$CLICKHOUSE_PORT_TCP' USER 'default' TABLE 'table_for_range_hash_dict' PASSWORD '' DB 'test')) LIFETIME(MIN 1000 MAX 2000) LAYOUT(RANGE_HASHED()) RANGE(MIN start MAX end);"
$CLICKHOUSE_CLIENT --query="SELECT dictGetFloat32('test.dict_range_hash', 'price', toUInt64(1), toDate('2016-01-02'));"
$CLICKHOUSE_CLIENT --query="SELECT dictGetFloat32('test.dict_range_hash', 'price', toUInt64(2), toDate('2016-02-02'));"
$CLICKHOUSE_CLIENT --query="SELECT dictGetFloat32('test.dict_range_hash', 'price', toUInt64(3), toDate('2016-03-02'));"
$CLICKHOUSE_CLIENT --query="SELECT dictGetFloat32('test.dict_range_hash', 'price', toUInt64(4), toDate('2016-04-02'));"

$CLICKHOUSE_CLIENT --query="DROP DICTIONARY IF EXISTS test.dict_range_hash;"
$CLICKHOUSE_CLIENT --query="DROP TABLE IF EXISTS test.table_for_range_hash_dict;"
