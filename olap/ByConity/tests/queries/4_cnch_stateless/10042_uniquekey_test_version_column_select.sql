DROP TABLE IF EXISTS unique_with_version_t1;
DROP TABLE IF EXISTS unique_with_version_t2;

CREATE TABLE unique_with_version_t1 (event_time DateTime, id UInt64, s String, m1 UInt32, m2 UInt64, seq UInt32)
ENGINE = CnchMergeTree(event_time) PARTITION BY toDate(event_time) ORDER BY (s, id) PRIMARY KEY s UNIQUE KEY id;

INSERT INTO unique_with_version_t1 VALUES ('2020-10-29 23:40:00', 10001, '10001A', 5, 500, 0), ('2020-10-29 23:40:00', 10002, '10002A', 2, 200, 1), ('2020-10-29 23:50:00', 10001, '10001B', 8, 800, 2), ('2020-10-29 23:50:00', 10002, '10002B', 5, 500, 3);
INSERT INTO unique_with_version_t1 VALUES ('2020-10-30 00:05:00', 10001, '10001A', 1, 100, 4), ('2020-10-30 00:05:00', 10002, '10002A', 2, 200, 5);
INSERT INTO unique_with_version_t1 VALUES ('2020-10-29 23:40:00', 10001, '10001A', 5, 500, 6), ('2020-10-29 23:40:00', 10002, '10002A', 2, 200, 7);
INSERT INTO unique_with_version_t1 VALUES ('2020-10-29 23:50:00', 10001, '10001B', 8, 800, 8), ('2020-10-29 23:50:00', 10002, '10002B', 5, 500, 9), ('2020-10-29 23:55:00', 10001, '10001C', 10, 1000, 10), ('2020-10-29 23:55:00', 10002, '10002C', 7, 700, 11);

-- Test insert into select
CREATE TABLE unique_with_version_t2 (event_time DateTime, id UInt64, s String, m1 UInt32, m2 UInt64, seq UInt64)
ENGINE = CnchMergeTree(event_time) PARTITION BY toDate(event_time) ORDER BY (s, id) PRIMARY KEY s UNIQUE KEY id;

INSERT INTO unique_with_version_t2 SELECT * FROM unique_with_version_t1 ORDER BY seq;

SELECT event_time, id, s, m1, m2 FROM unique_with_version_t2 ORDER BY event_time, id;

DROP TABLE IF EXISTS unique_with_version_t1;
DROP TABLE IF EXISTS unique_with_version_t2;
