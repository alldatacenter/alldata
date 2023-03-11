set execution.checkpointing.interval=15sec;
CREATE CATALOG alldata_catalog WITH (
  'type'='table-store',
  'warehouse'='file:/tmp/table_store'
);
USE CATALOG alldata_catalog;
CREATE TABLE word_count (
    word STRING PRIMARY KEY NOT ENFORCED,
    cnt BIGINT
);
CREATE TEMPORARY TABLE word_table (
    word STRING
) WITH (
    'connector' = 'datagen',
    'fields.word.length' = '1'
);
INSERT INTO word_count SELECT word, COUNT(*) FROM word_table GROUP BY word;
-- POC Test OLAP QUERY
SET sql-client.execution.result-mode = 'tableau';
RESET execution.checkpointing.interval;
SET execution.runtime-mode = 'batch';
SELECT * FROM word_count;

-- POC Test Stream QUERY
-- SET execution.runtime-mode = 'streaming';
-- SELECT `interval`, COUNT(*) AS interval_cnt FROM
--   (SELECT cnt / 10000 AS `interval` FROM word_count) GROUP BY `interval`;
