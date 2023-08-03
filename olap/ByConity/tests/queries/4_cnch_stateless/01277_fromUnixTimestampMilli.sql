-- -- Error cases
SELECT fromUnixTimestampMilli();  -- {serverError 42}
SELECT fromUnixTimestampMilli('abc');  -- {serverError 43}
SELECT fromUnixTimestampMilli('abc', 123);  -- {serverError 43}
SELECT fromUnixTimestampMilli(4299262262295);  -- {serverError 69}

WITH
    CAST(1234567891011 AS Int64)  AS i64,
    CAST(1234567891011 AS UInt64) AS u64
SELECT
    i64, fromUnixTimestampMilli(i64, 'UTC'),
    u64, fromUnixTimestampMilli(u64, 'Asia/Makassar');

SELECT 'range bounds';
WITH
    CAST(-2208985199123 AS Int64) AS lb,
    CAST(4294967295123 AS Int64) AS ub
SELECT
    lb, fromUnixTimestampMilli(lb, 'UTC'),
    ub, fromUnixTimestampMilli(ub, 'UTC');

SELECT 'test compatibility';
WITH
    toDateTime64('2019-09-16 19:20:12', 0) AS dt64
SELECT
    dt64,
    fromUnixTimestampMilli(toUnixTimestamp64Milli(dt64));
WITH
    toDateTime64('2019-09-16 19:20:12.345678910', 3, 'Asia/Makassar') AS dt64
SELECT
    dt64,
    fromUnixTimestampMilli(toUnixTimestamp64Milli(dt64), 'Asia/Makassar');
WITH
    CAST(1234567891011 AS Int64) AS val
SELECT
    val,
    toUnixTimestamp64Milli(toDateTime64(fromUnixTimestampMilli(val), 0)),
    toUnixTimestamp64Milli(toDateTime64(fromUnixTimestampMilli(val, 'UTC'), 0));
