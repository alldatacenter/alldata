DROP TABLE IF EXISTS u10103;

CREATE TABLE u10103 (d Date, k1 Int64, c1 Int64, c2 Int64) ENGINE = CnchMergeTree() PARTITION BY d ORDER BY k1 UNIQUE KEY k1;

INSERT INTO u10103 SELECT '2021-03-01', number, number + 1, 1 FROM system.numbers LIMIT 100000;
SELECT d, count(1), sum(k1), sum(c1), sum(c2) FROM u10103 GROUP BY d;

-- update a small amount of rows
INSERT INTO u10103 SELECT '2021-03-01', number, 0, 0 FROM system.numbers LIMIT 10;
SELECT 'after update key 0~9';
SELECT d, count(1), sum(k1), sum(c1), sum(c2), sum(if(c2 = 1, c1, k1 + 1)) FROM u10103 GROUP BY d;

INSERT INTO u10103 SELECT '2021-03-01', number + 65530, 0, 0 FROM system.numbers LIMIT 10;
SELECT 'after update key 65530~65539';
SELECT d, count(1), sum(k1), sum(c1), sum(c2), sum(if(c2 = 1, c1, k1 + 1)) FROM u10103 GROUP BY d;

-- update a large amount of rows
INSERT INTO u10103 SELECT '2021-03-01', number, 0, 0 FROM (SELECT number FROM system.numbers LIMIT 100000) WHERE number % 2 = 1;
SELECT 'after update all odd kyes';
SELECT d, count(1), sum(k1), sum(c1), sum(c2), sum(if(c2 = 1, c1, k1 + 1)) FROM u10103 GROUP BY d;

INSERT INTO u10103 SELECT '2021-03-01', number + 1000, 0, 0 FROM system.numbers LIMIT 1000;
SELECT 'after update key 1000~1999';
SELECT d, count(1), sum(k1), sum(c1), sum(c2), sum(if(c2 = 1, c1, k1 + 1)) FROM u10103 GROUP BY d;

-- update a small amount of rows again
INSERT INTO u10103 SELECT '2021-03-01', number + 10, 0, 0 FROM system.numbers LIMIT 10;
SELECT 'after update key 10~19';
SELECT d, count(1), sum(k1), sum(c1), sum(c2), sum(if(c2 = 1, c1, k1 + 1)) FROM u10103 GROUP BY d;

DROP TABLE IF EXISTS u10103;
