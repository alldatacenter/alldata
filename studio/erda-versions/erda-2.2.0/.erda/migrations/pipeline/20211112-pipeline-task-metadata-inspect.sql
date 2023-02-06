ALTER TABLE `pipeline_tasks` MODIFY `result` mediumtext COMMENT 'task的结果元数据';
ALTER TABLE `pipeline_tasks` ADD COLUMN `inspect` mediumtext COMMENT 'task的调度信息' AFTER `result`;