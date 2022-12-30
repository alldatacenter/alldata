SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for share
-- ----------------------------
DROP TABLE IF EXISTS `share`;

ALTER TABLE `source`
    DROP  COLUMN `parent_id` ,
    DROP COLUMN `is_folder` ,
    DROP COLUMN `index`;

DROP PROCEDURE IF EXISTS drop_source_prj_index;


ALTER TABLE `variable`
    DROP COLUMN `source_id`,
    DROP COLUMN `format`;
ALTER TABLE `variable`
DROP INDEX `source_id`;

SET FOREIGN_KEY_CHECKS = 1;