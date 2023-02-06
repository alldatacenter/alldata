# dice_test_cases
ALTER TABLE `dice_test_cases` ADD INDEX `idx_proj_testset_recycle_priority_updater_name` (`project_id`, `test_set_id`, `recycled`, `priority`, `updater_id`, `name`);

# dice_test_plan_case_relations
ALTER TABLE `dice_test_plan_case_relations` ADD INDEX `idx_testcaseid` (`test_case_id`);
ALTER TABLE `dice_test_plan_case_relations` ADD INDEX `idx_plan_set_case_status_updater` (`test_plan_id`, `test_set_id`, `test_case_id`, `exec_status`, `updater_id`);

# dice_test_sets
## related to: modules/dop/dao/testset.go:28
ALTER TABLE `dice_test_sets` MODIFY COLUMN `directory` varchar(5000) NOT NULL DEFAULT '' COMMENT '当前节点+所有父级节点的name集合（参考值：新建测试集1/新建测试集2/测试集名称3），这里冗余是为了方便界面展示。';
ALTER TABLE `dice_test_sets` ADD INDEX `idx_directory` (`directory`(191));

# dice_test_plan_members
ALTER TABLE `dice_test_plan_members` MODIFY COLUMN `user_id` varchar(191) NOT NULL DEFAULT '' COMMENT 'user_id';
ALTER TABLE `dice_test_plan_members` ADD INDEX `idx_plan_role_userid` (`test_plan_id`, `role`, `user_id`);