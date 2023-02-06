ALTER TABLE `dice_autotest_scene_step` MODIFY COLUMN `pre_type` varchar(191) NOT NULL COMMENT '排序类型';
ALTER TABLE `dice_autotest_scene_step` ADD INDEX `idx_preid_pretype_sceneid` (`pre_id`, `pre_type`, `scene_id`);
