SET show_table_uuid_in_table_create_query_if_not_nil = 0;



DROP TABLE IF EXISTS default_table;

CREATE TABLE default_table (
  key UInt64 DEFAULT 42,
  value1 UInt64 MATERIALIZED key * key,
  value2 ALIAS value1 * key
)
ENGINE = CnchMergeTree()
ORDER BY tuple();

ALTER TABLE default_table MODIFY COLUMN key REMOVE MATERIALIZED; --{serverError 36}
ALTER TABLE default_table MODIFY COLUMN key REMOVE ALIAS; --{serverError 36}

ALTER TABLE default_table MODIFY COLUMN value1 REMOVE DEFAULT; --{serverError 36}
ALTER TABLE default_table MODIFY COLUMN value1 REMOVE ALIAS; --{serverError 36}

ALTER TABLE default_table MODIFY COLUMN value2 REMOVE DEFAULT; --{serverError 36}
ALTER TABLE default_table MODIFY COLUMN value2 REMOVE MATERIALIZED; --{serverError 36}

SHOW CREATE TABLE default_table;

DROP TABLE default_table;

