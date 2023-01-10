SELECT a, b, c, d
FROM (
    (SELECT toUInt8(1) AS a, toUInt16(2) AS b, toUInt8(3) AS c, toUInt8(4) AS d)
    UNION ALL
    (SELECT toUInt8(11), toUInt8(12), toInt16(13), toInt8(14))
)
ORDER BY a, b, c, d;
