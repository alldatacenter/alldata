-- Test bad input
DROP TABLE IF EXISTS unique_with_partition_version_bad1;
CREATE TABLE unique_with_partition_version_bad1 (event_time DateTime, id UInt64, m1 UInt32, m2 UInt64) ENGINE = CnchMergeTree(event_time, id) PARTITION BY (event_time, id) ORDER BY id UNIQUE KEY id; -- { serverError 42 }
CREATE TABLE unique_with_partition_version_bad1 (event_time String, id UInt64, m1 UInt32, m2 UInt64) ENGINE = CnchMergeTree(event_time) PARTITION BY event_time ORDER BY id UNIQUE KEY id; -- { serverError 36 }
DROP TABLE IF EXISTS unique_with_partition_version_bad1;

DROP TABLE IF EXISTS u10040_pav;

CREATE TABLE u10040_pav (c1 DateTime, c2 UInt32, c3 UInt32) ENGINE = CnchMergeTree(toDate(c1))
PARTITION BY toDate(c1) ORDER BY c2 UNIQUE KEY c2 SETTINGS partition_level_unique_keys = 0;

INSERT INTO u10040_pav VALUES ('2020-10-29 00:00:00', 1000, 0), ('2020-10-29 00:00:00', 1001, 1), ('2020-10-30 00:00:00', 1000, 2), ('2020-10-28 00:00:00', 1000, 3);
SELECT '1st insert';
SELECT * FROM u10040_pav ORDER BY c1, c2;

INSERT INTO u10040_pav VALUES ('2020-10-27 00:00:00', 1001, 10), ('2020-10-28 00:00:00', 1001, 11), ('2020-10-28', 1002, 13);
SELECT 'insert smaller version';
SELECT * FROM u10040_pav ORDER BY c1, c2;

INSERT INTO u10040_pav VALUES ('2020-10-29 00:00:00', 1002, 20), ('2020-10-28 00:00:00', 1002, 21), ('2020-10-29 00:00:00', 1001, 22), ('2020-10-28 00:00:00', 1001, 23);
SELECT 'insert greater version';
SELECT * FROM u10040_pav ORDER BY c1, c2;

SELECT 'dedup parts with different commit time';
SYSTEM START DEDUP WORKER u10040_pav;
SYSTEM STOP DEDUP WORKER u10040_pav;

INSERT INTO u10040_pav Format Values SETTINGS enable_staging_area_for_write = 1
('2020-10-30 00:00:00', 1000, 30), ('2020-10-30 00:00:00', 1001, 31), ('2020-10-28 00:00:00', 1002, 32);

INSERT INTO u10040_pav Format Values SETTINGS enable_staging_area_for_write = 1
('2020-10-30 00:00:00', 1000, 40), ('2020-10-29 00:00:00', 1001, 41), ('2020-10-28 00:00:00', 1002, 42);

INSERT INTO u10040_pav VALUES ('2020-10-29 00:00:00', 1003, 50);
SELECT * FROM u10040_pav ORDER BY c1, c2;

DROP TABLE IF EXISTS u10040_pav;
