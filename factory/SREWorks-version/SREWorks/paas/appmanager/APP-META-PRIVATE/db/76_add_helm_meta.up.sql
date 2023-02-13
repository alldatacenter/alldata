CREATE TABLE IF NOT EXISTS `am_helm_meta`
(
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`      datetime     DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`    datetime     DEFAULT NULL COMMENT '最后修改时间',
    `app_id`          varchar(64)  NOT NULL COMMENT '应用 ID',
    `helm_package_id` varchar(128) NOT NULL COMMENT 'Helm 包标识 ID',
    `name`            varchar(128) DEFAULT NULL COMMENT 'Helm 名称',
    `description`     longtext COMMENT '描述信息',
    `component_type`  varchar(32)  NOT NULL COMMENT '组件类型',
    `package_type`    varchar(32)  NOT NULL COMMENT '包类型',
    `helm_ext`        longtext COMMENT 'Helm 扩展信息',
    `options`         longtext COMMENT '构建 Options 信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_helm_meta` (`app_id`, `helm_package_id`),
    INDEX `idx_name` (`name`),
    INDEX `idx_component_type` (`component_type`),
    INDEX `idx_package_type` (`package_type`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Helm 元信息表';