
DROP TABLE IF EXISTS rename1;
DROP TABLE IF EXISTS rename2;
DROP TABLE IF EXISTS rename11;
DROP TABLE IF EXISTS rename22;
CREATE TABLE rename1 (d Date, a String, b String) ENGINE = CnchMergeTree() PARTITION BY toYYYYMM(d) ORDER BY d SETTINGS index_granularity = 8192;
CREATE TABLE rename2 (d Date, a String, b String) ENGINE = CnchMergeTree() PARTITION BY toYYYYMM(d) ORDER BY d SETTINGS index_granularity = 8192;
INSERT INTO rename1 VALUES ('2015-01-01', 'hello', 'world');
INSERT INTO rename2 VALUES ('2015-01-02', 'hello2', 'world2');
SELECT * FROM rename1;
SELECT * FROM rename2;
RENAME TABLE rename1 TO rename11, rename2 TO rename22;
SELECT * FROM rename11;
SELECT * FROM rename22;


DROP TABLE IF EXISTS rename1;
DROP TABLE IF EXISTS rename2;
DROP TABLE IF EXISTS rename11;
DROP TABLE IF EXISTS rename22;
