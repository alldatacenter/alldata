set max_threads=1;
Use test;
DROP TABLE IF EXISTS enum;
CREATE TABLE enum (x Enum8('Hello' = -100, '\\' = 0, '\t\\t' = 111), y UInt8) 
ENGINE = CnchMergeTree ORDER BY y;
INSERT INTO enum (y) VALUES (0);
SELECT * FROM enum ORDER BY x, y FORMAT PrettyCompactMonoBlock;
INSERT INTO enum (x) VALUES ('\\');
SELECT * FROM enum ORDER BY x, y FORMAT PrettyCompactMonoBlock;
INSERT INTO enum (x) VALUES ('\t\\t');
SELECT * FROM enum ORDER BY x, y FORMAT PrettyCompactMonoBlock;
SELECT x, y, toInt8(x), toString(x) AS s, CAST(toString(x) AS Enum8('Hello' = -100, '\\' = 0, '\t\\t' = 111)) AS casted FROM enum ORDER BY x, y FORMAT PrettyCompactMonoBlock;
DROP TABLE enum;
