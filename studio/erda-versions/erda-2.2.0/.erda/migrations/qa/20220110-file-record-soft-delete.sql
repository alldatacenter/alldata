ALTER TABLE `dice_test_file_records` ADD `soft_deleted_at` bigint(20) NOT NULL DEFAULT 0 COMMENT '删除时间(时间戳)';
ALTER TABLE `dice_test_file_records` ADD `org_id`          bigint(20) DEFAULT 0 COMMENT '组织ID';
ALTER TABLE `dice_test_file_records` RENAME TO `erda_file_record`;