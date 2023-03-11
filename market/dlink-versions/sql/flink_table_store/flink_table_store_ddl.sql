set execution.checkpointing.interval=15sec;
CREATE CATALOG alldata_catalog WITH (
  'type'='table-store',
  'warehouse'='file:/tmp/table_store'
);
USE CATALOG alldata_catalog;
create database alldata_db;
use alldata_db;
CREATE TABLE IF NOT EXISTS alldata_db.word_count (
    word STRING PRIMARY KEY NOT ENFORCED,
    cnt BIGINT
);
CREATE TEMPORARY TABLE IF NOT EXISTS alldata_db.word_table (
    word STRING
) WITH (
    'connector' = 'datagen',
    'fields.word.length' = '1'
);
INSERT INTO alldata_db.word_count SELECT word, COUNT(*) FROM alldata_db.word_table GROUP BY word;
