DROP TABLE IF EXISTS u10108_pl;

CREATE TABLE u10108_pl (d Date, k1 Int64, k2 Int64, v1 Int64) ENGINE = CnchMergeTree() PARTITION BY d ORDER BY k1 UNIQUE KEY (k1, k2);

INSERT INTO u10108_pl SELECT '2021-01-01', 1, number, number FROM system.numbers LIMIT 100;
INSERT INTO u10108_pl SELECT '2021-01-01', 2, number, number FROM system.numbers LIMIT 100;
INSERT INTO u10108_pl SELECT '2021-01-02', 1, number, number FROM system.numbers LIMIT 100;
INSERT INTO u10108_pl SELECT '2021-01-02', 2, number, number FROM system.numbers LIMIT 100;
INSERT INTO u10108_pl SELECT '2021-01-03', 1, number, number FROM system.numbers LIMIT 100;
INSERT INTO u10108_pl SELECT '2021-01-03', 2, number, number FROM system.numbers LIMIT 100;

SELECT d, k1, count(1), sum(v1) FROM u10108_pl GROUP BY d, k1 ORDER BY d, k1;

-- detach/attach partition is not supported
ALTER TABLE u10108_pl DETACH PARTITION '2021-01-01'; -- { serverError 48 }
ALTER TABLE u10108_pl ATTACH PARTITION '2021-01-01'; -- { serverError 48 }

-- drop partition
SELECT 'drop partition 2021-01-01';
ALTER TABLE u10108_pl DROP PARTITION '2021-01-01';
INSERT INTO u10108_pl SELECT '2021-01-01', 3, number, number FROM system.numbers LIMIT 100;
SELECT d, k1, count(1), sum(v1) FROM u10108_pl GROUP BY d, k1 ORDER BY d, k1;

-- drop partition where
SELECT 'drop partition where d=\'2021-01-02\'';
ALTER TABLE u10108_pl DROP PARTITION WHERE d = '2021-01-02';
INSERT INTO u10108_pl SELECT '2021-01-02', 3, number, number FROM system.numbers LIMIT 100;
SELECT d, k1, count(1), sum(v1) FROM u10108_pl GROUP BY d, k1 ORDER BY d, k1;

DROP TABLE IF EXISTS u10108_pl;
