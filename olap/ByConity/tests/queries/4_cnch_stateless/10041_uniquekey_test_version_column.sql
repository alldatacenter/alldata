DROP TABLE IF EXISTS unique_with_version_bad1;
DROP TABLE IF EXISTS unique_with_version_bad2;

CREATE TABLE unique_with_version_bad1 (event_time DateTime, id UInt64, m1 UInt32, m2 UInt64) ENGINE = CnchMergeTree(m1) PARTITION BY toDate(event_time) ORDER BY id UNIQUE KEY id;
-- Test alter version column for a table
ALTER TABLE unique_with_version_bad1 DROP COLUMN m1; -- { serverError 524 }
ALTER TABLE unique_with_version_bad1 RENAME COLUMN m1 to a1; -- { serverError 524 }
ALTER TABLE unique_with_version_bad1 MODIFY COLUMN m1 String; -- { serverError 524 }

-- Test use not exist as version column
CREATE TABLE unique_with_version_bad2 (event_time DateTime, id UInt64, m1 UInt32, m2 UInt64) ENGINE = CnchMergeTree(unknown) PARTITION BY toDate(event_time) ORDER BY id UNIQUE KEY id; -- { serverError 16 }
-- Test expression as version column
CREATE TABLE unique_with_version_bad2 (event_time DateTime, id UInt64, m1 UInt32, m2 UInt64) ENGINE = CnchMergeTree(sipHash64(id)) PARTITION BY toDate(event_time) ORDER BY id UNIQUE KEY id; -- { serverError 36 }

DROP TABLE IF EXISTS unique_with_version_bad1;
DROP TABLE IF EXISTS unique_with_version_bad2;

-- Test use version column
DROP TABLE IF EXISTS unique_with_version;
CREATE TABLE unique_with_version (event_time DateTime, id UInt64, s String, m1 UInt32, m2 UInt64) ENGINE = CnchMergeTree(event_time) PARTITION BY toDate(event_time) ORDER BY (s, id) PRIMARY KEY s UNIQUE KEY id;

INSERT INTO unique_with_version VALUES ('2020-10-29 23:40:00', 10001, '10001A', 5, 500), ('2020-10-29 23:40:00', 10002, '10002A', 2, 200), ('2020-10-29 23:50:00', 10001, '10001B', 8, 800), ('2020-10-29 23:50:00', 10002, '10002B', 5, 500);
INSERT INTO unique_with_version VALUES ('2020-10-30 00:05:00', 10001, '10001A', 1, 100), ('2020-10-30 00:05:00', 10002, '10002A', 2, 200);
INSERT INTO unique_with_version VALUES ('2020-10-29 23:40:00', 10001, '10001A', 5, 500), ('2020-10-29 23:40:00', 10002, '10002A', 2, 200);

SELECT event_time, id, s, m1, m2 FROM unique_with_version ORDER BY event_time, id;
INSERT INTO unique_with_version VALUES ('2020-10-29 23:50:00', 10001, '10001B', 8, 800), ('2020-10-29 23:50:00', 10002, '10002B', 5, 500), ('2020-10-29 23:55:00', 10001, '10001C', 10, 1000), ('2020-10-29 23:55:00', 10002, '10002C', 7, 700);
SELECT event_time, id, s, m1, m2 FROM unique_with_version ORDER BY event_time, id;

DROP TABLE IF EXISTS unique_with_version;
