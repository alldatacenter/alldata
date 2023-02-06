ALTER TABLE `dice_mboxs` ADD INDEX `idx_org_user_status_dedup` (`org_id`, `user_id`, `status`, `deduplicate_id`);

ALTER TABLE `dice_audit` ADD INDEX `idx_org_start_scopetype` (`org_id`, `start_time`, `scope_type`);

ALTER TABLE `ps_tickets` MODIFY COLUMN `target_id` varchar(191) DEFAULT NULL COMMENT '目标id';
ALTER TABLE `ps_tickets` MODIFY COLUMN `status` varchar(191) DEFAULT NULL COMMENT '工单状态';
ALTER TABLE `ps_tickets` ADD INDEX `idx_target_status_create_update` (`target_type`, `target_id`, `status`, `created_at`, `updated_at`);
