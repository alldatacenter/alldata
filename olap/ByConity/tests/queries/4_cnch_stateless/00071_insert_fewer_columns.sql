DROP TABLE IF EXISTS insert_fewer_columns;
CREATE TABLE insert_fewer_columns (a UInt8, b UInt8) Engine = CnchMergeTree ORDER BY tuple();
INSERT INTO insert_fewer_columns (a) VALUES (1), (2);
SELECT * FROM insert_fewer_columns;

-- Test position arguments in insert.
DROP TABLE IF EXISTS insert_fewer_columns_2;
CREATE TABLE insert_fewer_columns_2 (b UInt8, a UInt8) Engine = CnchMergeTree ORDER BY tuple();
INSERT INTO insert_fewer_columns_2 SELECT * FROM insert_fewer_columns;
SELECT a, b FROM insert_fewer_columns;
SELECT a, b FROM insert_fewer_columns_2;

DROP TABLE IF EXISTS insert_fewer_columns_2;
DROP TABLE insert_fewer_columns;
