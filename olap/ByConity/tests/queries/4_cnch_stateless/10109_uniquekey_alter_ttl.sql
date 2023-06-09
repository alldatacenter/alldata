DROP TABLE IF EXISTS u10109_ttl;

CREATE TABLE u10109_ttl (d Date, id Int32, val Int32)
ENGINE = CnchMergeTree PARTITION BY d ORDER BY id UNIQUE KEY id TTL d + INTERVAL 1 DAY;

-- Insert a past date, so it will be deleted #1
INSERT INTO u10109_ttl VALUES ('2021-01-10', 10001, 1);

-- Insert a very far away future date, so it will not be ttled #2
INSERT INTO u10109_ttl VALUES ('2100-07-23', 10002, 1);

SELECT * FROM u10109_ttl ORDER BY d, id;

ALTER TABLE u10109_ttl modify ttl d + INTERVAL 10 DAY;

-- Insert a past date, so it will be deleted #1
INSERT INTO u10109_ttl VALUES ('2020-01-10', 10003, 1);

-- Insert a very far away future date, so it won't be ttled.
INSERT INTO u10109_ttl VALUES ('2100-10-10', 10004, 1);

SELECT * FROM u10109_ttl ORDER BY d, id;

-- This raw should exist after modifying ttl.
INSERT INTO u10109_ttl VALUES (toDate(now() - INTERVAL 8 DAY), 12345, 1);

SELECT id, val from u10109_ttl ORDER BY id;

DROP TABLE IF EXISTS u10109_ttl;
