ALTER TABLE `dice_autotest_exec_history` drop INDEX `idx_plan_id`;
ALTER TABLE `dice_autotest_exec_history` drop INDEX `idx_plan_id_2`;
ALTER TABLE `dice_autotest_exec_history` drop INDEX `idx_iteration_id`;
ALTER TABLE `dice_autotest_exec_history` ADD INDEX `idx_type` (`type`);
ALTER TABLE `dice_autotest_exec_history` ADD INDEX `idx_iteration_id` (`iteration_id`);
ALTER TABLE `dice_autotest_exec_history` ADD INDEX `idx_plan_id` (`plan_id`);
ALTER TABLE `dice_autotest_exec_history` ADD `time_begin` datetime NOT NULL DEFAULT '1000-01-01 00:00:00' COMMENT '执行开始时间';
ALTER TABLE `dice_autotest_exec_history` ADD `time_end` datetime NOT NULL DEFAULT '1000-01-01 00:00:00' COMMENT '执行结束时间';

