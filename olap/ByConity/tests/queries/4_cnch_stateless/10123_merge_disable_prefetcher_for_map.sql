DROP TABLE IF EXISTS test.only_map_col;
CREATE TABLE test.only_map_col (m Map(String, String)) Engine=CnchMergeTree() ORDER BY tuple();
INSERT INTO test.only_map_col FORMAT JSONEachRow {"m": {"name": "cnch"}};
INSERT INTO test.only_map_col FORMAT JSONEachRow {"m": {"name": "bytehouse"}};
SELECT * FROM test.only_map_col ORDER BY m{'name'};

SYSTEM START MERGES test.only_map_col;
SYSTEM STOP MERGES test.only_map_col;
-- USE mutaions_sync = 1 after it's avaiable.
OPTIMIZE TABLE test.only_map_col;
SELECT sleep(3) FORMAT Null;

SELECT '-- AFTER MERGE --';

SELECT * FROM test.only_map_col ORDER BY m{'name'};

DROP TABLE test.only_map_col;
