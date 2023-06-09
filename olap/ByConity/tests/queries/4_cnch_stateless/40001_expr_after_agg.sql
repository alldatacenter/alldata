

DROP TABLE IF EXISTS x;

CREATE TABLE x (a Int32, b Int32) ENGINE = CnchMergeTree() ORDER BY a;

INSERT INTO x VALUES (1, 6), (2, 10), (1, 4), (101, 6);

SELECT
    a % 100,
    b,
    a % 100 + 1,
    b > 5,
    b < 10,
    SUM(1 + 1),
    1 + 1
FROM x
WHERE b > 5 GROUP BY a % 100, b ORDER BY a % 100, b;

SELECT
    a + 1
FROM x
WHERE b > 5 GROUP BY a % 100, b ORDER BY a % 100, b; -- { serverError 215 }

DROP TABLE IF EXISTS x;
