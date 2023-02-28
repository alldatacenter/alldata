CREATE TABLE customer_order_relation
(
    `customerregister_id`   String,
    `waitingorder_id`       String,
    `kind`                  Int64,
    `create_time`           Int64,
    `last_ver`              Int64,
    `__cc_ck_sign` Int8 DEFAULT 1
)
 ENGINE = CollapsingMergeTree(__cc_ck_sign)
 ORDER BY `customerregister_id`
 SETTINGS index_granularity = 8192