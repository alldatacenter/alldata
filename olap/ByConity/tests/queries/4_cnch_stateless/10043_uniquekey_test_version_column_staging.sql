set enable_staging_area_for_write = 1;

-- Test use version column with staging area
DROP TABLE IF EXISTS unique_with_version_staging;
CREATE TABLE unique_with_version_staging (event_time DateTime, id UInt64, s String, m1 UInt32, m2 UInt64) ENGINE = CnchMergeTree(event_time) PARTITION BY toDate(event_time) ORDER BY (s, id) PRIMARY KEY s UNIQUE KEY id;

INSERT INTO unique_with_version_staging VALUES ('2020-10-29 23:40:00', 10001, '10001A', 5, 500), ('2020-10-29 23:40:00', 10002, '10002A', 2, 200), ('2020-10-29 23:50:00', 10001, '10001B', 8, 800), ('2020-10-29 23:50:00', 10002, '10002B', 5, 500);
INSERT INTO unique_with_version_staging VALUES ('2020-10-30 00:05:00', 10001, '10001A', 1, 100), ('2020-10-30 00:05:00', 10002, '10002A', 2, 200);
INSERT INTO unique_with_version_staging VALUES ('2020-10-29 23:40:00', 10001, '10001A', 5, 500), ('2020-10-29 23:40:00', 10002, '10002A', 2, 200);

-- Catalog in CI are slow
SYSTEM SYNC DEDUP WORKER unique_with_version_staging;
SELECT event_time, id, s, m1, m2 FROM unique_with_version_staging ORDER BY event_time, id;

INSERT INTO unique_with_version_staging VALUES ('2020-10-29 23:50:00', 10001, '10001B', 8, 800), ('2020-10-29 23:50:00', 10002, '10002B', 5, 500), ('2020-10-29 23:55:00', 10001, '10001C', 10, 1000), ('2020-10-29 23:55:00', 10002, '10002C', 7, 700);

SYSTEM SYNC DEDUP WORKER unique_with_version_staging;
SELECT event_time, id, s, m1, m2 FROM unique_with_version_staging ORDER BY event_time, id;

DROP TABLE IF EXISTS unique_with_version_staging;
