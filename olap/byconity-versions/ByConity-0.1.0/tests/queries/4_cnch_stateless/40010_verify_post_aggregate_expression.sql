

DROP TABLE IF EXISTS t;

CREATE TABLE t (a Int32, b Int32, c Array(Int32)) ENGINE = CnchMergeTree() ORDER BY a;

INSERT INTO t VALUES (10, 20, [1, 2, 3]);

SELECT a FROM t GROUP BY a;
SELECT a + 1 FROM t GROUP BY a;
SELECT b FROM t GROUP BY a; -- { serverError 215 }
SELECT b + 1 FROM t GROUP BY a; -- { serverError 215 }
-- below query runs successfully in InterpreterSelectQuery
-- SELECT a FROM t GROUP BY a HAVING b > 0; -- { serverError 215 }
SELECT a FROM t GROUP BY a ORDER BY b; -- { serverError 215 }
SELECT a FROM t GROUP BY a LIMIT 1 BY b; -- { serverError 215 }
SELECT * FROM t GROUP BY a;  -- { serverError 215 }
SELECT a AS b FROM t GROUP BY a ORDER BY b;
SELECT a AS b FROM t GROUP BY a ORDER BY b + 1;

SELECT a + b FROM t GROUP BY a + b, c;
SELECT (a + b) + 2 FROM t GROUP BY a + b, c;

-- TODO: wrong result when setting enable_optimizer=0
SELECT arrayMap(a -> a + b, c) FROM t GROUP BY a + b, c SETTINGS enable_optimizer=1; -- { serverError 215 }

SELECT arrayMap(x -> x + (a + b), c) FROM t GROUP BY a + b, c;

-- TODO: wrong result for both optimizer/non-optimizer
--       the root cause is Project[expr1 = a + b, expr2 = arrayMap(a -> a + b, c)] will have a wrong result
--       since sub expr `a + b` will be wrongly reused
-- SELECT arrayMap(x -> x + (a + b), any(arrayMap(a -> a + b, c))) FROM t GROUP BY a + b, c;

SELECT arrayMap(x -> x + (a + b), any(arrayMap(d -> d + b, c))) FROM t GROUP BY a + b, c;

DROP TABLE IF EXISTS t;
