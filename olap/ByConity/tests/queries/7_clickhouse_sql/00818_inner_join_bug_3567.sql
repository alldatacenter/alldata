DROP TABLE IF EXISTS test.table1;
DROP TABLE IF EXISTS test.table2;

CREATE TABLE test.table1(a String, b Date) ENGINE CnchMergeTree order by a;
CREATE TABLE test.table2(c String, a String, d Date) ENGINE CnchMergeTree order by c;

INSERT INTO test.table1 VALUES ('a', '2018-01-01') ('b', '2018-01-01') ('c', '2018-01-01');
INSERT INTO test.table2 VALUES ('D', 'd', '2018-01-01') ('B', 'b', '2018-01-01') ('C', 'c', '2018-01-01');

SELECT * FROM test.table1 t1;
-- to determine a better behavior of alias
-- SELECT *, c as a, d as b FROM test.table2;
SELECT * FROM test.table1 t1 ALL LEFT JOIN (SELECT *, c, d as b FROM test.table2) t2 USING (a, b) ORDER BY a, d;
SELECT * FROM test.table1 t1 ALL INNER JOIN (SELECT *, c, d as b FROM test.table2) t2 USING (a, b) ORDER BY a, d;

DROP TABLE test.table1;
DROP TABLE test.table2;