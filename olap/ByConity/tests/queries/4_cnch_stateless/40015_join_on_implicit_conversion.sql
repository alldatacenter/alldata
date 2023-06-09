SET enable_optimizer=1;

SELECT a1, a2, a3, a4, b1, b2, b3, b4
FROM (SELECT toUInt8(1) AS a1, toUInt16(2) AS a2, toUInt8(3) AS a3, toUInt8(4) AS a4) AS r
JOIN (SELECT toUInt8(1) AS b1, toUInt8(2) AS b2, toInt16(3) AS b3, toInt8(4) AS b4) AS s
ON a1 = b1 AND a2 = b2 AND a3 = b3 AND a4 = b4;

-- cast for complex expressions
SELECT a1, a2, a3, a4, b1, b2, b3, b4
FROM (SELECT toUInt8(1) AS a1, toUInt16(2) AS a2, toUInt8(3) AS a3, toUInt8(4) AS a4) AS r
JOIN (SELECT toUInt8(1) AS b1, toUInt8(2) AS b2, toInt16(3) AS b3, toInt8(4) AS b4) AS s
ON (a1 + 1) = (b1 + 1) AND (a2 + 1) = (b2 + 1) AND (a3 + 1) = (b3 + 1) AND (a4 + 1) = (b4 + 1);

-- one column being casted to multiple join keys
SELECT a1, b1, b2, b3, b4
FROM (SELECT toUInt8(1) AS a1) AS r
JOIN (SELECT toUInt8(1) AS b1, toInt8(1) AS b2, toUInt16(1) AS b3, toInt16(1) AS b4) AS s
ON a1 = b1 AND a1 = b2 AND a1 = b3 AND a1 = b4;

SELECT a1, b1, b2, b3, b4
FROM (SELECT toUInt8(1) AS a1) AS r
JOIN (SELECT toUInt16(2) AS b1, toInt16(2) AS b2, toUInt32(2) AS b3, toInt32(2) AS b4) AS s
ON (a1 + 1) = b1 AND (a1 + 1) = b2 AND (a1 + 1) = b3 AND (a1 + 1) = b4;

-- test outer join when join_use_nulls = 0
SET join_use_nulls = 0;

SELECT a1, a2, a3, a4, b1, b2, b3, b4
FROM (SELECT toUInt8(1) AS a1, toUInt16(1) AS a2, toUInt8(1) AS a3, toUInt8(1) AS a4) AS r
LEFT JOIN (SELECT toUInt8(2) AS b1, toUInt8(2) AS b2, toInt16(2) AS b3, toInt8(2) AS b4) AS s
ON a1 = b1 AND a2 = b2 AND a3 = b3 AND a4 = b4;

SELECT a1, a2, a3, a4, b1, b2, b3, b4
FROM (SELECT toUInt8(1) AS a1, toUInt16(1) AS a2, toUInt8(1) AS a3, toUInt8(1) AS a4) AS r
RIGHT JOIN (SELECT toUInt8(2) AS b1, toUInt8(2) AS b2, toInt16(2) AS b3, toInt8(2) AS b4) AS s
ON a1 = b1 AND a2 = b2 AND a3 = b3 AND a4 = b4;

SELECT a1, a2, a3, a4, b1, b2, b3, b4
FROM (SELECT toUInt8(1) AS a1, toUInt16(1) AS a2, toUInt8(1) AS a3, toUInt8(1) AS a4) AS r
FULL JOIN (SELECT toUInt8(2) AS b1, toUInt8(2) AS b2, toInt16(2) AS b3, toInt8(2) AS b4) AS s
ON a1 = b1 AND a2 = b2 AND a3 = b3 AND a4 = b4
ORDER BY a1, b1;

-- test outer join when join_use_nulls = 1
SET join_use_nulls = 1;

SELECT a1, a2, a3, a4, b1, b2, b3, b4
FROM (SELECT toUInt8(1) AS a1, toUInt16(1) AS a2, toUInt8(1) AS a3, toUInt8(1) AS a4) AS r
LEFT JOIN (SELECT toUInt8(2) AS b1, toUInt8(2) AS b2, toInt16(2) AS b3, toInt8(2) AS b4) AS s
ON a1 = b1 AND a2 = b2 AND a3 = b3 AND a4 = b4;

SELECT a1, a2, a3, a4, b1, b2, b3, b4
FROM (SELECT toUInt8(1) AS a1, toUInt16(1) AS a2, toUInt8(1) AS a3, toUInt8(1) AS a4) AS r
RIGHT JOIN (SELECT toUInt8(2) AS b1, toUInt8(2) AS b2, toInt16(2) AS b3, toInt8(2) AS b4) AS s
ON a1 = b1 AND a2 = b2 AND a3 = b3 AND a4 = b4;

SELECT a1, a2, a3, a4, b1, b2, b3, b4
FROM (SELECT toUInt8(1) AS a1, toUInt16(1) AS a2, toUInt8(1) AS a3, toUInt8(1) AS a4) AS r
FULL JOIN (SELECT toUInt8(2) AS b1, toUInt8(2) AS b2, toInt16(2) AS b3, toInt8(2) AS b4) AS s
ON a1 = b1 AND a2 = b2 AND a3 = b3 AND a4 = b4
ORDER BY a1, b1;