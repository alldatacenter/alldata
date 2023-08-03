
DROP TABLE IF EXISTS visits;
CREATE TABLE visits (str String) ENGINE = CnchMergeTree ORDER BY (str);
SELECT 1
FROM visits
ARRAY JOIN arrayFilter(t -> 1, arrayMap(x -> tuple(x), [42])) AS i
WHERE ((str, 1) IN ('x', 0));
DROP TABLE visits;
