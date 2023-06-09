
DROP TABLE IF EXISTS data2013;
DROP TABLE IF EXISTS data2014;
DROP TABLE IF EXISTS data2015;

CREATE TABLE data2013 (name String, value UInt32) ENGINE = CnchMergeTree ORDER BY name;
CREATE TABLE data2014 (name String, value UInt32) ENGINE = CnchMergeTree ORDER BY name;
CREATE TABLE data2015 (data_name String, data_value UInt32) ENGINE = CnchMergeTree ORDER BY data_name;

INSERT INTO data2013(name,value) VALUES('Alice', 1000) ('Bob', 2000) ('Carol', 5000);

INSERT INTO data2014(name,value) VALUES('Alice', 2000) ('Bob', 2000) ('Dennis', 35000);

INSERT INTO data2015(data_name, data_value) VALUES('Foo', 42) ('Bar', 1);

SELECT val FROM
(SELECT value AS val FROM data2013 WHERE name = 'Alice'
UNION ALL
SELECT value AS val FROM data2014 WHERE name = 'Alice')
ORDER BY val ASC;

SELECT val, name FROM
(SELECT value AS val, value AS val_1, name FROM data2013 WHERE name = 'Alice'
UNION ALL
SELECT value AS val, value, name FROM data2014 WHERE name = 'Alice')
ORDER BY val ASC;

DROP TABLE data2013;
DROP TABLE data2014;
DROP TABLE data2015;
