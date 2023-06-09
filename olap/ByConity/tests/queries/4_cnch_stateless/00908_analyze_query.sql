
set enable_debug_queries = 1;

DROP TABLE IF EXISTS a;
CREATE TABLE a (a UInt8, b UInt8) ENGINE CnchMergeTree ORDER BY a;

EXPLAIN SYNTAX SELECT * FROM a;

DROP TABLE a;
