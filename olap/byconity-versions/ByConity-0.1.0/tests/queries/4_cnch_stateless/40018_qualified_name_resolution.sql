SELECT count(*) + a, count(*) + (t.b + 2)
FROM (SELECT 1 as a, 2 AS b) AS t
GROUP BY t.a, b + 2;
