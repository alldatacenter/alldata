DROP TABLE IF EXISTS grouping_sets;
CREATE TABLE grouping_sets(a String, b Int32, s Int32) Engine = CnchMergeTree ORDER BY tuple();

INSERT INTO grouping_sets VALUES ('a', 1, 10), ('a', 1, 15), ('a', 2, 20);
INSERT INTO grouping_sets VALUES ('a', 2, 25), ('b', 1, 10), ('b', 1, 5);
INSERT INTO grouping_sets VALUES ('b', 2, 20), ('b', 2, 15);

SELECT a, b, sum(s), count() from grouping_sets GROUP BY GROUPING SETS(a, b) ORDER BY a, b;

SELECT a, b, sum(s), count() from grouping_sets GROUP BY GROUPING SETS(a, b) WITH TOTALS ORDER BY a, b;  -- { serverError 48 }

-- WITH GROUPING SETS only support with optimizer
-- SELECT a, b, sum(s), count() from grouping_sets GROUP BY a, b WITH GROUPING SETS ORDER BY a, b;
-- SELECT a, b, sum(s), count() from grouping_sets GROUP BY a, b WITH GROUPING SETS WITH TOTALS ORDER BY a, b;  -- { serverError 48 }

DROP TABLE grouping_sets;
