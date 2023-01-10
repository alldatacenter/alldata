DROP TABLE IF EXISTS test_same_part;

CREATE TABLE test_same_part
(
    `name` String,
    `id` Bigint
)
ENGINE = CnchHive(`data.olap.cnch_hms.service.lf`, `cnch_hive_external_table`, `ci_test`)
PARTITION BY id;

-- table test_ci in different partition exist same part name
-- maybe this sql cannot fullly explain this issue
-- TODO: use system table show the same part
SELECT * FROM test_same_part ORDER BY name;

DROP TABLE IF EXISTS test_same_part;
