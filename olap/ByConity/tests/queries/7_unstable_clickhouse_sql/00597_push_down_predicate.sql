USE test;
SET send_logs_level = 'none';

DROP TABLE IF EXISTS test.test_view;
DROP TABLE IF EXISTS test.push_down_predicate;

CREATE TABLE test.push_down_predicate(date Date, id Int8, name String, value Int64) ENGINE = CnchMergeTree() PARTITION BY toYYYYMM(date) ORDER BY (id, date);
CREATE VIEW test.test_view AS SELECT * FROM test.push_down_predicate;

INSERT INTO test.push_down_predicate VALUES('2000-01-01', 1, 'test string 1', 1);
INSERT INTO test.push_down_predicate VALUES('2000-01-01', 2, 'test string 2', 2);

SET enable_optimize_predicate_expression = 1;
SET enable_debug_queries = 1;

SELECT '-------No need for predicate optimization, but still works-------';
SELECT 1;
SELECT 1 AS id WHERE id = 1;
SELECT arrayJoin([1,2,3]) AS id WHERE id = 1;
SELECT * FROM test.push_down_predicate WHERE id = 1;

SELECT '-------Need push down-------';

-- Optimize predicate expressions without tables
ANALYZE SELECT * FROM system.one ANY LEFT JOIN (SELECT 0 AS dummy) USING dummy WHERE 1;
SELECT * FROM system.one ANY LEFT JOIN (SELECT 0 AS dummy) USING dummy WHERE 1;

ANALYZE SELECT toString(value) AS value FROM (SELECT 1 AS value) WHERE value = '1';
SELECT toString(value) AS value FROM (SELECT 1 AS value) WHERE value = '1';

ANALYZE SELECT * FROM (SELECT 1 AS id UNION ALL SELECT 2) WHERE id = 1;
SELECT * FROM (SELECT 1 AS id UNION ALL SELECT 2) WHERE id = 1;

ANALYZE SELECT * FROM (SELECT arrayJoin([1, 2, 3]) AS id) WHERE id = 1;
SELECT * FROM (SELECT arrayJoin([1, 2, 3]) AS id) WHERE id = 1;

ANALYZE SELECT id FROM (SELECT arrayJoin([1, 2, 3]) AS id) WHERE id = 1;
SELECT id FROM (SELECT arrayJoin([1, 2, 3]) AS id) WHERE id = 1;

ANALYZE SELECT * FROM (SELECT 1 AS id, (SELECT 1) as subquery) WHERE subquery = 1;
SELECT * FROM (SELECT 1 AS id, (SELECT 1) as subquery) WHERE subquery = 1;

-- Optimize predicate expressions using tables
ANALYZE SELECT * FROM (SELECT toUInt64(b) AS a, sum(id) AS b FROM test.push_down_predicate) WHERE a = 3;
SELECT * FROM (SELECT toUInt64(b) AS a, sum(id) AS b FROM test.push_down_predicate) WHERE a = 3;

ANALYZE SELECT date, id, name, value FROM (SELECT date, name, value, min(id) AS id FROM test.push_down_predicate GROUP BY date, name, value) WHERE id = 1;
SELECT date, id, name, value FROM (SELECT date, name, value, min(id) AS id FROM test.push_down_predicate GROUP BY date, name, value) WHERE id = 1;

ANALYZE SELECT * FROM (SELECT toUInt64(b) AS a, sum(id) AS b FROM test.push_down_predicate AS table_alias) AS outer_table_alias WHERE outer_table_alias.b = 3;
SELECT * FROM (SELECT toUInt64(b) AS a, sum(id) AS b FROM test.push_down_predicate AS table_alias) AS outer_table_alias WHERE outer_table_alias.b = 3;

-- Optimize predicate expression with asterisk
ANALYZE SELECT * FROM (SELECT * FROM test.push_down_predicate) WHERE id = 1;
SELECT * FROM (SELECT * FROM test.push_down_predicate) WHERE id = 1;

-- Optimize predicate expression with asterisk and nested subquery
ANALYZE SELECT * FROM (SELECT * FROM (SELECT * FROM test.push_down_predicate)) WHERE id = 1;
SELECT * FROM (SELECT * FROM (SELECT * FROM test.push_down_predicate)) WHERE id = 1;

-- Optimize predicate expression with qualified asterisk
ANALYZE SELECT * FROM (SELECT b.* FROM (SELECT * FROM test.push_down_predicate) AS b) WHERE id = 1;
SELECT * FROM (SELECT b.* FROM (SELECT * FROM test.push_down_predicate) AS b) WHERE id = 1;

-- Optimize predicate expression without asterisk
ANALYZE SELECT * FROM (SELECT date, id, name, value FROM test.push_down_predicate) WHERE id = 1;
SELECT * FROM (SELECT date, id, name, value FROM test.push_down_predicate) WHERE id = 1;

