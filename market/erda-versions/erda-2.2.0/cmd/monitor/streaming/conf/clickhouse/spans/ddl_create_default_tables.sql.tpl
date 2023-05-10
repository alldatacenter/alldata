CREATE TABLE IF NOT EXISTS <database>.spans_series ON CLUSTER '{cluster}'
(
  `org_name` LowCardinality(String),
  `series_id` UInt64,
  `trace_id` String,
  `span_id` String,
	`parent_span_id` String,
  `start_time` DateTime64(9,'Asia/Shanghai') CODEC(DoubleDelta),
  `end_time` DateTime64(9,'Asia/Shanghai') CODEC(DoubleDelta),
  `tags` Map(String,String),
  INDEX idx_trace_id(trace_id) TYPE bloom_filter GRANULARITY 1
)
ENGINE = ReplicatedMergeTree('/clickhouse/tables/{cluster}-{shard}/{database}/spans_series', '{replica}')
PARTITION BY toYYYYMMDD(end_time)
ORDER BY (org_name, start_time, end_time, series_id)
TTL toDateTime(end_time) + INTERVAL <ttl_in_days> DAY;

// add materialized column&index for high cardinality tag
ALTER TABLE <database>.spans_series ADD COLUMN IF NOT EXISTS `tags.http_path` String MATERIALIZED tags['http_path'];
ALTER TABLE <database>.spans_series ADD INDEX IF NOT EXISTS idx_http_path(tags.http_path) TYPE bloom_filter GRANULARITY 1;

CREATE TABLE IF NOT EXISTS <database>.spans_meta ON CLUSTER '{cluster}'
(
  `org_name` LowCardinality(String),
  `series_id` UInt64,
  `key` LowCardinality(String),
  `value` String,
  `create_at` DateTime64(9,'Asia/Shanghai') CODEC(DoubleDelta),
  INDEX idx_series_id(series_id) TYPE bloom_filter GRANULARITY 1
)
ENGINE = ReplicatedReplacingMergeTree('/clickhouse/tables/{cluster}-{shard}/{database}/spans_meta', '{replica}')
PARTITION BY toYYYYMMDD(create_at)
ORDER BY (org_name, key, value, series_id)
TTL toDateTime(create_at) + INTERVAL <ttl_in_days>*2 DAY;

// create distributed table
// notice: ddls to the <table> table should be synced to the <table>_all table
CREATE TABLE IF NOT EXISTS <database>.spans_meta_all ON CLUSTER '{cluster}' AS <database>.spans_meta
ENGINE = Distributed('{cluster}', <database>, spans_meta, rand());

CREATE TABLE IF NOT EXISTS <database>.spans_series_all ON CLUSTER '{cluster}' AS <database>.spans_series
ENGINE = Distributed('{cluster}', <database>, spans_series, rand());