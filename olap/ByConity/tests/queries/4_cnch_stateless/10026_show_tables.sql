DROP DATABASE IF EXISTS test_show_tables;

CREATE DATABASE test_show_tables;

DROP TABLE IF EXISTS test_show_tables.A;
DROP TABLE IF EXISTS test_show_tables.B;

CREATE TABLE test_show_tables.A (A UInt8) ENGINE=CnchMergeTree() ORDER BY A;
CREATE TABLE test_show_tables.B (A UInt8) ENGINE=CnchMergeTree() ORDER BY A;

SHOW TABLES from test_show_tables;

DROP TABLE test_show_tables.A;
DROP TABLE test_show_tables.B;