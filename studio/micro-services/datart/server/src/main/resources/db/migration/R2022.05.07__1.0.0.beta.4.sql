ALTER TABLE `storyboard`
DROP COLUMN `parent_id`,
    DROP COLUMN `is_folder`,
    DROP COLUMN `index`;

ALTER TABLE `view`
DROP COLUMN `type`;

