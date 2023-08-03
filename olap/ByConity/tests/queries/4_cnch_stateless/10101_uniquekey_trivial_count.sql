DROP TABLE IF EXISTS u10101;

CREATE TABLE u10101 (d Date, k1 Int64, c1 Int64, c2 Int64) ENGINE = CnchMergeTree() PARTITION BY d ORDER BY k1 UNIQUE KEY k1;

INSERT INTO u10101 SELECT '2021-03-01', number, number + 1, 1 FROM system.numbers LIMIT 100000;
SELECT count() FROM u10101;

-- update a small amount of rows
INSERT INTO u10101 SELECT '2021-03-01', number, 0, 0 FROM system.numbers LIMIT 10;
SELECT 'after update key 0~9';
SELECT count() FROM u10101 SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE d = '2021-03-01' SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE toYear(d) = '2021'  SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE toYear(d) = '2022'  SETTINGS max_rows_to_read = 1;

INSERT INTO u10101 SELECT '2021-03-01', number + 65530, 0, 0 FROM system.numbers LIMIT 10;
SELECT 'after update key 65530~65539';
SELECT count() FROM u10101 SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE d = '2021-03-01' SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE toYear(d) = '2021'  SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE toYear(d) = '2022'  SETTINGS max_rows_to_read = 1;

-- update a large amount of rows
INSERT INTO u10101 SELECT '2021-03-01', number, 0, 0 FROM (SELECT number FROM system.numbers LIMIT 100000) WHERE number % 2 = 1;
SELECT 'after update all odd kyes';
SELECT count() FROM u10101 SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE d = '2021-03-01' SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE toYear(d) = '2021'  SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE toYear(d) = '2022'  SETTINGS max_rows_to_read = 1;

INSERT INTO u10101 SELECT '2021-03-01', number + 1000, 0, 0 FROM system.numbers LIMIT 1000;
SELECT 'after update key 1000~1999';
SELECT count() FROM u10101 SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE d = '2021-03-01' SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE toYear(d) = '2021'  SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE toYear(d) = '2022'  SETTINGS max_rows_to_read = 1;

-- update a small amount of rows again
INSERT INTO u10101 SELECT '2021-03-01', number + 10, 0, 0 FROM system.numbers LIMIT 10;
SELECT 'after update key 10~19';
SELECT count() FROM u10101 SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE d = '2021-03-01' SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE toYear(d) = '2021'  SETTINGS max_rows_to_read = 1;
SELECT count() FROM u10101 WHERE toYear(d) = '2022'  SETTINGS max_rows_to_read = 1;

DROP TABLE IF EXISTS u10101;