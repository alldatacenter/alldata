ALTER TABLE `dice_pipeline_lifecycle_hook_clients` MODIFY COLUMN `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键';

ALTER TABLE `dice_pipeline_lifecycle_hook_clients` MODIFY COLUMN `name` varchar(128);

ALTER TABLE `dice_pipeline_lifecycle_hook_clients` ADD UNIQUE KEY `uk_name` (`name`);