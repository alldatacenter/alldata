SELECT range(x) AS k, count() FROM (SELECT number % 2 ? number : 0 AS x FROM system.numbers LIMIT 10) GROUP BY range(x) ORDER BY k;
SELECT range(x) AS k1, range(y) AS k2, count() FROM (SELECT number % 2 ? number : 0 AS x, number % 3 ? toUInt64(20 - number) : 0 AS y FROM system.numbers LIMIT 20) GROUP BY range(x), range(y) ORDER BY k1, k2;
