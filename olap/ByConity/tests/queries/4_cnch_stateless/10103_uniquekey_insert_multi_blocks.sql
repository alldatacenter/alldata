DROP TABLE IF EXISTS u10103_mb;

CREATE TABLE u10103_mb (k1 String, k2 String, v Int64) ENGINE = CnchMergeTree() ORDER BY v UNIQUE KEY (k1, k2);

SYSTEM START MERGES u10103_mb;
SYSTEM STOP MERGES u10103_mb;

SET max_block_size = 1024; -- make the size of blocks produces by `system.numbers` to be 1024
SET min_insert_block_size_rows = 1024; -- make SquashingBlockOutputStream produce block for every 1024 rows
SET max_string_size_for_unique_key = 9; -- throw exception (and abort txn) if size of key columns > 9

SELECT '# test commit of multi-block txn';
INSERT INTO u10103_mb SELECT '', toString(number), number FROM system.numbers LIMIT 10240;
SELECT count(1), sum(v) FROM u10103_mb;
SELECT 'num parts', count(1) FROM system.cnch_parts WHERE database=currentDatabase() and table='u10103_mb' and active;

SELECT '# test rollback of multi-block txn';
-- set k1 to 'kkkkk' so that the total key size will exceed max_string_size_for_unique_key for the last block
INSERT INTO u10103_mb SELECT 'kkkkk', toString(number), number FROM system.numbers LIMIT 10240; -- { serverError 651 }
SELECT count(1), sum(v) FROM u10103_mb;
SELECT 'num parts', count(1) FROM system.cnch_parts WHERE database=currentDatabase() and table='u10103_mb' and active;

DROP TABLE IF EXISTS u10103_mb;
