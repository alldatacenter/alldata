
DROP TABLE IF EXISTS test_subquery;
CREATE TABLE test_subquery(x Int32, y String) Engine = CnchMergeTree ORDER BY tuple();
INSERT INTO test_subquery VALUES (1, 'a'), (2, 'b');
SELECT x, y FROM test_subquery WHERE x = (SELECT max(x) FROM test_subquery);
DROP TABLE test_subquery;

DROP TABLE IF EXISTS table_1;
DROP TABLE IF EXISTS table_2;
DROP TABLE IF EXISTS table_3;
CREATE TABLE table_1 (a Int32, b String, key_1 Int32) Engine = CnchMergeTree ORDER BY tuple();
CREATE TABLE table_2 (c Int32, d String, key_2 Int32) Engine = CnchMergeTree ORDER BY tuple();
CREATE TABLE table_3 (e Int32, f String, key_3 Int32) Engine = CnchMergeTree ORDER BY tuple();

INSERT INTO table_1 values (1, 'a', 1);
INSERT INTO table_2 values (2, 'b', 1);
INSERT INTO table_3 values (3, 'c', 1);

CREATE TABLE table_4 engine = CnchMergeTree ORDER BY key_1 AS
SELECT
    t1.a AS col_a,
    t1.b AS col_b,
    t1.key_1 AS key_1,
    t2.c AS col_c,
    t2.d AS col_d,
    t2.key_2 AS key_2,
    t3.e AS col_e,
    t3.f AS col_f,
    t3.key_3 AS key_3
FROM table_1 AS t1
INNER JOIN table_2 AS t2 ON t2.key_2 = t1.key_1
INNER JOIN table_3 AS t3 ON t3.key_3 = t1.key_1;

SELECT * FROM table_4;

DROP TABLE table_1;
DROP TABLE table_2;
DROP TABLE table_3;
DROP TABLE table_4;

DROP TABLE IF EXISTS test_subquery;
DROP TABLE IF EXISTS test_subquery2;

CREATE TABLE test_subquery(cnt UInt64, p_date Date) Engine = CnchMergeTree PARTITION BY p_date ORDER BY tuple();
CREATE TABLE test_subquery2(x UInt64, p_date Date) Engine = CnchMergeTree PARTITION BY p_date ORDER BY tuple();
SELECT * from test_subquery WHERE cnt IN (SELECT count() FROM test_subquery2);
SELECT * from test_subquery WHERE cnt IN (SELECT count() FROM test_subquery2);
SELECT * from test_subquery WHERE cnt IN (SELECT count() FROM test_subquery2);
SELECT * from test_subquery WHERE cnt IN (SELECT count() FROM test_subquery2);

DROP TABLE test_subquery;
DROP TABLE test_subquery2;

