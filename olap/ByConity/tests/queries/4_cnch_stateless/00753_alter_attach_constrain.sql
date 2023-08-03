DROP TABLE IF EXISTS alter_attach1;
CREATE TABLE alter_attach1 (c1 Date, c2 UInt32, c3 String) ENGINE = CnchMergeTree ORDER BY tuple() PARTITION BY (c1, c2);
INSERT INTO alter_attach1 VALUES ('2021-10-01', 1001, 'test1'), ('2021-10-02', 1002, 'test2'), ('2021-10-03', 1003, 'test3');

--1. Test normal case: can attach from table with same data structure;
DROP TABLE IF EXISTS alter_attach2;
CREATE TABLE alter_attach2 (c1 Date, c2 UInt32, c3 String) ENGINE = CnchMergeTree ORDER BY tuple() PARTITION BY (c1, c2);
ALTER TABLE alter_attach1 DETACH PARTITION ('2021-10-01', 1001);
ALTER TABLE alter_attach2 ATTACH DETACHED PARTITION ('2021-10-01', 1001) FROM alter_attach1;
SELECT * FROM alter_attach2;

--2. Test attach from table with different data structure. Will throw exception
DROP TABLE IF EXISTS alter_attach3;
CREATE TABLE alter_attach3 (c1 Date, c2 UInt32, c4 String) ENGINE = CnchMergeTree ORDER BY tuple() PARTITION BY (c1, c2);
ALTER TABLE alter_attach1 DETACH PARTITION ('2021-10-02', 1002);
ALTER TABLE alter_attach3 ATTACH DETACHED PARTITION ('2021-10-02', 1002) FROM alter_attach1 ; -- { serverError 122 }
SELECT * FROM alter_attach3;

--3. Test attach from table with same data structure but same partition key order in columns' definition.
DROP TABLE IF EXISTS alter_attach4;
CREATE TABLE alter_attach4 (c2 UInt32, c1 Date, c3 String) ENGINE = CnchMergeTree ORDER BY tuple() PARTITION BY (c1, c2);
ALTER TABLE alter_attach1 DETACH PARTITION ('2021-10-03', 1003);
ALTER TABLE alter_attach4 ATTACH DETACHED PARTITION ('2021-10-03', 1003) FROM alter_attach1; -- { serverError 122 }
SELECT * FROM alter_attach4;

-- clean tables
DROP TABLE IF EXISTS alter_attach1;
DROP TABLE IF EXISTS alter_attach2;
DROP TABLE IF EXISTS alter_attach3;
DROP TABLE IF EXISTS alter_attach4;