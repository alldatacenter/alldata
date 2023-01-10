DROP DATABASE IF EXISTS truncate_test;
DROP TABLE IF EXISTS truncate_test.test_merge_tree;

CREATE DATABASE truncate_test;
CREATE TABLE truncate_test.test_merge_tree(p Date, k UInt64) ENGINE = CnchMergeTree() PARTITION BY toYYYYMM(p) ORDER BY k SETTINGS index_granularity = 1;

SELECT '======Before Truncate======';
INSERT INTO truncate_test.test_merge_tree VALUES('2000-01-01', 1);
SELECT * FROM truncate_test.test_merge_tree;

SELECT '======After Truncate And Empty======';
TRUNCATE TABLE truncate_test.test_merge_tree;
SELECT * FROM truncate_test.test_merge_tree;

SELECT '======After Truncate And Insert Data======';
INSERT INTO truncate_test.test_merge_tree VALUES('2000-01-01', 1);
SELECT * FROM truncate_test.test_merge_tree;

DROP TABLE IF EXISTS truncate_test.test_merge_tree;
DROP DATABASE IF EXISTS truncate_test;
