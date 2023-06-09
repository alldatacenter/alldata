SELECT toFixedString('', 4) AS str, empty(toFixedString('', 4)) AS is_empty;
SELECT toFixedString('\0abc', 4) AS str, empty(toFixedString('\0abc', 4)) AS is_empty;

USE test;
DROP TABLE IF EXISTS defaulted;
CREATE TABLE defaulted (v6 FixedString(16)) ENGINE=CnchMergeTree ORDER BY v6;
INSERT INTO defaulted SELECT toFixedString('::0', 16) FROM numbers(32768);
SELECT count(), notEmpty(v6) e FROM defaulted GROUP BY notEmpty(v6);
DROP TABLE defaulted;
