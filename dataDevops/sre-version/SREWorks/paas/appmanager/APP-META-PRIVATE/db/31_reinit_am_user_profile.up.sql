DROP TABLE IF EXISTS `am_user_profile`;
CREATE TABLE `am_user_profile`
(
    `id`           bigint(20)                                                   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`   datetime(0)                                                  NULL DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                  NULL DEFAULT NULL COMMENT '最后修改时间',
    `user_id`       varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
    `profile`      longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci    NOT NULL COMMENT 'profile 信息 JSON',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user_id` (`user_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '用户Profile信息'
  ROW_FORMAT = Dynamic;