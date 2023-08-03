DROP TABLE IF EXISTS u10107_common;

CREATE TABLE u10107_common (d Date, k1 String, k2 Int64, k3 String, c1 Int64, c2 String, c3 Float64) ENGINE = CnchMergeTree() PARTITION BY d ORDER BY k1 UNIQUE KEY (k2, sipHash64(k3));

-- drop key column is not allowed
ALTER TABLE u10107_common DROP COLUMN k1; -- { serverError 47 }
ALTER TABLE u10107_common DROP COLUMN k2; -- { serverError 524 }
ALTER TABLE u10107_common DROP COLUMN k3; -- { serverError 524 }
ALTER TABLE u10107_common DROP COLUMN c3;
SELECT 'After drop c3';
DESC u10107_common;

INSERT INTO u10107_common VALUES ('20210101', 'k1', 1, 'k3', 10, '1');
INSERT INTO u10107_common VALUES ('20210102', 'k1', 2, 'k3', 20, '2');
INSERT INTO u10107_common VALUES ('20210103', 'k1', 3, 'k3', 30, '3');

-- modify/rename/clear key column is not allowed
ALTER TABLE u10107_common MODIFY COLUMN k2 String; -- { serverError 524 }
ALTER TABLE u10107_common RENAME COLUMN k2 TO k4; -- { serverError 524 }

-- add column with default values
ALTER TABLE u10107_common ADD COLUMN c3 String DEFAULT 'N/A';
SELECT 'After add c3';
DESC u10107_common;
INSERT INTO u10107_common VALUES ('20210101', 'k1', 4, 'k3', 40, '4', 'c3');
SELECT * FROM u10107_common ORDER BY d, k2;

-- add column without default
ALTER TABLE u10107_common DROP COLUMN c2, DROP COLUMN c3;
ALTER TABLE u10107_common ADD COLUMN c4 Int32;
SELECT 'After drop c2, c3 and add c4 int32';
DESC u10107_common;
INSERT INTO u10107_common VALUES ('20210101', 'k1', 4, 'k3', 40, 4), ('20210102', 'k1', 5, 'k3', 50, 5);
SELECT * FROM u10107_common ORDER BY d, k2;

-- comment column
ALTER TABLE u10107_common COMMENT COLUMN c4 'comment for c4';
SELECT 'After comment c4';
DESC u10107_common;

-- rename column
ALTER TABLE u10107_common RENAME COLUMN c1 TO c3;
SELECT 'After rename c1 to c3';
DESC u10107_common;

DROP TABLE IF EXISTS u10107_common;
