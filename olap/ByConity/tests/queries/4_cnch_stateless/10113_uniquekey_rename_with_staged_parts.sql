DROP TABLE IF EXISTS u10113_rename;
DROP TABLE IF EXISTS u10113_rename2;

set enable_staging_area_for_write = 1;
CREATE TABLE u10113_rename (c1 Int32, c2 Int32, k1 UInt32, k2 String, v1 UInt32)
ENGINE = CnchMergeTree()
ORDER BY (k1, intHash64(k1))
UNIQUE KEY (k1, k2)
SAMPLE BY intHash64(k1);

INSERT INTO u10113_rename VALUES (0, 100, 1, 'a', 1), (1, 101, 1, 'b', 1), (2, 102, 1, 'c', 1);
INSERT INTO u10113_rename VALUES (3, 103, 1, 'b', 1), (4, 104, 2, 'b', 1), (5, 105, 2, 'a', 1);

SELECT '-- before rename --';
system sync dedup worker u10113_rename;
SELECT 'dedup worker status:', table, is_active from system.cnch_dedup_workers where database=currentDatabase() and table='u10113_rename';
SELECT '#staged parts:', count() FROM system.cnch_staged_parts where database=currentDatabase() and table = 'u10113_rename' and to_publish;
SELECT '#parts:', count() FROM system.cnch_parts where database=currentDatabase() and table='u10113_rename' and active;
SELECT * FROM u10113_rename order by k1, k2;

SELECT '-- rename in same database and insert some values';
RENAME TABLE u10113_rename TO u10113_rename2;
INSERT INTO u10113_rename2 VALUES (6, 106, 3, 'a', 1), (7, 107, 3, 'b', 1), (8, 108, 2, 'a', 1);

SELECT '-- after rename --';
system sync dedup worker u10113_rename2;
SELECT '#staged parts:', count() FROM system.cnch_staged_parts where database=currentDatabase() and table = 'u10113_rename2' and to_publish;
SELECT '#parts:', count() FROM system.cnch_parts where database=currentDatabase() and table='u10113_rename2' and active;
SELECT * FROM u10113_rename2 order by k1, k2;

DROP TABLE IF EXISTS u10113_rename;
DROP TABLE IF EXISTS u10113_rename2;