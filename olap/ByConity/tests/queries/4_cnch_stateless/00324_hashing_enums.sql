SELECT cityHash64(x, y) FROM (SELECT 1 AS x, CAST(1 AS Enum8('Hello' = 0, 'World' = 1)) AS y);
SELECT cityHash64(x, y) FROM (SELECT 1 AS x,1 AS y);
