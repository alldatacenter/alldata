#!/usr/bin/env bash

set -e

CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh

# Attach partition from path
${CLICKHOUSE_CLIENT} --multiquery << EOF


DROP TABLE IF EXISTS test_attach_from_path_partition_src;
DROP TABLE IF EXISTS test_attach_from_path_partition_dst;

CREATE TABLE test_attach_from_path_partition_src (key Int, value Int) ENGINE=CnchMergeTree() PARTITION BY (key) ORDER BY (key, value);
CREATE TABLE test_attach_from_path_partition_dst (key Int, value Int) ENGINE=CnchMergeTree() PARTITION BY (key) ORDER BY (key, value);

INSERT INTO test_attach_from_path_partition_src VALUES(0, 0);
INSERT INTO test_attach_from_path_partition_src VALUES(0, 1);
INSERT INTO test_attach_from_path_partition_src VALUES(1, 2);
INSERT INTO test_attach_from_path_partition_src VALUES(1, 3);

SELECT '---';
SELECT * FROM test_attach_from_path_partition_src ORDER BY (key, value);
SELECT '---';
SELECT * FROM test_attach_from_path_partition_dst ORDER BY (key, value);
SELECT '---';

EOF

# SRC_PART_PATH=$(${CLICKHOUSE_CLIENT} -q "SELECT hdfs_path FROM system.cnch_parts WHERE database = 'test' AND table = 'test_attach_from_path_partition_src' LIMIT 1")
# SRC_PATH=$(dirname ${SRC_PART_PATH})

${CLICKHOUSE_CLIENT} --multiquery << EOF



DROP TABLE test_attach_from_path_partition_src;

DROP TABLE test_attach_from_path_partition_dst;

EOF

# Attach parts from path
${CLICKHOUSE_CLIENT} --multiquery << EOF


DROP TABLE IF EXISTS test_attach_from_path_parts_src;
DROP TABLE IF EXISTS test_attach_from_path_parts_dst;

CREATE TABLE test_attach_from_path_parts_src (key Int, value Int) ENGINE=CnchMergeTree() PARTITION BY (key) ORDER BY (key, value);
CREATE TABLE test_attach_from_path_parts_dst (key Int, value Int) ENGINE=CnchMergeTree() PARTITION BY (key) ORDER BY (key, value);

INSERT INTO test_attach_from_path_parts_src VALUES(0, 0);
INSERT INTO test_attach_from_path_parts_src VALUES(1, 1);

SELECT '---';
SELECT * FROM test_attach_from_path_parts_src ORDER BY (key, value);
SELECT '---';
SELECT * FROM test_attach_from_path_parts_dst ORDER BY (key, value);
SELECT '---';

EOF

# SRC_PART_PATH=$(${CLICKHOUSE_CLIENT} -q "SELECT hdfs_path FROM system.cnch_parts WHERE database = 'test' AND table = 'test_attach_from_path_parts_src' LIMIT 1")
# SRC_PATH=$(dirname ${SRC_PART_PATH})

${CLICKHOUSE_CLIENT} --multiquery << EOF



DROP TABLE test_attach_from_path_parts_src;

DROP TABLE test_attach_from_path_parts_dst;

EOF
