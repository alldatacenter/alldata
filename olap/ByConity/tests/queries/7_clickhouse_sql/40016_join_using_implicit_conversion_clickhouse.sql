SET enable_optimizer=1;

SELECT a, b, c, d
FROM (SELECT toUInt8(1) AS a, toUInt16(1) AS b, toUInt8(1) AS c, toUInt8(1) AS d) AS r
LEFT JOIN (SELECT toUInt8(2) AS a, toUInt8(2) AS b, toInt16(2) AS c, toInt8(2) AS d) AS s
USING (a, b, c, d);

SELECT a, b, c, d
FROM (SELECT toUInt8(1) AS a, toUInt16(1) AS b, toUInt8(1) AS c, toUInt8(1) AS d) AS r
RIGHT JOIN (SELECT toUInt8(2) AS a, toUInt8(2) AS b, toInt16(2) AS c, toInt8(2) AS d) AS s
USING (a, b, c, d);

SELECT a, b, c, d
FROM (SELECT toUInt8(1) AS a, toUInt16(1) AS b, toUInt8(1) AS c, toUInt8(1) AS d) AS r
FULL JOIN (SELECT toUInt8(2) AS a, toUInt8(2) AS b, toInt16(2) AS c, toInt8(2) AS d) AS s
USING (a, b, c, d)
ORDER BY a, b, c, d;

SELECT a + 1 AS x, a + 1 AS y, a + 1 AS u, a + 1 AS v
FROM (SELECT toUInt8(1) AS a) AS r
JOIN (SELECT toUInt16(2) AS x, toInt16(2) AS y, toUInt32(2) AS u, toInt32(2) AS v) AS s
USING (x, y, u, v);

SELECT a + 1 AS x, a + 1 AS y, a + 1 AS u, a + 1 AS v
FROM (SELECT toUInt8(1) AS a) AS r
LEFT JOIN (SELECT toUInt16(3) AS x, toInt16(3) AS y, toUInt32(3) AS u, toInt32(3) AS v) AS s
USING (x, y, u, v);

-- SELECT a + 1 AS x, a + 1 AS y, a + 1 AS u, a + 1 AS v
-- FROM (SELECT toUInt8(1) AS a) AS r
-- RIGHT JOIN (SELECT toUInt16(3) AS x, toInt16(3) AS y, toUInt32(3) AS u, toInt32(3) AS v) AS s
-- USING (x, y, u, v);
--
-- SELECT a + 1 AS x, a + 1 AS y, a + 1 AS u, a + 1 AS v
-- FROM (SELECT toUInt8(1) AS a) AS r
-- FULL JOIN (SELECT toUInt16(3) AS x, toInt16(3) AS y, toUInt32(3) AS u, toInt32(3) AS v) AS s
-- USING (x, y, u, v)
-- ORDER BY x, y, u, v;
