ALTER TABLE `pipeline_archives` ADD INDEX `idx_status_time_created` (`status`, `time_created`);
ALTER TABLE `pipeline_archives` ADD INDEX `idx_status` (`status`);
ALTER TABLE `pipeline_archives` ADD INDEX `idx_time_created` (`time_created`);