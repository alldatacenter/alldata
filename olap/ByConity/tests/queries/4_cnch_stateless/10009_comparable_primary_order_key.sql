
DROP TABLE IF EXISTS test_comparable;

CREATE TABLE test_comparable (d UInt8, m Map(String, String)) Engine=CnchMergeTree PARTITION BY m ORDER BY d; -- { serverError 549 }
CREATE TABLE test_comparable (d UInt8, m Map(String, String)) Engine=CnchMergeTree PARTITION BY (d, m) ORDER BY d; -- { serverError 549 }

CREATE TABLE test_comparable (d UInt8, m Map(String, String)) Engine=CnchMergeTree ORDER BY m; -- { serverError 549 }
CREATE TABLE test_comparable (d UInt8, m Map(String, String)) Engine=CnchMergeTree ORDER BY (d,m); -- { serverError 549 }

CREATE TABLE test_comparable (d UInt8, m Map(String, String)) Engine=CnchMergeTree ORDER BY d PRIMARY KEY m; -- { serverError 549 }
CREATE TABLE test_comparable (d UInt8, m Map(String, String)) Engine=CnchMergeTree ORDER BY d PRIMARY KEY (d, m); -- { serverError 549 }

CREATE TABLE test_comparable (d UInt8, m Map(String, String)) Engine=CnchMergeTree ORDER BY d;
DROP TABLE test_comparable;