-- Optimize predicate expression without asterisk and contains nested subquery
ANALYZE SELECT * FROM (SELECT date, id, name, value FROM (SELECT date, id, name, value FROM test.push_down_predicate)) WHERE id = 1;
SELECT * FROM (SELECT date, id, name, value FROM (SELECT date, id, name, value FROM test.push_down_predicate)) WHERE id = 1;

-- Optimize predicate expression with qualified
ANALYZE SELECT * FROM (SELECT * FROM test.push_down_predicate) AS b WHERE b.id = 1;
SELECT * FROM (SELECT * FROM test.push_down_predicate) AS b WHERE b.id = 1;

-- Optimize predicate expression with qualified and nested subquery
ANALYZE SELECT * FROM (SELECT * FROM (SELECT * FROM test.push_down_predicate) AS a) AS b WHERE b.id = 1;
SELECT * FROM (SELECT * FROM (SELECT * FROM test.push_down_predicate) AS a) AS b WHERE b.id = 1;

-- Optimize predicate expression with aggregate function
ANALYZE SELECT * FROM (SELECT id, date, min(value) AS value FROM test.push_down_predicate GROUP BY id, date) WHERE id = 1;
SELECT * FROM (SELECT id, date, min(value) AS value FROM test.push_down_predicate GROUP BY id, date) WHERE id = 1;

-- Optimize predicate expression with union all query
ANALYZE SELECT * FROM (SELECT * FROM test.push_down_predicate UNION ALL SELECT * FROM test.push_down_predicate) WHERE id = 1;
SELECT * FROM (SELECT * FROM test.push_down_predicate UNION ALL SELECT * FROM test.push_down_predicate) WHERE id = 1;

-- Optimize predicate expression with join query
ANALYZE SELECT * FROM (SELECT * FROM test.push_down_predicate) ANY LEFT JOIN (SELECT * FROM test.push_down_predicate) USING id WHERE id = 1;
SELECT * FROM (SELECT * FROM test.push_down_predicate) ANY LEFT JOIN (SELECT * FROM test.push_down_predicate) USING id WHERE id = 1;

ANALYZE SELECT * FROM (SELECT toInt8(1) AS id) ANY LEFT JOIN test.push_down_predicate USING id WHERE value = 1;
SELECT * FROM (SELECT toInt8(1) AS id) ANY LEFT JOIN test.push_down_predicate USING id WHERE value = 1;

-- FIXME: no support for aliased tables for now.
ANALYZE SELECT b.value FROM (SELECT toInt8(1) AS id) ANY LEFT JOIN test.push_down_predicate AS b USING id WHERE value = 1;
SELECT b.value FROM (SELECT toInt8(1) AS id) ANY LEFT JOIN test.push_down_predicate AS b USING id WHERE value = 1;

-- Optimize predicate expression with join and nested subquery
ANALYZE SELECT * FROM (SELECT * FROM (SELECT * FROM test.push_down_predicate) ANY LEFT JOIN (SELECT * FROM test.push_down_predicate) USING id) WHERE id = 1;
SELECT * FROM (SELECT * FROM (SELECT * FROM test.push_down_predicate) ANY LEFT JOIN (SELECT * FROM test.push_down_predicate) USING id) WHERE id = 1;

-- Optimize predicate expression with join query and qualified
ANALYZE SELECT * FROM (SELECT * FROM test.push_down_predicate) ANY LEFT JOIN (SELECT * FROM test.push_down_predicate) AS b USING id WHERE b.id = 1;
SELECT * FROM (SELECT * FROM test.push_down_predicate) ANY LEFT JOIN (SELECT * FROM test.push_down_predicate) AS b USING id WHERE b.id = 1;

-- Compatibility test
ANALYZE SELECT * FROM (SELECT toInt8(1) AS id, toDate('2000-01-01') AS date FROM system.numbers LIMIT 1) ANY LEFT JOIN (SELECT * FROM test.push_down_predicate) AS b USING date, id WHERE b.date = toDate('2000-01-01');
SELECT * FROM (SELECT toInt8(1) AS id, toDate('2000-01-01') AS date FROM system.numbers LIMIT 1) ANY LEFT JOIN (SELECT * FROM test.push_down_predicate) AS b USING date, id WHERE b.date = toDate('2000-01-01');

ANALYZE SELECT * FROM (SELECT * FROM (SELECT * FROM test.push_down_predicate) AS a ANY LEFT JOIN (SELECT * FROM test.push_down_predicate) AS b  ON  a.id = b.id) WHERE id = 1;
SELECT * FROM (SELECT * FROM (SELECT * FROM test.push_down_predicate) AS a ANY LEFT JOIN (SELECT * FROM test.push_down_predicate) AS b  ON  a.id = b.id) WHERE id = 1;


DROP TABLE IF EXISTS test.test_view;
DROP TABLE IF EXISTS test.push_down_predicate;
