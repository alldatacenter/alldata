
DROP TABLE IF EXISTS nulls;
CREATE TABLE nulls (d Date, x Nullable(UInt64)) ENGINE = CnchMergeTree() PARTITION BY toYYYYMM(d) ORDER BY d SETTINGS index_granularity = 8192;
INSERT INTO nulls SELECT toDate('2000-01-01'), number % 10 != 0 ? number : NULL FROM system.numbers LIMIT 10000;
SELECT count() FROM nulls WHERE x IS NULL;
DROP TABLE nulls;
