USE test;
DROP TABLE IF EXISTS nested;
CREATE TABLE nested(column Nested(name String,names Array(String),types Array(Enum8('PU' = 1, 'US' = 2, 'OTHER' = 3)))) ENGINE = CnchMergeTree ORDER BY tuple();
SYSTEM START MERGES test.nested;
SYSTEM STOP MERGES test.nested;
INSERT INTO nested VALUES (['Hello', 'World'], [['a'], ['b', 'c']], [['PU', 'US'], ['OTHER']]);
SELECT * FROM nested;
DETACH TABLE nested PERMANENTLY;
ATTACH TABLE nested;
SELECT * FROM nested;
INSERT INTO nested VALUES (['GoodBye'], [['1', '2']], [['PU', 'US', 'OTHER']]);
SELECT * FROM nested ORDER BY column.name;
SYSTEM START MERGES test.nested;
SELECT sleep(3) FORMAT Null;
OPTIMIZE TABLE test.nested PARTITION tuple();
SELECT sleep(3) FORMAT Null;
SELECT sleep(3) FORMAT Null;
SELECT * FROM nested ORDER BY column.name;
DETACH TABLE nested PERMANENTLY;
ATTACH TABLE nested;
SELECT * FROM nested ORDER BY column.name;
DROP TABLE IF EXISTS nested;
CREATE TABLE nested
(
    column Nested
    (
        name String,
        names Array(String),
        types Array(Enum8('PU' = 1, 'US' = 2, 'OTHER' = 3))
    )
) ENGINE = CnchMergeTree ORDER BY column.name;
INSERT INTO nested VALUES (['Hello', 'World'], [['a'], ['b', 'c']], [['PU', 'US'], ['OTHER']]);
SELECT * FROM nested;
DROP TABLE IF EXISTS nested;
CREATE TABLE nested
(
    column Nested
    (
        name String,
        names Array(String),
        types Array(Enum8('PU' = 1, 'US' = 2, 'OTHER' = 3))
    )
) ENGINE = CnchMergeTree ORDER BY column.name;
INSERT INTO nested VALUES (['Hello', 'World'], [['a'], ['b', 'c']], [['PU', 'US'], ['OTHER']]);
SELECT * FROM nested;

DROP TABLE IF EXISTS nested;
CREATE TABLE nested
(
    column Nested
    (
        name String,
        names Array(String),
        types Array(Enum8('PU' = 1, 'US' = 2, 'OTHER' = 3))
    )
) ENGINE = CnchMergeTree ORDER BY column.name;
INSERT INTO nested VALUES (['Hello', 'World'], [['a'], ['b', 'c']], [['PU', 'US'], ['OTHER']]);
SELECT * FROM nested;
DROP TABLE IF EXISTS nested;

CREATE TABLE nested
(
    column Nested
    (
        name String,
        names Array(String),
        types Array(Enum8('PU' = 1, 'US' = 2, 'OTHER' = 3))
    )
) ENGINE = CnchMergeTree ORDER BY column.name;
INSERT INTO nested VALUES (['Hello', 'World'], [['a'], ['b', 'c']], [['PU', 'US'], ['OTHER']]);
SELECT * FROM nested;
DROP TABLE nested;
