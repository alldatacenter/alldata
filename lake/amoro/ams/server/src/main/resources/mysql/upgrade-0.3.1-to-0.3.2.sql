DROP TABLE IF EXISTS `optimize_task_history`;
CREATE TABLE `optimize_task_history`
(
    `task_trace_id`     varchar(50) NOT NULL COMMENT 'Optimize task uuid',
    `retry`             int(11) NOT NULL COMMENT 'Retry times for the same task_trace_id',
    `task_plan_group`   varchar(40) NOT NULL COMMENT 'Plan group of task, task of one plan group are planned together',
    `catalog_name`      varchar(64) NOT NULL COMMENT 'Catalog name',
    `db_name`           varchar(64) NOT NULL COMMENT 'Database name',
    `table_name`        varchar(64) NOT NULL COMMENT 'Table name',
    `start_time`        datetime(3) DEFAULT NULL COMMENT 'Task start time',
    `end_time`          datetime(3) DEFAULT NULL COMMENT 'Task end time',
    `cost_time`         bigint(20) DEFAULT NULL COMMENT 'Task cost time',
    `queue_id`          int(11) DEFAULT NULL COMMENT 'Queue id which execute task',
    PRIMARY KEY (`task_trace_id`, `retry`),
    KEY `table_end_time_plan_group_index` (`catalog_name`, `db_name`, `table_name`, `end_time`, `task_plan_group`),
    KEY `table_plan_group_index` (`catalog_name`, `db_name`, `table_name`, `task_plan_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'History of each optimize task execute';

ALTER TABLE `optimize_task` DROP COLUMN `is_delete_pos_delete`;
ALTER TABLE `optimize_task` CHANGE COLUMN `task_group` `task_commit_group` varchar(40) DEFAULT NULL COMMENT 'UUID. Commit group of task, task of one commit group should commit together';
ALTER TABLE `optimize_task` CHANGE COLUMN `task_history_id` `task_plan_group` varchar(40) DEFAULT NULL COMMENT 'UUID. Plan group of task, task of one plan group are planned together';
ALTER TABLE `optimize_table_runtime` CHANGE COLUMN `latest_task_history_id` `latest_task_plan_group` varchar(40) DEFAULT NULL COMMENT 'Latest task plan group';
ALTER TABLE `optimize_job` CHANGE COLUMN `job_id` `optimizer_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT;
ALTER TABLE `optimize_job` CHANGE COLUMN `job_name` `optimizer_name` varchar(1024) DEFAULT NULL COMMENT 'optimizer name';
ALTER TABLE `optimize_job` CHANGE COLUMN `job_start_time` `optimizer_start_time` varchar(1024) DEFAULT NULL COMMENT 'optimizer start time';
ALTER TABLE `optimize_job` CHANGE COLUMN `job_fail_time` `optimizer_fail_time` varchar(1024) DEFAULT NULL COMMENT 'optimizer fail time';
ALTER TABLE `optimize_job` CHANGE COLUMN `job_status` `optimizer_status` varchar(16) DEFAULT NULL COMMENT 'optimizer status';
ALTER TABLE `optimize_job` RENAME TO `optimizer`;