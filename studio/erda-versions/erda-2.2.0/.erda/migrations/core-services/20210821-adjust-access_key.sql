ALTER TABLE `erda_access_key` ADD `access_key` char(24) NOT NULL COMMENT 'Access Key ID, the column access_key_id is deprecated';
ALTER TABLE `erda_access_key` MODIFY COLUMN `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary Key';
ALTER TABLE `erda_access_key` MODIFY COLUMN `status` int NOT NULL DEFAULT 0 COMMENT 'status of access key';
ALTER TABLE `erda_access_key` MODIFY COLUMN `subject_type` int NOT NULL DEFAULT 0 COMMENT 'authentication subject type. eg: SYSTEM, MICRO_SERVICE';
ALTER TABLE `erda_access_key` ADD INDEX `idx_status` (`status`);