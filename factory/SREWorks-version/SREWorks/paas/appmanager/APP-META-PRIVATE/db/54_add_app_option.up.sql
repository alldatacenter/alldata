CREATE TABLE `am_app_option`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`   datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime    DEFAULT NULL COMMENT '最后修改时间',
    `app_id`       varchar(64) DEFAULT NULL COMMENT '应用 ID',
    `key`          varchar(64) DEFAULT NULL COMMENT 'Key',
    `value`        longtext    DEFAULT NULL COMMENT 'Value',
    `value_type`   varchar(16) DEFAULT NULL COMMENT 'Value 类型',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_option` (`app_id`, `key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='应用配置选项表';
