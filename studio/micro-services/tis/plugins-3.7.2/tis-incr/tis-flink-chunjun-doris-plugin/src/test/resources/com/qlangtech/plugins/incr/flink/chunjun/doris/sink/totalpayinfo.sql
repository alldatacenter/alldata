
         CREATE TABLE `totalpayinfo` (
         `id` varchar(32) NULL COMMENT "",
         `update_time` DATETIME   NULL,
         `entity_id` varchar(10) NULL COMMENT "",
         `num` int(11) NULL COMMENT "",
         `create_time` bigint(20) NULL COMMENT "",
         `update_date` DATE       NULL,
         `start_time`  DATETIME   NULL
         ) ENGINE=OLAP
           UNIQUE KEY(`id`,`update_time`)
           DISTRIBUTED BY HASH(`id`)
           PROPERTIES (
             "replication_num" = "1"
           );
