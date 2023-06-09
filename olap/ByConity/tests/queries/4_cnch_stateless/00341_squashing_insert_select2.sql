USE test;
DROP TABLE IF EXISTS numbers_squashed;
CREATE TABLE numbers_squashed (number UInt8) ENGINE = CnchMergeTree ORDER BY number;
SET min_insert_block_size_rows = 100;
SET min_insert_block_size_bytes = 0;
SET max_threads = 1;
INSERT INTO numbers_squashed
SELECT arrayJoin(range(10)) AS number
UNION ALL
SELECT arrayJoin(range(100)) AS number
UNION ALL
SELECT arrayJoin(range(10)) AS number;
SYSTEM STOP MERGES test.numbers_squashed;
SELECT blockSize() AS b, count() / blockSize() AS c FROM numbers_squashed GROUP BY blockSize() ORDER BY c DESC, b ASC;
SELECT count() FROM numbers_squashed;
INSERT INTO numbers_squashed
SELECT arrayJoin(range(100)) AS number
UNION ALL
SELECT arrayJoin(range(10))
UNION ALL
SELECT arrayJoin(range(100));
SYSTEM STOP MERGES test.numbers_squashed;
SELECT blockSize() AS b, count() / blockSize() AS c FROM numbers_squashed GROUP BY blockSize() ORDER BY c DESC, b ASC;
SELECT count() FROM numbers_squashed;
INSERT INTO numbers_squashed
SELECT arrayJoin(range(10)) AS number
UNION ALL
SELECT arrayJoin(range(100))
UNION ALL
SELECT arrayJoin(range(100));
SYSTEM STOP MERGES test.numbers_squashed;
SELECT blockSize() AS b, count() / blockSize() AS c FROM numbers_squashed GROUP BY blockSize() ORDER BY c DESC, b ASC;
SELECT count() FROM numbers_squashed;
INSERT INTO numbers_squashed
SELECT arrayJoin(range(10)) AS number
UNION ALL
SELECT arrayJoin(range(10))
UNION ALL
SELECT arrayJoin(range(10))
UNION ALL
SELECT arrayJoin(range(100))
UNION ALL
SELECT arrayJoin(range(10));
SYSTEM STOP MERGES test.numbers_squashed;
SELECT blockSize() AS b, count() / blockSize() AS c FROM numbers_squashed GROUP BY blockSize() ORDER BY c DESC, b ASC;
SELECT count() FROM numbers_squashed;
SET min_insert_block_size_rows = 10;
INSERT INTO numbers_squashed
SELECT arrayJoin(range(10)) AS number
UNION ALL
SELECT arrayJoin(range(10))
UNION ALL
SELECT arrayJoin(range(10))
UNION ALL
SELECT arrayJoin(range(100))
UNION ALL
SELECT arrayJoin(range(10));
SYSTEM STOP MERGES test.numbers_squashed;
SELECT blockSize() AS b, count() / blockSize() AS c FROM numbers_squashed GROUP BY blockSize() ORDER BY c DESC, b ASC;
SELECT count() FROM numbers_squashed;
DROP TABLE numbers_squashed;
