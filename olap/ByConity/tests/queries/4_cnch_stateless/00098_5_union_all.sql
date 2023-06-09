
DROP TABLE IF EXISTS data2013;
DROP TABLE IF EXISTS data2014;

CREATE TABLE data2013 (name String, value UInt32) ENGINE = CnchMergeTree ORDER BY name;
CREATE TABLE data2014 (name String, value UInt32) ENGINE = CnchMergeTree ORDER BY name;

INSERT INTO data2013(name,value) VALUES('Alice', 1000) ('Bob', 2000) ('Carol', 5000);

INSERT INTO data2014(name,value) VALUES('Alice', 2000) ('Bob', 2000) ('Dennis', 35000);

SELECT nn,vv FROM (SELECT name AS nn, value AS vv FROM data2013 UNION ALL SELECT name AS nn, value AS vv FROM data2014) ORDER BY nn,vv ASC;

DROP TABLE data2013;
DROP TABLE data2014;
