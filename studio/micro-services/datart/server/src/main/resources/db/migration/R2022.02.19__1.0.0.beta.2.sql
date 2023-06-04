SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for source_schemas
-- ----------------------------
DROP TABLE IF EXISTS `source_schemas`;

ALTER TABLE `folder`
DROP COLUMN `sub_type`,
    DROP COLUMN `avatar`;

SET FOREIGN_KEY_CHECKS = 1;