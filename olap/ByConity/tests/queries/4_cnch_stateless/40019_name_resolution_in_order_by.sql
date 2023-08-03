SET enable_optimizer = 1;

DROP TABLE IF EXISTS t;
CREATE TABLE t (a Int32, b Int32) ENGINE = CnchMergeTree() ORDER BY a;
INSERT INTO t VALUES (1, 1) (1, 2) (2, 1) (2, 2);

SELECT t.a AS origin_a, t.b AS origin_b, -a AS a
FROM t
ORDER BY -a ASC, -b ASC;

SELECT t.a AS origin_a, t.b AS origin_b, -a AS a
FROM t
ORDER BY -t.a ASC, -t.b ASC;

SELECT t.a AS origin_a, t.b AS origin_b, -a AS a
FROM t
ORDER BY a ASC, b ASC;

DROP TABLE IF EXISTS t;
