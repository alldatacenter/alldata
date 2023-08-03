
DROP TABLE IF EXISTS deduplication;
CREATE TABLE deduplication (d Date DEFAULT '2015-01-01', x Int8)
ENGINE = CnchMergeTree PARTITION BY d ORDER BY x SETTINGS index_granularity = 1;
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
INSERT INTO deduplication (x) VALUES (1);
SELECT distinct * FROM deduplication;
DETACH TABLE deduplication PERMANENTLY;
ATTACH TABLE deduplication;
SELECT distinct * FROM deduplication;
DROP TABLE deduplication;
