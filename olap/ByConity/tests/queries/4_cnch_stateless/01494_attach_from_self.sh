#!/usr/bin/env bash

set -e

CURDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. $CURDIR/../shell_config.sh

# Attach from self's detached partition
$CLICKHOUSE_CLIENT --multiquery <<'EOF'


DROP TABLE IF EXISTS test_attach_detached_from_self;
CREATE TABLE test_attach_detached_from_self (key Int, value Int) ENGINE = CnchMergeTree() PARTITION BY key ORDER BY (key, value);

INSERT INTO test_attach_detached_from_self VALUES(0, 0) (0, 1) (0, 2) (1, 0) (1, 1) (1, 2);

SELECT '---';
SELECT * FROM test_attach_detached_from_self ORDER BY (key, value);
SELECT '---';
ALTER TABLE test_attach_detached_from_self DETACH PARTITION 0;
SELECT '---';
SELECT * from test_attach_detached_from_self ORDER BY (key, value);
SELECT '---';
ALTER TABLE test_attach_detached_from_self ATTACH PARTITION 0;
SELECT '---';
SELECT * from test_attach_detached_from_self ORDER BY (key, value);
SELECT '---';

DROP TABLE test_attach_detached_from_self;

EOF

# Attach from self's detached part
$CLICKHOUSE_CLIENT --multiquery << 'EOF'


DROP TABLE IF EXISTS test_attach_from_self_detached_part;
CREATE TABLE test_attach_from_self_detached_part (key Int, value Int) ENGINE = CnchMergeTree() PARTITION BY key ORDER BY (key, value);

INSERT INTO test_attach_from_self_detached_part VALUES(0, 0);
INSERT INTO test_attach_from_self_detached_part VALUES(1, 1);

EOF

# PART_PATH=$(${CLICKHOUSE_CLIENT} -q "SELECT hdfs_path FROM system.cnch_parts WHERE database='test' AND table='test_attach_from_self_detached_part' AND partition='0'")
# PART_NAME=$(basename ${PART_PATH})

$CLICKHOUSE_CLIENT --multiquery << EOF


SELECT '---';
SELECT * FROM test_attach_from_self_detached_part ORDER BY (key, value);
SELECT '---';

ALTER TABLE test_attach_from_self_detached_part DETACH PARTITION 0;

SELECT '---';
SELECT * FROM test_attach_from_self_detached_part ORDER BY (key, value);
SELECT '---';

ALTER TABLE test_attach_from_self_detached_part DETACH PARTITION 1;

SELECT '---';
SELECT * FROM test_attach_from_self_detached_part ORDER BY (key, value);
SELECT '---';

DROP TABLE test_attach_from_self_detached_part;

EOF