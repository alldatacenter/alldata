-- MIGRATION_BASE

-- maintainer: sfwn
-- added time: 2021/06/04
-- filename: 4.0/migrations/qa/v001/migration.sql

ALTER TABLE `dice_api_test`
    ADD INDEX `idx_projectid_usercase` (`project_id`, `usecase_id`, `usecase_order`);
