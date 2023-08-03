SET enable_optimizer=1;

SELECT a, b, c, d
FROM (SELECT toUInt8(1) AS a, toUInt16(2) AS b, toUInt8(3) AS c, toUInt8(4) AS d) AS r
JOIN (SELECT toUInt8(1) AS a, toUInt8(2) AS b, toInt16(3) AS c, toInt8(4) AS d) AS s
USING (a, b, c, d);