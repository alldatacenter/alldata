SELECT x, round(x), floor(x), ceil(x), trunc(x) FROM (SELECT toUInt8(number) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x), floor(x), ceil(x), trunc(x) FROM (SELECT toUInt16(number) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x), floor(x), ceil(x), trunc(x) FROM (SELECT toUInt32(number) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x), floor(x), ceil(x), trunc(x) FROM (SELECT toUInt64(number) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x), floor(x), ceil(x), trunc(x) FROM (SELECT toInt8(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x), floor(x), ceil(x), trunc(x) FROM (SELECT toInt16(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x), floor(x), ceil(x), trunc(x) FROM (SELECT toInt32(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x), floor(x), ceil(x), trunc(x) FROM (SELECT toInt64(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x), floor(x), ceil(x), trunc(x) FROM (SELECT toFloat32(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x), floor(x), ceil(x), trunc(x) FROM (SELECT toFloat64(number - 10) AS x FROM system.numbers LIMIT 20);

SELECT x, round(x), floor(x), ceil(x), trunc(x) FROM (SELECT toFloat32((number - 10) / 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x), floor(x), ceil(x), trunc(x) FROM (SELECT toFloat64((number - 10) / 10) AS x FROM system.numbers LIMIT 20);

SELECT x, round(x, 1), floor(x, 1), ceil(x, 1), trunc(x, 1) FROM (SELECT toFloat32((number - 10) / 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, 1), floor(x, 1), ceil(x, 1), trunc(x, 1) FROM (SELECT toFloat64((number - 10) / 10) AS x FROM system.numbers LIMIT 20);

SELECT x, round(x, -1), floor(x, -1), ceil(x, -1), trunc(x, -1) FROM (SELECT toUInt8(number) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -1), floor(x, -1), ceil(x, -1), trunc(x, -1) FROM (SELECT toUInt16(number) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -1), floor(x, -1), ceil(x, -1), trunc(x, -1) FROM (SELECT toUInt32(number) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -1), floor(x, -1), ceil(x, -1), trunc(x, -1) FROM (SELECT toUInt64(number) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -1), floor(x, -1), ceil(x, -1), trunc(x, -1) FROM (SELECT toInt8(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -1), floor(x, -1), ceil(x, -1), trunc(x, -1) FROM (SELECT toInt16(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -1), floor(x, -1), ceil(x, -1), trunc(x, -1) FROM (SELECT toInt32(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -1), floor(x, -1), ceil(x, -1), trunc(x, -1) FROM (SELECT toInt64(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -1), floor(x, -1), ceil(x, -1), trunc(x, -1) FROM (SELECT toFloat32(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -1), floor(x, -1), ceil(x, -1), trunc(x, -1) FROM (SELECT toFloat64(number - 10) AS x FROM system.numbers LIMIT 20);

SELECT x, round(x, -2), floor(x, -2), ceil(x, -2), trunc(x, -2) FROM (SELECT toUInt8(number) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -2), floor(x, -2), ceil(x, -2), trunc(x, -2) FROM (SELECT toUInt16(number) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -2), floor(x, -2), ceil(x, -2), trunc(x, -2) FROM (SELECT toUInt32(number) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -2), floor(x, -2), ceil(x, -2), trunc(x, -2) FROM (SELECT toUInt64(number) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -2), floor(x, -2), ceil(x, -2), trunc(x, -2) FROM (SELECT toInt8(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -2), floor(x, -2), ceil(x, -2), trunc(x, -2) FROM (SELECT toInt16(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -2), floor(x, -2), ceil(x, -2), trunc(x, -2) FROM (SELECT toInt32(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -2), floor(x, -2), ceil(x, -2), trunc(x, -2) FROM (SELECT toInt64(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -2), floor(x, -2), ceil(x, -2), trunc(x, -2) FROM (SELECT toFloat32(number - 10) AS x FROM system.numbers LIMIT 20);
SELECT x, round(x, -2), floor(x, -2), ceil(x, -2), trunc(x, -2) FROM (SELECT toFloat64(number - 10) AS x FROM system.numbers LIMIT 20);

SELECT x, floor(x, -1), floor(x, -2), floor(x, -3), floor(x, -4), floor(x, -5), floor(x, -6), floor(x, -7), floor(x, -8), floor(x, -9), floor(x, -10) FROM (SELECT 123456789 AS x);
SELECT x, floor(x, -1), floor(x, -2), floor(x, -3), floor(x, -4), floor(x, -5), floor(x, 1), floor(x, 2), floor(x, 3), floor(x, 4), floor(x, 5) FROM (SELECT 12345.6789 AS x);


SELECT roundToExp2(100), roundToExp2(64), roundToExp2(3), roundToExp2(0), roundToExp2(-1);
SELECT roundToExp2(0.9), roundToExp2(0), roundToExp2(-0.5), roundToExp2(-0.6), roundToExp2(-0.2);
