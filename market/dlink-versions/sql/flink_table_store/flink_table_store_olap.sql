CREATE CATALOG alldata_catalog WITH (
  'type'='table-store',
  'warehouse'='file:/tmp/table_store'
);

-- SET sql-client.execution.result-mode = 'tableau';
-- RESET execution.checkpointing.interval;
-- SET execution.runtime-mode = 'batch';
SELECT * FROM alldata_catalog.alldata_db.word_count;