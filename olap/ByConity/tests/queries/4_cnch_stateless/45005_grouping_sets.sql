DROP DATABASE IF EXISTS test;

DROP TABLE IF EXISTS grouping_sets;

CREATE TABLE grouping_sets(`a` String, `b` Int32, `s` Int32)
    ENGINE = CnchMergeTree()
    PRIMARY KEY `a`
    ORDER BY `a`
    SETTINGS index_granularity = 8192;

INSERT INTO grouping_sets VALUES ('a', 1, 10), ('a', 1, 15), ('a', 2, 20);
INSERT INTO grouping_sets VALUES ('a', 2, 25), ('b', 1, 10), ('b', 1, 5);
INSERT INTO grouping_sets VALUES ('b', 2, 20), ('b', 2, 15);

SELECT a, b, sum(s), count() from grouping_sets GROUP BY GROUPING SETS(a, b) ORDER BY a, b;

SELECT a, b, sum(s), count() from grouping_sets GROUP BY GROUPING SETS((a, b), (a, b)) ORDER BY a, b;

select a,b,sums,cnt
from(
    SELECT a, b, sum(s) sums, count() cnt from grouping_sets GROUP BY GROUPING SETS(a, b)
        union all
    SELECT a, b, sum(s) sums, count() cnt from grouping_sets GROUP BY GROUPING SETS(a, b)
    ) ORDER BY a, b ;

SELECT a, b+b, sum(s), count() from grouping_sets GROUP BY GROUPING SETS(a, b+b) ORDER BY a, b+b;

-- WITH GROUPING SETS only support with optimizer
-- SELECT a, b, sum(s), count() from grouping_sets GROUP BY a, b WITH GROUPING SETS ORDER BY a, b;

DROP TABLE grouping_sets;
