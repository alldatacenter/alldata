ALTER TABLE `dice_autotest_exec_history` ADD INDEX `idx_step_id` (`step_id`);
ALTER TABLE `dice_autotest_exec_history` ADD INDEX `idx_scene_id` (`scene_id`);
ALTER TABLE `dice_autotest_exec_history` ADD INDEX `idx_project_id_iteration_id_type_execute_time` (`project_id`, `iteration_id`, `type`, `execute_time`);