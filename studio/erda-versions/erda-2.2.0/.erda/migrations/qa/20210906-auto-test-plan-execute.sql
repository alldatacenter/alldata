ALTER TABLE `dice_autotest_plan` ADD `execute_time` datetime DEFAULT NULL COMMENT 'auto test plan latest execute time';
ALTER TABLE `dice_autotest_plan` ADD `pass_rate` decimal NOT NULL DEFAULT 0 COMMENT 'auto test plan execute pass rate';
