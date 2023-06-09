DROP TABLE IF EXISTS test_alter_check;

CREATE TABLE test_alter_check (id UInt64, key UInt64 , tag String) ENGINE = CnchMergeTree() PARTITION BY id ORDER BY key;

ALTER TABLE test_alter_check RENAME COLUMN `id` to `uid`; -- { serverError 524 }
ALTER TABLE test_alter_check CLEAR COLUMN `tag`; -- { serverError 48 }

DROP TABLE IF EXISTS test_alter_check;
