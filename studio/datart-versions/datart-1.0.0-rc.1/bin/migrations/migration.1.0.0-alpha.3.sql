ALTER TABLE `view`
    MODIFY COLUMN `index` double(16, 8) NULL DEFAULT NULL AFTER `is_folder`;

ALTER TABLE `folder`
    MODIFY COLUMN `index` double(16, 8) NULL DEFAULT NULL AFTER `parent_id`;