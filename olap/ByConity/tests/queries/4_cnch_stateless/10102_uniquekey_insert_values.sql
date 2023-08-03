SELECT '# partition level unique';
DROP TABLE IF EXISTS u10102_pl;

CREATE TABLE u10102_pl (d Date, id Int32, s String) ENGINE = CnchMergeTree()
PARTITION BY d ORDER BY s UNIQUE KEY id SETTINGS partition_level_unique_keys = 1;

-- First "START MERGES" to make sure CnchMergeMutateThread is created on server
-- Then "STOP MERGES" to stop CnchMergeMutateThread's background task so that it will not conflict with `OPTIMIZE` query
SYSTEM START MERGES u10102_pl;
SYSTEM STOP MERGES u10102_pl;

SELECT 'Test dedup within block';
INSERT INTO u10102_pl VALUES ('2021-03-01', 1, '1a'), ('2021-03-01', 2, '2a'), ('2021-03-01', 3, '3a'), ('2021-03-01', 1, '1b'), ('2021-04-01', 1, '1a'), ('2021-03-01', 1, '1c'), ('2021-03-01', 3, '3b');
SELECT * FROM u10102_pl ORDER BY d, id;

SELECT 'Test dedup with existing data';
-- insert new keys & update old keys
INSERT INTO u10102_pl VALUES ('2021-03-01', 0, '0a'), ('2021-03-01', 1, '1d'), ('2021-03-01', 1, '1e'), ('2021-03-01', 4, '4a'), ('2021-04-01', 1, '1b');
INSERT INTO u10102_pl VALUES ('2021-03-01', 2, '2b'), ('2021-03-01', 4, '4b');
-- insert new keys
INSERT INTO u10102_pl VALUES ('2021-03-01', 5, '5a'), ('2021-03-01', 6, '6a');
SELECT * FROM u10102_pl ORDER BY d, id;

SELECT 'after merge';
OPTIMIZE TABLE u10102_pl SETTINGS mutations_sync = 1;
SELECT * FROM u10102_pl ORDER BY d, id;

DROP TABLE IF EXISTS u10102_pl;

----------------------------
SELECT '# table level unique';
DROP TABLE IF EXISTS u10102_tl;

CREATE TABLE u10102_tl (d Date, id Int32, s String) ENGINE = CnchMergeTree()
PARTITION BY d ORDER BY s UNIQUE KEY id SETTINGS partition_level_unique_keys = 0;

-- First "START MERGES" to make sure CnchMergeMutateThread is created on server
-- Then "STOP MERGES" to stop CnchMergeMutateThread's background task so that it will not conflict with `OPTIMIZE` query
SYSTEM START MERGES u10102_tl;
SYSTEM STOP MERGES u10102_tl;

SELECT 'Test dedup within block';
INSERT INTO u10102_tl VALUES
('2021-03-01', 1, '1a'), ('2021-03-01', 2, '2a'), ('2021-03-01', 3, '3a'), ('2021-03-01', 4, '4a'), ('2021-02-01', 1, '1b'), ('2021-02-01', 2, '2b'), ('2021-04-01', 3, '3b'), ('2021-03-01', 4, '4b'), ('2021-03-01', 1, '1c'), ('2021-03-01', 4, '4c');
SELECT * FROM u10102_tl ORDER BY id;

SELECT 'Test dedup with existing data';
-- insert new keys & update old keys
INSERT INTO u10102_tl VALUES
('2021-04-01', 1, '1d'), ('2021-04-01', 2, '2d'), ('2021-04-01', 3, '3d'), ('2021-04-01', 4, '4d'), ('2021-04-01', 5, '5d');
-- insert new keys
INSERT INTO u10102_tl VALUES ('2021-04-01', 6, '6d'), ('2021-04-01', 7, '7d'), ('2021-04-01', 8, '8d');
SELECT * FROM u10102_tl ORDER BY id;

SELECT 'after merge';
OPTIMIZE TABLE u10102_tl SETTINGS mutations_sync = 1;
SELECT * FROM u10102_tl ORDER BY id;

DROP TABLE IF EXISTS u10102_tl;

----------------------------
SELECT '# materialized column';
DROP TABLE IF EXISTS u10102_mc;

CREATE TABLE u10102_mc (d Date, id Int32, s String, arr Array(Int32), sum materialized arraySum(arr))
ENGINE = CnchMergeTree PARTITION BY d ORDER BY (s, id) PRIMARY KEY s UNIQUE KEY id;

INSERT INTO u10102_mc VALUES ('2020-10-29', 1001, '1001A', [1,2]), ('2020-10-29', 1002, '1002A', [3,4]), ('2020-10-29', 1001, '1001B', [5,6]), ('2020-10-29', 1001, '1001C', [7,8]);

SELECT d, id, s, arr, sum FROM u10102_mc ORDER BY d, id;

INSERT INTO u10102_mc VALUES ('2020-10-29', 1002, '1002B', [9, 10]), ('2020-10-30', 1001, '1001A', [1,2]), ('2020-10-30', 1002, '1002A', [3,4]), ('2020-10-30', 1001, '1001B', [5,6]);
SELECT d, id, s, arr, sum FROM u10102_mc ORDER BY d, id;

DROP TABLE IF EXISTS u10102_mc;

----------------------------
SELECT '# composite key (partition level)';
DROP TABLE IF EXISTS u10102_pl_composite;

CREATE TABLE u10102_pl_composite (d Date, id Int32, s String, revenue Int32)
ENGINE = CnchMergeTree PARTITION BY d ORDER BY id UNIQUE KEY (id, s);

INSERT INTO u10102_pl_composite VALUES ('2020-10-29', 1001, '1001A', 100), ('2020-10-29', 1002, '1002A', 200), ('2020-10-29', 1001, '1001B', 300), ('2020-10-29', 1001, '1001A', 400);
SELECT d, id, s, revenue FROM u10102_pl_composite ORDER BY d, id, s;

DROP TABLE IF EXISTS u10102_pl_composite;

----------------------------
SELECT '# composite key (table level)';
DROP TABLE IF EXISTS u10102_tl_composite;

CREATE TABLE u10102_tl_composite (d Date, id Int32, s String, arr Array(Int32)) ENGINE = CnchMergeTree()
PARTITION BY d ORDER BY s UNIQUE KEY (id,s)
SETTINGS partition_level_unique_keys=0;

INSERT INTO u10102_tl_composite VALUES ('2020-10-25', 1001, '1001A', [1,1]), ('2020-10-25', 1002, '1002A', [2,1]), ('2020-10-25', 1001, '1001B', [1,2]);
INSERT INTO u10102_tl_composite VALUES ('2020-10-26', 1002, '1002B', [2,2]), ('2020-10-26', 1003, '1003A', [3,1]);
INSERT INTO u10102_tl_composite VALUES ('2020-10-27', 1003, '1003B', [3,2]), ('2020-10-27', 1004, '1004A', [4,1]);

SELECT d, id, s, arr FROM u10102_tl_composite ORDER BY d, id, s;

DROP TABLE IF EXISTS u10102_tl_composite;
