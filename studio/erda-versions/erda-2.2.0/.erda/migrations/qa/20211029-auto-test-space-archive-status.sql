ALTER TABLE `dice_autotest_space` ADD `archive_status` varchar(191) NOT NULL DEFAULT 'Init' COMMENT 'auto test space archive status';

ALTER TABLE `dice_autotest_space` ADD INDEX `idx_project_id` (`project_id`);
