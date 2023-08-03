SELECT dummy IN (0) AS x, count() GROUP BY dummy IN (0);

SELECT 1 IN (0) AS x, count() GROUP BY 1 IN (0);
SELECT 0 IN (0) AS x, count() GROUP BY 0 IN (0);
SELECT materialize(1) IN (0) AS x, count() GROUP BY materialize(1) IN (0);
SELECT materialize(0) IN (0) AS x, count() GROUP BY materialize(0) IN (0);

SELECT
    number IN (1, 2) AS x,
    count()
FROM numbers(10)
GROUP BY number IN (1, 2);
