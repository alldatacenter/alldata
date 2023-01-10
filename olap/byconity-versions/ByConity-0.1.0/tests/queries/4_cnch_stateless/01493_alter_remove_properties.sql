SET show_table_uuid_in_table_create_query_if_not_nil = 0;  -- show table without uuid

CREATE DATABASE IF NOT EXISTS db_01493;
USE db_01493;

DROP TABLE IF EXISTS prop_table;

CREATE TABLE prop_table
(
    column_default UInt64 DEFAULT 42,
    column_materialized UInt64 MATERIALIZED column_default * 42,
    column_alias UInt64 ALIAS column_default + 1,
    column_codec String CODEC(ZSTD(10)),
    column_comment Date COMMENT 'Some comment'
)
ENGINE CnchMergeTree()
PARTITION BY column_comment
ORDER BY tuple()
TTL column_comment + INTERVAL 2 MONTH;

SHOW CREATE TABLE prop_table;

INSERT INTO prop_table (column_codec, column_comment) VALUES ('str', today());

SELECT column_default, column_materialized, column_alias, column_codec FROM prop_table;

ALTER TABLE prop_table MODIFY COLUMN column_comment REMOVE COMMENT;

SHOW CREATE TABLE prop_table;

ALTER TABLE prop_table MODIFY COLUMN column_codec REMOVE CODEC;

SHOW CREATE TABLE prop_table;

ALTER TABLE prop_table MODIFY COLUMN column_alias REMOVE ALIAS;

SELECT column_default, column_materialized, column_alias, column_codec FROM prop_table;

SHOW CREATE TABLE prop_table;

INSERT INTO prop_table (column_alias, column_codec, column_comment) VALUES (33, 'trs', today() + 1);

SELECT column_default, column_materialized, column_alias, column_codec FROM prop_table ORDER BY column_comment;

ALTER TABLE prop_table MODIFY COLUMN column_materialized REMOVE MATERIALIZED;

SHOW CREATE TABLE prop_table;

INSERT INTO prop_table (column_materialized, column_alias, column_codec, column_comment) VALUES (11, 44, 'rts', today() + 2);

SELECT column_default, column_materialized, column_alias, column_codec FROM prop_table ORDER BY column_comment;

ALTER TABLE prop_table MODIFY COLUMN column_default REMOVE DEFAULT;

SHOW CREATE TABLE prop_table;

INSERT INTO prop_table (column_materialized, column_alias, column_codec, column_comment) VALUES (22, 55, 'tsr', today() + 3);

SELECT column_default, column_materialized, column_alias, column_codec FROM prop_table ORDER BY column_comment;

SHOW CREATE TABLE prop_table;

ALTER TABLE prop_table REMOVE TTL;

SHOW CREATE TABLE prop_table;

DROP TABLE prop_table;
