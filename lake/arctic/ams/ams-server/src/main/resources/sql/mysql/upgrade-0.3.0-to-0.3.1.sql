CREATE TABLE `api_tokens`
(
    `id`         int(11) NOT NULL AUTO_INCREMENT,
    `apikey`     varchar(256) NOT NULL COMMENT 'openapi client public key',
    `secret`     varchar(256) NOT NULL COMMENT 'The key used by the client to generate the request signature',
    `apply_time` datetime DEFAULT NULL COMMENT 'apply time',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `account_unique` (`apikey`) USING BTREE COMMENT 'account unique'
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='Openapi  secret';

CREATE TABLE `ddl_record`
(
    `table_identifier` varchar(384) NOT NULL COMMENT 'table full name with catalog.db.table',
    `ddl`              mediumtext COMMENT 'ddl',
    `ddl_type`         varchar(256) NOT NULL COMMENT 'ddl type',
    `commit_time`      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'ddl commit time'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'ddl record of table';

ALTER TABLE `snapshot_info_cache` ADD COLUMN `producer` varchar(64) NOT NULL DEFAULT 'INGESTION' COMMENT 'who produce this snapshot';
ALTER TABLE `snapshot_info_cache` ADD COLUMN `file_size` bigint(20) NOT NULL DEFAULT 0 COMMENT 'file size';
ALTER TABLE `snapshot_info_cache` ADD COLUMN `file_count` int(11) NOT NULL DEFAULT 0 COMMENT 'file count';
ALTER TABLE `snapshot_info_cache` modify COLUMN `table_identifier` varchar(384) NOT NULL;
ALTER TABLE `file_info_cache` ADD COLUMN `producer` varchar(64) NOT NULL DEFAULT 'INGESTION' COMMENT 'who produce this snapshot';
ALTER TABLE `file_info_cache` modify COLUMN `table_identifier` varchar(384) NOT NULL;
ALTER TABLE `table_metadata` ADD COLUMN `cur_schema_id` int(11) NOT NULL DEFAULT 0 COMMENT 'current schema id';
ALTER TABLE `table_transaction_meta` modify COLUMN `table_identifier` varchar(384) NOT NULL;
ALTER TABLE `optimize_file` MODIFY COLUMN `optimize_type` varchar(10) NOT NULL COMMENT 'Optimize type: Major, Minor, FullMajor';
ALTER TABLE `optimize_table_runtime` ADD COLUMN `latest_full_optimize_time` MEDIUMTEXT NULL COMMENT 'Latest Full Optimize time for all partitions';
ALTER TABLE `optimize_task_history` MODIFY COLUMN `task_history_id` varchar(40) NOT NULL COMMENT 'Task history id' first, MODIFY COLUMN 
`task_group_id` varchar(40) NOT NULL COMMENT 'Task group id' after `task_history_id`;
ALTER TABLE `optimize_task_history` ADD PRIMARY KEY (`task_history_id`,`task_group_id`);
