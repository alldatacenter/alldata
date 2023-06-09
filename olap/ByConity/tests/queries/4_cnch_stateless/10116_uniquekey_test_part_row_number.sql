DROP TABLE IF EXISTS test.part_row_number;

CREATE TABLE test.part_row_number (id Int64, c2 String, c3 Int64) ENGINE=CnchMergeTree order by id unique key id SETTINGS index_granularity=4;

SYSTEM STOP MERGES test.part_row_number;

INSERT INTO test.part_row_number VALUES (10001, 'BJ', 10), (10002, 'SH', 20), (10003, 'BJ', 30), (10004, 'SH',40);

SELECT '---row_number alone----';
SELECT _part_row_number FROM test.part_row_number;

SELECT '---single column----';
SELECT _part_row_number, id FROM test.part_row_number;
SELECT _part_row_number, id FROM test.part_row_number limit 1, 1;


SELECT '---two column----';
SELECT id, _part_row_number, c3 FROM test.part_row_number;

SELECT '---all column----';
SELECT *, _part_row_number FROM test.part_row_number;
SELECT _part_row_number, * FROM test.part_row_number;

INSERT INTO test.part_row_number VALUES (10006, 'BJ', 60), (10005, 'BJ', 50), (10007, 'BJ', 70), (10008, 'SH', 80);

INSERT INTO test.part_row_number VALUES (10009, 'BJ', 90);

SELECT '---all column across granu----';
-- NOTE: order by _part to keep a stable order
select id, c2, c3, _part_row_number from (SELECT *, _part_row_number, _part FROM test.part_row_number order by _part limit 2, 5);

DROP TABLE IF EXISTS test.part_row_number;


