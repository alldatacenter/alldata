
drop table if exists table;

CREATE TABLE table (a UInt32,  date Date, b UInt64,  c UInt64, str String, d Int8, arr Array(UInt64), arr_alias Array(UInt64) ALIAS arr) ENGINE = CnchMergeTree() PARTITION BY toYYYYMM(date) SAMPLE BY intHash32(c) ORDER BY (a, date, intHash32(c), b) SETTINGS index_granularity = 8192;

SELECT alias2 AS alias3
FROM table
ARRAY JOIN
    arr_alias AS alias2,
    arrayEnumerateUniq(arr_alias) AS _uniq_Event
WHERE (date = toDate('2010-10-10')) AND (a IN (2, 3)) AND (str NOT IN ('z', 'x')) AND (d != -1)
LIMIT 1;

drop table if exists table;

