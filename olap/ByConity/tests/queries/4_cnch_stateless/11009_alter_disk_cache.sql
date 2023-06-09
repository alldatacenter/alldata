DROP TABLE IF EXISTS at_dc;
CREATE TABLE at_dc(a UInt32, p UInt32) ENGINE = CnchMergeTree ORDER BY a PARTITION BY p;

INSERT INTO at_dc SELECT number, 1 FROM numbers(10) SETTINGS enable_preload_parts=1, enable_async_preload_parts=0;

SELECT a FROM at_dc WHERE p = 1 ORDER BY a SETTINGS disk_cache_mode='FORCE_CHECKSUMS_DISK_CACHE';

DROP TABLE at_dc;
