DROP TABLE IF EXISTS dts_partition_test;
CREATE TABLE dts_partition_test (n Int64, s String) Engine = CnchMergeTree ORDER BY (n);

INSERT INTO dts_partition_test VALUES (12345678, '12345678'), (3413243243, '3413243243'), (111, '111'), (-111, '-111');

SELECT dtspartition(n, 12000), dtspartition(s, 12000) FROM dts_partition_test;

DROP TABLE dts_partition_test;

CREATE TABLE dts_partition_test (s String) Engine = CnchMergeTree ORDER BY (s);

INSERT INTO dts_partition_test VALUES ('abc'), ('abcabc'), ('abcabcabc'), ('abcabcabcabcabcabcabcabc'), ('abcabcabcabcabcabcabcabcabcabcabcabc'), ('abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc'), ('中国人'), ('中国人中国人中国人中国人中国人中国人中国人中国人中国人中国人');

SELECT dtspartition(s, 12000) FROM dts_partition_test;

-- Check the non-const split number
SELECT dtspartition(600, toUInt64(20));
SELECT dtspartition(600, toUInt32(20));
SELECT dtspartition(600, toUInt16(20));
SELECT dtspartition(600, toUInt8(20));
SELECT dtspartition(600, toInt64(20));
SELECT dtspartition(600, toInt32(20));
SELECT dtspartition(600, toInt16(20));
SELECT dtspartition(600, toInt8(20));

DROP TABLE dts_partition_test;
