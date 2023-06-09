DROP TABLE IF EXISTS test_read_without_cache;
CREATE TABLE test_read_without_cache(key Int, value String) ENGINE = CnchMergeTree ORDER BY tuple() SETTINGS enable_local_disk_cache = 0;

INSERT INTO test_read_without_cache VALUES(0, '0');
INSERT INTO test_read_without_cache VALUES(1, '1');

SELECT * FROM test_read_without_cache ORDER BY (key, value);
