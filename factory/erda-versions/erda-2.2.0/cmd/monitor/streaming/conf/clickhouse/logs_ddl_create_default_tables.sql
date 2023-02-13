// create log table
CREATE TABLE IF NOT EXISTS <database>.logs ON CLUSTER '{cluster}'
(
    `_id` String,
    `timestamp` DateTime64(9, 'Asia/Shanghai'),
    `source` LowCardinality(String),
    `id` String,
    `org_name` LowCardinality(String),
    `tenant_id` LowCardinality(String),
    `group_id` String,
    `stream`  Enum8('' = 0, 'stdout' = 1, 'stderr' = 2),
    `offset` Int64,
    `content` String,
    `tags` Map(String,String),

    `tags.trace_id` String MATERIALIZED tags['trace_id'],
    `tags.level` LowCardinality(String) MATERIALIZED tags['level'],
    `tags.application_name` LowCardinality(String) MATERIALIZED tags['application_name'],
    `tags.service_name` String MATERIALIZED tags['service_name'],
    `tags.pod_name` String MATERIALIZED tags['pod_name'],
    `tags.pod_ip` String MATERIALIZED tags['pod_ip'],
    `tags.container_name` String MATERIALIZED tags['container_name'],
    `tags.container_id` String MATERIALIZED tags['container_id'],
    `tags.monitor_log_key` LowCardinality(String) MATERIALIZED tags['monitor_log_key'],
    `tags.msp_env_id` LowCardinality(String) MATERIALIZED tags['msp_env_id'],
    `tags.dice_application_id` LowCardinality(String) MATERIALIZED tags['dice_application_id'],

    INDEX idx__id(_id) TYPE minmax GRANULARITY 1,
    INDEX idx_trace_id(tags.trace_id) TYPE bloom_filter GRANULARITY 1,
    INDEX idx_id(id) TYPE bloom_filter GRANULARITY 1,
    INDEX idx_monitor_log_key(tags.monitor_log_key) TYPE bloom_filter GRANULARITY 1,
    INDEX idx_msp_env_id(tags.msp_env_id) TYPE bloom_filter GRANULARITY 1,
    INDEX idx_dice_application_id(tags.dice_application_id) TYPE bloom_filter GRANULARITY 1
)
ENGINE = ReplicatedMergeTree('/clickhouse/tables/{cluster}-{shard}/logs', '{replica}')
PARTITION BY toYYYYMMDD(timestamp)
ORDER BY (org_name, tenant_id, group_id, timestamp)
TTL toDateTime(timestamp) + INTERVAL <ttl_in_days> DAY;

// create distributed table
// notice: ddls to the logs table should be synced to the logs_all table
CREATE TABLE IF NOT EXISTS <database>.logs_all ON CLUSTER '{cluster}'
AS <database>.logs
    ENGINE = Distributed('{cluster}', <database>, logs, rand());