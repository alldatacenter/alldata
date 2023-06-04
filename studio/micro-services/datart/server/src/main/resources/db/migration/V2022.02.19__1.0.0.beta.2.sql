SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for source_schemas
-- ----------------------------
DROP TABLE IF EXISTS `source_schemas`;
CREATE TABLE `source_schemas`  (
                                   `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                                   `source_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                                   `schemas` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
                                   `update_time` datetime(0) NULL DEFAULT NULL,
                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;


ALTER TABLE `folder`
    ADD COLUMN `sub_type` varchar(255) NULL AFTER `rel_type`,
    ADD COLUMN `avatar` varchar(255) NULL AFTER `rel_id`;