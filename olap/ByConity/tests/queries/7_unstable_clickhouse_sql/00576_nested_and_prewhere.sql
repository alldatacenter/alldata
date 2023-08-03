USE test;
DROP TABLE IF EXISTS test.nested;

CREATE TABLE test.nested (x UInt64, filter UInt8, n Nested(a UInt64)) ENGINE = CnchMergeTree ORDER BY x;
INSERT INTO test.nested SELECT number, number % 2, range(number % 10) FROM system.numbers LIMIT 100000;

ALTER TABLE test.nested ADD COLUMN n.b Array(UInt64);
SELECT DISTINCT n.b FROM test.nested PREWHERE filter order by 1;

ALTER TABLE test.nested ADD COLUMN n.c Array(UInt64) DEFAULT arrayMap(x -> x * 2, n.a);
SELECT DISTINCT n.c FROM test.nested PREWHERE filter order by 1;
SELECT DISTINCT n.a, n.c FROM test.nested PREWHERE filter order by 1;

DROP TABLE test.nested;
