ALTER TABLE `am_user_profile` ADD COLUMN `namespace_id` varchar(32) NULL COMMENT '部署目标 Namespace ID';
ALTER TABLE `am_user_profile` ADD COLUMN `stage_id` varchar(32) NULL COMMENT 'Stage ID';