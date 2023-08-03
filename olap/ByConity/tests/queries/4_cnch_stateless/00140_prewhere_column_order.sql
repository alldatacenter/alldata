
DROP TABLE IF EXISTS prewhere;

CREATE TABLE prewhere (d Date, a String, b String) ENGINE = CnchMergeTree() PARTITION BY toYYYYMM(d) ORDER BY d SETTINGS index_granularity = 8192;
INSERT INTO prewhere VALUES ('2015-01-01', 'hello', 'world');

ALTER TABLE prewhere ADD COLUMN a1 String AFTER a;
INSERT INTO prewhere VALUES ('2015-01-01', 'hello1', 'xxx', 'world1');

SELECT d, a, a1, b FROM prewhere PREWHERE a LIKE 'hello%' ORDER BY a1;

DROP TABLE prewhere;
