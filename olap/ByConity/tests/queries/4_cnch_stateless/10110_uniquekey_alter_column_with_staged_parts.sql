DROP TABLE IF EXISTS u10110_common;

set enable_staging_area_for_write = 0;
CREATE TABLE u10110_common (d Date, k1 String, k2 Int64, k3 String, c1 Int64, c2 String, c3 Float64) ENGINE = CnchMergeTree() PARTITION BY d ORDER BY k1 UNIQUE KEY (k2, sipHash64(k3));

-- drop key column is not allowed
ALTER TABLE u10110_common DROP COLUMN k1; -- { serverError 47 }
ALTER TABLE u10110_common DROP COLUMN k2; -- { serverError 524 }
ALTER TABLE u10110_common DROP COLUMN k3; -- { serverError 524 }
ALTER TABLE u10110_common DROP COLUMN c3;
SELECT 'After drop c3, table description:';
DESC u10110_common;

SELECT '';
SELECT 'sync insert two rows, stop dedup worker and async insert one row';
INSERT INTO u10110_common VALUES ('20210101', 'k1', 1, 'k3', 10, '1');
INSERT INTO u10110_common VALUES ('20210102', 'k1', 2, 'k3', 20, '2')('20210101', 'k1', 1, 'k3', 15, '1');

set enable_staging_area_for_write = 1;
system start dedup worker u10110_common;
system stop dedup worker u10110_common;
INSERT INTO u10110_common VALUES ('20210102', 'k1', 2, 'k3', 25, '2')('20210103', 'k1', 3, 'k3', 30, '3');
SELECT '#staged parts:', count() FROM system.cnch_staged_parts where database=currentDatabase() and table = 'u10110_common' and to_publish;
SELECT * FROM u10110_common ORDER BY d, k2;


-- modify/rename/clear key column is not allowed
ALTER TABLE u10110_common MODIFY COLUMN k2 String; -- { serverError 524 }
ALTER TABLE u10110_common RENAME COLUMN k2 TO k4; -- { serverError 524 }

-- add column with default values
SELECT '';
ALTER TABLE u10110_common ADD COLUMN c3 String DEFAULT 'N/A';
SELECT 'After add c3, table description:';
DESC u10110_common;
SELECT 'start dedup worker and check whether staged part has added column c2';
system start dedup worker u10110_common;
system sync dedup worker u10110_common;
SELECT '#staged parts:', count() FROM system.cnch_staged_parts where database=currentDatabase() and table = 'u10110_common' and to_publish;
SELECT * FROM u10110_common ORDER BY d, k2;

-- add column without default
SELECT '';
SELECT 'stop dedup worker and async insert one row to test next alter command';
system stop dedup worker u10110_common;
INSERT INTO u10110_common VALUES ('20210101', 'k1', 4, 'k3', 40, '4', 'c3');
SELECT '#staged parts:', count() FROM system.cnch_staged_parts where database=currentDatabase() and table = 'u10110_common' and to_publish;
ALTER TABLE u10110_common DROP COLUMN c2, DROP COLUMN c3;
ALTER TABLE u10110_common ADD COLUMN c4 Int32;
SELECT 'After drop c2, c3 and add c4 int32, table description:';
DESC u10110_common;
SELECT 'start dedup worker and check whether staged part has applied alter commands';
system start dedup worker u10110_common;
system sync dedup worker u10110_common;
SELECT '#staged parts:', count() FROM system.cnch_staged_parts where database=currentDatabase() and table = 'u10110_common' and to_publish;
SELECT * FROM u10110_common ORDER BY d, k2;


-- comment column
SELECT '';
ALTER TABLE u10110_common COMMENT COLUMN c4 'comment for c4';
SELECT 'After comment c4';
DESC u10110_common;

DROP TABLE IF EXISTS u10110_common;
