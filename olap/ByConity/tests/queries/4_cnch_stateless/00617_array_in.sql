
DROP TABLE IF EXISTS test_array_ops;
CREATE TABLE test_array_ops(arr Array(Nullable(Int64))) ENGINE = CnchMergeTree ORDER BY tuple();
INSERT INTO test_array_ops(arr) values ([null, 10, -20]);
INSERT INTO test_array_ops(arr) values ([10, -20]);
INSERT INTO test_array_ops(arr) values ([]);
SELECT sleep(3) FORMAT Null;
SELECT count(*) FROM test_array_ops where arr < CAST([10, -20] AS Array(Nullable(Int64)));
SELECT count(*) FROM test_array_ops where arr > CAST([10, -20] AS Array(Nullable(Int64)));
SELECT sleep(3) FORMAT Null;
SELECT count(*) FROM test_array_ops where arr >= CAST([10, -20] AS Array(Nullable(Int64)));
SELECT count(*) FROM test_array_ops where arr <= CAST([10, -20] AS Array(Nullable(Int64)));
SELECT sleep(3) FORMAT Null;
SELECT count(*) FROM test_array_ops where arr = CAST([10, -20] AS Array(Nullable(Int64)));
SELECT sleep(3) FORMAT Null;
SELECT sleep(3) FORMAT Null;
SELECT count(*) FROM test_array_ops where arr IN( CAST([10, -20] AS Array(Nullable(Int64))), CAST([null,10, -20] AS Array(Nullable(Int64))));
DROP TABLE test_array_ops;
