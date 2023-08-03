CREATE TABLE `table_blocker` (
  `blocker_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Blocker unique id',
  `catalog_name` varchar(64) NOT NULL COMMENT 'Catalog name',
  `db_name` varchar(128) NOT NULL COMMENT 'Database name',
  `table_name` varchar(128) NOT NULL COMMENT 'Table name',
  `operations` varchar(128) NOT NULL COMMENT 'Blocked operations',
  `create_time` datetime(3) DEFAULT NULL COMMENT 'Blocker create time',
  `expiration_time` datetime(3) DEFAULT NULL COMMENT 'Blocker expiration time',
  `properties` mediumtext COMMENT 'Blocker properties',
  PRIMARY KEY (`blocker_id`),
  KEY `table_index` (`catalog_name`,`db_name`,`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Table blockers';

ALTER TABLE `optimize_group` ADD COLUMN `scheduling_policy`   varchar(20) COMMENT 'Optimize group scheduling policy' after `name`;
ALTER TABLE `optimize_task` CHANGE `max_change_transaction_id` `to_sequence` bigint(20) NOT NULL DEFAULT '-1' COMMENT 'to sequence';
ALTER TABLE `optimize_task` CHANGE `min_change_transaction_id` `from_sequence` bigint(20) NOT NULL DEFAULT '-1' COMMENT 'from sequence';
ALTER TABLE `optimize_history` CHANGE `max_change_transaction_id` `partition_optimized_sequence` mediumtext COMMENT 'optimized sequence of these tasks';
