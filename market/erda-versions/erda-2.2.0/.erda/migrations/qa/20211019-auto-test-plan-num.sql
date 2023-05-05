ALTER TABLE `dice_autotest_plan` ADD `execute_rate`  decimal(10, 2)   NOT NULL DEFAULT 0 COMMENT '执行率';
ALTER TABLE `dice_autotest_plan` ADD `success_api_num` int(20)        NOT NULL DEFAULT 0 COMMENT '执行成功api数量';
ALTER TABLE `dice_autotest_plan` ADD `total_api_num`   int(20)        NOT NULL DEFAULT 0 COMMENT '总api数量';
ALTER TABLE `dice_autotest_plan` ADD `cost_time_sec`   int(20)        NOT NULL DEFAULT 0 COMMENT '执行耗时';
ALTER TABLE `dice_autotest_plan` MODIFY `pass_rate`  decimal(10, 2)   NOT NULL DEFAULT 0 COMMENT '通过率';