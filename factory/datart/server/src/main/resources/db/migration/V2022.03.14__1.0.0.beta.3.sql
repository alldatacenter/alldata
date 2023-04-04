SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for share
-- ----------------------------
DROP TABLE IF EXISTS `share`;
CREATE TABLE `share`  (
                          `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                          `org_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                          `viz_type` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                          `viz_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                          `authentication_mode` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                          `roles` text CHARACTER SET utf8 COLLATE utf8_general_ci,
                          `row_permission_by` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                          `authentication_code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
                          `expiry_date` timestamp NULL DEFAULT NULL,
                          `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                          `create_time` timestamp NULL DEFAULT NULL,
                          PRIMARY KEY (`id`) USING BTREE,
                          KEY `viz_id` (`viz_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

ALTER TABLE `source`
    ADD COLUMN `parent_id` varchar(32) NULL AFTER `org_id`,
    ADD COLUMN `is_folder` tinyint(1) NULL AFTER `parent_id`,
    ADD COLUMN `index` double(16, 8) NULL AFTER `is_folder`;


ALTER TABLE `schedule`
    MODIFY COLUMN `index` double(16, 8) NULL DEFAULT NULL AFTER `is_folder`;

ALTER TABLE `variable`
    ADD COLUMN `source_id` varchar(32) NULL AFTER `view_id`,
    ADD COLUMN `format` varchar(255) NULL AFTER `value_type`;
ALTER TABLE `variable`
    ADD INDEX `source_id`(`source_id`) USING BTREE;

DELETE FROM `source_schemas`
WHERE id IN
      (SELECT temp.id FROM (
                            SELECT id FROM `source_schemas`
                            WHERE source_id IN
                                  (SELECT source_id FROM `source_schemas` GROUP BY source_id HAVING count(*) > 1)
                              AND id NOT IN
                                  (SELECT max(id) FROM `source_schemas` GROUP BY source_id HAVING count(*) > 1)
                        ) temp );

ALTER TABLE `source_schemas`
    ADD UNIQUE INDEX `source_id`(`source_id`) USING BTREE;

SET FOREIGN_KEY_CHECKS = 1;
