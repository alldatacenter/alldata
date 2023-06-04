ALTER TABLE `storyboard`
    ADD COLUMN `parent_id` varchar(32) NULL AFTER `org_id`,
    ADD COLUMN `is_folder` tinyint(1) NULL AFTER `parent_id`,
    ADD COLUMN `index` double(16, 8) NULL AFTER `is_folder`;

ALTER TABLE `view`
    ADD COLUMN `type` varchar(32) NULL AFTER `script`;

