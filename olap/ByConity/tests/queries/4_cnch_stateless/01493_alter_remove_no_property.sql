SET show_table_uuid_in_table_create_query_if_not_nil = 0;



DROP TABLE IF EXISTS no_prop_table;

CREATE TABLE no_prop_table
(
    some_column UInt64
)
ENGINE CnchMergeTree()
ORDER BY tuple();

SHOW CREATE TABLE no_prop_table;
-- just nothing happened
ALTER TABLE no_prop_table MODIFY COLUMN some_column REMOVE DEFAULT; --{serverError 36}
ALTER TABLE no_prop_table MODIFY COLUMN some_column REMOVE MATERIALIZED; --{serverError 36}
ALTER TABLE no_prop_table MODIFY COLUMN some_column REMOVE ALIAS; --{serverError 36}
ALTER TABLE no_prop_table MODIFY COLUMN some_column REMOVE CODEC; --{serverError 36}
ALTER TABLE no_prop_table MODIFY COLUMN some_column REMOVE COMMENT; --{serverError 36}

ALTER TABLE no_prop_table REMOVE TTL; --{serverError 36}

SHOW CREATE TABLE no_prop_table;

DROP TABLE no_prop_table;
