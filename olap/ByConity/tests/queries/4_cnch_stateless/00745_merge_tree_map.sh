#!/usr/bin/env bash
set -e

CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh

${CLICKHOUSE_CLIENT} --query "DROP TABLE IF EXISTS merge_map"

${CLICKHOUSE_CLIENT} --query "CREATE TABLE merge_map (n UInt8, m Map(String, String)) Engine=CnchMergeTree ORDER BY n"
${CLICKHOUSE_CLIENT} --query "INSERT INTO merge_map VALUES (1, {'k1': 'v1'})"
${CLICKHOUSE_CLIENT} --query "SELECT n, m{'k1'} FROM merge_map"

${CLICKHOUSE_CLIENT} --query "INSERT INTO merge_map VALUES (2, {'k2': 'v2'})"
${CLICKHOUSE_CLIENT} --query "SELECT n, m{'k1'} FROM merge_map ORDER BY n"

${CLICKHOUSE_CLIENT} --query "SELECT n, m{'k2'} FROM merge_map ORDER BY n"

${CLICKHOUSE_CLIENT} --query "ALTER TABLE merge_map ADD COLUMN ma Map(String, Array(String))"

${CLICKHOUSE_CLIENT} --query "INSERT INTO merge_map VALUES (3, {'k3': 'v3', 'k3.1': 'v3.1'}, {'k3': ['v3.1', 'v3.2']})"
${CLICKHOUSE_CLIENT} --query "SELECT n, m{'k3'}, ma{'k3'} FROM merge_map ORDER BY n"

${CLICKHOUSE_CLIENT} --query "ALTER TABLE merge_map DROP COLUMN ma"
${CLICKHOUSE_CLIENT} --query "DESC TABLE merge_map"

${CLICKHOUSE_CLIENT} --query "SELECT n, mapKeys(m) FROM merge_map ORDER BY n"

${CLICKHOUSE_CLIENT} --query "DROP TABLE merge_map"

${CLICKHOUSE_CLIENT} --query "CREATE TABLE merge_map (n UInt8, m Map(String, String) KV) Engine=CnchMergeTree ORDER BY n"
${CLICKHOUSE_CLIENT} --query "INSERT INTO merge_map VALUES (1, {'k1': 'v1'})"
${CLICKHOUSE_CLIENT} --query "SELECT n, m{'k1'} FROM merge_map"

${CLICKHOUSE_CLIENT} --query "INSERT INTO merge_map VALUES (2, {'k2': 'v2'})"
${CLICKHOUSE_CLIENT} --query "SELECT n, m{'k1'} FROM merge_map ORDER BY n"

${CLICKHOUSE_CLIENT} --query "SELECT n, m{'k2'} FROM merge_map ORDER BY n"

${CLICKHOUSE_CLIENT} --query "ALTER TABLE merge_map ADD COLUMN ma Map(String, Array(String)) KV"

${CLICKHOUSE_CLIENT} --query "INSERT INTO merge_map VALUES (3, {'k3': 'v3', 'k3.1': 'v3.1'}, {'k3': ['v3.1', 'v3.2']})"
${CLICKHOUSE_CLIENT} --query "SELECT n, m{'k3'}, ma{'k3'} FROM merge_map ORDER BY n"

${CLICKHOUSE_CLIENT} --query "ALTER TABLE merge_map DROP COLUMN ma"
${CLICKHOUSE_CLIENT} --query "DESC TABLE merge_map"

${CLICKHOUSE_CLIENT} --query "SELECT n, mapKeys(m) FROM merge_map ORDER BY n"

${CLICKHOUSE_CLIENT} --query "DROP TABLE merge_map"
