CREATE TABLE customer_order_relation
(
    `customerregister_id`   VARCHAR(150),
    `waitingorder_id`       VARCHAR(150),
    `kind`                  BIGINT,
    `create_time`           BIGINT,
    `last_ver`              BIGINT
)
 ENGINE=olap
PRIMARY KEY(`customerregister_id`,`waitingorder_id`)
DISTRIBUTED BY HASH(customerregister_id,waitingorder_id)
BUCKETS 10
PROPERTIES("replication_num" = "1")
