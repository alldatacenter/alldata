
SET enable_optimizer = 0; -- in table don't support in optimizer
DROP TABLE IF EXISTS set;
CREATE TABLE set (x String) ENGINE = CnchMergeTree ORDER BY x;
INSERT INTO set VALUES ('hello');
SELECT (arrayJoin(['hello', 'world']) AS s) IN set, s;

DROP TABLE set;
CREATE TABLE set (x String) ENGINE = CnchMergeTree ORDER BY x;
INSERT INTO set VALUES ('hello');
SELECT (arrayJoin(['hello', 'world']) AS s) IN set, s;

DROP TABLE set;

DROP TABLE IF EXISTS join;
CREATE TABLE join (k UInt8, x String) ENGINE = CnchMergeTree ORDER BY k;
INSERT INTO join VALUES (1, 'hello');
SELECT k, x FROM (SELECT arrayJoin([1, 2]) AS k) js1 ANY LEFT JOIN join USING k ORDER BY k;

DROP TABLE join;
CREATE TABLE join (k UInt8, x String) ENGINE = CnchMergeTree ORDER BY k;
INSERT INTO join VALUES (1, 'hello');
SELECT k, x FROM (SELECT arrayJoin([1, 2]) AS k) js1 ANY LEFT JOIN join USING k ORDER BY k;

DROP TABLE join;
