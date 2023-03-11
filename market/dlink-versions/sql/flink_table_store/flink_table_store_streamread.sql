CREATE CATALOG alldata_catalog WITH (
  'type'='table-store',
  'warehouse'='file:/tmp/table_store'
);
SET execution.runtime-mode = 'streaming';
SELECT `interval`, COUNT(*) AS interval_cnt FROM
  (SELECT cnt / 10000 AS `interval` FROM alldata_catalog.alldata_db.word_count) GROUP BY `interval`;