SELECT x, roundDown(x, [0, 1, 2, 3, 4, 5]) FROM (SELECT number AS x FROM system.numbers LIMIT 10);
SELECT x, roundDown(x, [-1.5, e(), pi(), 5.5]) FROM (SELECT toUInt8(number) AS x FROM system.numbers LIMIT 10);
SELECT x, roundDown(x, [e(), pi(), pi(), e()]) FROM (SELECT toInt32(number) AS x FROM system.numbers LIMIT 10);
SELECT x, roundDown(x, [6, 5, 4]) FROM (SELECT number AS x FROM system.numbers LIMIT 10);
SELECT x, roundDown(x, [6, 5, 4]) FROM (SELECT 1 AS x);

SET send_logs_level = 'none';
SELECT x, roundDown(x, []) FROM (SELECT 1 AS x); -- { serverError 43 }
SELECT x, roundDown(x, CAST([], 'Array(UInt8)')) FROM (SELECT 1 AS x); -- { serverError 36 }
SELECT roundDown(number, [number]) FROM system.numbers LIMIT 10; -- { serverError 44 }

SELECT x, roundDown(x, [1]) FROM (SELECT 1 AS x);
SELECT x, roundDown(x, [1.5]) FROM (SELECT 1 AS x);

SELECT x, roundDown(x, (SELECT groupArray(number * 1.25) FROM numbers(100000))) FROM (SELECT number % 10 AS x FROM system.numbers LIMIT 10);

SELECT x, roundDown(x, [4, 5, 6]) FROM (SELECT toDecimal64(number, 5) / 100 AS x FROM system.numbers LIMIT 10);
SELECT x, roundDown(x, [toDecimal64(0.04, 5), toDecimal64(0.05, 5), toDecimal64(0.06, 5)]) FROM (SELECT toDecimal64(number, 5) / 100 AS x FROM system.numbers LIMIT 10);
SELECT x, roundDown(x, [toDecimal32(0.04, 2), toDecimal32(0.05, 2), toDecimal32(0.06, 2)]) FROM (SELECT toDecimal64(number, 5) / 100 AS x FROM system.numbers LIMIT 10);
