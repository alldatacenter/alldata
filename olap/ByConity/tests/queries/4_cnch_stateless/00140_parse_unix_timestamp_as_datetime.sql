SELECT min(ts = toUInt32(toDateTime(toString(ts)))) FROM (SELECT 1000000000 + 1234 * number AS ts FROM system.numbers LIMIT 1000000);

DROP TABLE IF EXISTS datetime;
CREATE TABLE datetime (d DateTime, i int) ENGINE = CnchMergeTree() ORDER BY d;
INSERT INTO datetime VALUES (, 1), (-1, 2), (0, 3), (4291747599, 4), (4291747199, 5) , (1, 6)
SELECT toDateTime(d, 'Etc/UTC'), i FROM datetime ORDER BY i;
SELECT toDateTime(d, 'Asia/Singapore'), i FROM datetime ORDER BY i;
DROP TABLE datetime;
