

-- Test Attach other table's detached partition
DROP TABLE IF EXISTS test_attach_other_tbl_detached_src;
DROP TABLE IF EXISTS test_attach_other_tbl_detached_dest;

CREATE TABLE test_attach_other_tbl_detached_src (key Int, value Int) ENGINE = CnchMergeTree() PARTITION BY (key) ORDER BY (key, value);
CREATE TABLE test_attach_other_tbl_detached_dest (key Int, value Int) ENGINE = CnchMergeTree() PARTITION BY (key) ORDER BY (key, value);

INSERT INTO test_attach_other_tbl_detached_src VALUES (0, 0) (0, 1) (0, 2) (1, 0) (1, 1) (1, 2);

SELECT '---';
SELECT * FROM test_attach_other_tbl_detached_src ORDER BY (key, value);
SELECT '---';
SELECT * FROM test_attach_other_tbl_detached_dest ORDER BY (key, value);
SELECT '---';

ALTER TABLE test_attach_other_tbl_detached_src DETACH PARTITION 0;

SELECT '---';
SELECT * FROM test_attach_other_tbl_detached_src ORDER BY (key, value);
SELECT '---';
SELECT * FROM test_attach_other_tbl_detached_dest ORDER BY (key, value);
SELECT '---';

ALTER TABLE test_attach_other_tbl_detached_dest ATTACH DETACHED PARTITION 0 FROM test_attach_other_tbl_detached_src;

SELECT '---';
SELECT * FROM test_attach_other_tbl_detached_src ORDER BY (key, value);
SELECT '---';
SELECT * FROM test_attach_other_tbl_detached_dest ORDER BY (key, value);
SELECT '---';

DROP TABLE test_attach_other_tbl_detached_src;
DROP TABLE test_attach_other_tbl_detached_dest;

-- Test attach parts from other table's non detached
DROP TABLE IF EXISTS test_attach_from_others_active_0;
DROP TABLE IF EXISTS test_attach_from_others_active_1;

CREATE TABLE test_attach_from_others_active_0(key Int, value Int) ENGINE = CnchMergeTree() partition by key ORDER BY (key, value);
CREATE TABLE test_attach_from_others_active_1(key Int, value Int) ENGINE = CnchMergeTree() partition by key ORDER BY (key, value);

INSERT INTO test_attach_from_others_active_0 VALUES (0, 0) (0, 1) (0, 2) (1, 0) (1, 1) (1, 2);

SELECT '---';
SELECT * FROM test_attach_from_others_active_0 ORDER BY (key, value);
SELECT '---';
SELECT * FROM test_attach_from_others_active_1 ORDER BY (key, value);
SELECT '---';

ALTER TABLE test_attach_from_others_active_1 ATTACH PARTITION 0 FROM test_attach_from_others_active_0;

SELECT '---';
SELECT * FROM test_attach_from_others_active_0 ORDER BY (key, value);
SELECT '---';
SELECT * FROM test_attach_from_others_active_1 ORDER BY (key, value);
SELECT '---';

DROP TABLE test_attach_from_others_active_0;
DROP TABLE test_attach_from_others_active_1;

-- Test replace partition

DROP TABLE IF EXISTS test_replace_from_others_active_0;
DROP TABLE IF EXISTS test_replace_from_others_active_1;

CREATE TABLE test_replace_from_others_active_0(key Int, value Int) ENGINE = CnchMergeTree() partition by key ORDER BY (key, value);
CREATE TABLE test_replace_from_others_active_1(key Int, value Int) ENGINE = CnchMergeTree() partition by key ORDER BY (key, value);

INSERT INTO test_replace_from_others_active_0 VALUES(0, 0) (1, 1);
INSERT INTO test_replace_from_others_active_1 VALUES(0, 2) (1, 3);

SELECT '---';
SELECT * FROM test_replace_from_others_active_0 ORDER BY (key, value);
SELECT '---';
SELECT * FROM test_replace_from_others_active_1 ORDER BY (key, value);
SELECT '---';

ALTER TABLE test_replace_from_others_active_1 REPLACE PARTITION 0 FROM test_replace_from_others_active_0;

SELECT '---';
SELECT * FROM test_replace_from_others_active_0 ORDER BY (key, value);
SELECT '---';
SELECT * FROM test_replace_from_others_active_1 ORDER BY (key, value);
SELECT '---';

DROP TABLE test_replace_from_others_active_0;
DROP TABLE test_replace_from_others_active_1;
