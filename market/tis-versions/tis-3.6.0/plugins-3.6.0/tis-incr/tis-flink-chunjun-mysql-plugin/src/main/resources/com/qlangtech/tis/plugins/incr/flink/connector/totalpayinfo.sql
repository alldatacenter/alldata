
CREATE TABLE `totalpayinfo` (
   `id` varchar(32) NOT Null COMMENT "",
   `entity_id` varchar(10) NULL COMMENT "",
   `num` int(11) NULL COMMENT "",
   `create_time` bigint(20) NULL COMMENT "",
   `update_time` DATETIME   NULL,
   `update_date` DATE       NULL,
   `start_time`  DATETIME   NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
