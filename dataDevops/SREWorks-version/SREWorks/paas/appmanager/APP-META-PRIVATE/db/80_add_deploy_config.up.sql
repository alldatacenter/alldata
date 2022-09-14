CREATE TABLE IF NOT EXISTS `am_deploy_config`
(
    `id`               bigint(20)   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`       datetime   DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`     datetime   DEFAULT NULL COMMENT '最后修改时间',
    `app_id`           varchar(64)  NOT NULL COMMENT '应用 ID',
    `type_id`          varchar(128) NOT NULL COMMENT '类型 ID',
    `env_id`           varchar(128) NOT NULL COMMENT '环境 ID',
    `api_version`      varchar(32)  NOT NULL COMMENT 'API Version',
    `current_revision` int          NOT NULL COMMENT '当前版本',
    `enabled`          tinyint(1) DEFAULT '0' COMMENT '是否开启',
    `config`           longtext COMMENT '配置内容，允许包含 Jinja',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_deploy_config` (`app_id`, `type_id`(64), `env_id`(64), `api_version`),
    INDEX `idx_current_revision` (`current_revision`),
    INDEX `idx_enabled` (`enabled`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='部署配置表';

CREATE TABLE IF NOT EXISTS `am_deploy_config_history`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`   datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime DEFAULT NULL COMMENT '最后修改时间',
    `app_id`       varchar(64)  NOT NULL COMMENT '应用 ID',
    `type_id`      varchar(128) NOT NULL COMMENT '类型 ID',
    `env_id`       varchar(128) NOT NULL COMMENT '环境 ID',
    `api_version`  varchar(32)  NOT NULL COMMENT 'API Version',
    `revision`     int          NOT NULL COMMENT '历史版本号',
    `config`       longtext COMMENT '配置内容，允许包含 Jinja',
    PRIMARY KEY (`id`),
    INDEX `idx_deploy_config_history` (`app_id`, `type_id`(64), `env_id`(64), `api_version`),
    INDEX `idx_revision` (`revision`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='部署配置历史表';