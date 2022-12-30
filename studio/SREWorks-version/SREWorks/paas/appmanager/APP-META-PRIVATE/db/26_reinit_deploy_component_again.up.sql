drop table if exists am_deploy_app;
drop table if exists am_deploy_app_attr;
drop table if exists am_deploy_component;
drop table if exists am_deploy_component_attr;

CREATE TABLE IF NOT EXISTS `am_deploy_app`
(
    `id`                   bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`         datetime    DEFAULT NULL COMMENT '最后修改时间',
    `app_package_id`       bigint NOT NULL COMMENT '当前部署单使用的应用包 ID',
    `app_id`               varchar(32) DEFAULT NULL COMMENT '应用唯一标识',
    `namespace_id`         varchar(32) DEFAULT NULL COMMENT '部署目标 Namespace ID',
    `gmt_start`            datetime    DEFAULT NULL COMMENT '部署开始时间',
    `gmt_end`              datetime    DEFAULT NULL COMMENT '部署结束时间',
    `deploy_status`        varchar(32) DEFAULT NULL COMMENT '状态',
    `deploy_error_message` mediumtext COMMENT '错误信息',
    `deploy_creator`       varchar(64) DEFAULT NULL COMMENT '部署工单发起人',
    `deploy_process_id`    bigint      DEFAULT NULL COMMENT '部署流程 ID',
    PRIMARY KEY (`id`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_app_package_id` (`app_package_id`),
    KEY `idx_deploy_creator` (`deploy_creator`),
    KEY `idx_deploy_process_id` (`deploy_process_id`),
    KEY `idx_deploy_status` (`deploy_status`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`),
    KEY `idx_namespace_id` (`namespace_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='部署工单 - AppPackage 表';

CREATE TABLE IF NOT EXISTS `am_deploy_app_attr`
(
    `id`            bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`    datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`  datetime    DEFAULT NULL COMMENT '最后修改时间',
    `deploy_app_id` bigint NOT NULL COMMENT '所属部署单 ID',
    `attr_type`     varchar(32) DEFAULT NULL COMMENT '类型',
    `attr_value`    longtext COMMENT '值',
    PRIMARY KEY (`id`),
    KEY `idx_deploy_app_id` (`deploy_app_id`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='部署工单 - Component 属性存储表';

CREATE TABLE IF NOT EXISTS `am_deploy_component`
(
    `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`         datetime    DEFAULT NULL COMMENT '最后修改时间',
    `deploy_id`            bigint       NOT NULL COMMENT '所属部署单 ID',
    `deploy_type`          varchar(16)  NOT NULL COMMENT '部署类型(COMPONENT/TRAIT)',
    `identifier`           varchar(255) NOT NULL COMMENT '当前部署项对应的组件(Trait)标识ID',
    `app_id`               varchar(32) DEFAULT NULL COMMENT '应用唯一标识',
    `namespace_id`         varchar(32) DEFAULT NULL COMMENT '部署目标 Namespace ID',
    `gmt_start`            datetime    DEFAULT NULL COMMENT '部署开始时间',
    `gmt_end`              datetime    DEFAULT NULL COMMENT '部署结束时间',
    `deploy_status`        varchar(16) DEFAULT NULL COMMENT '状态',
    `deploy_error_message` longtext COMMENT '错误信息',
    `deploy_creator`       varchar(64) DEFAULT NULL COMMENT '部署工单发起人',
    `deploy_process_id`    varchar(64) DEFAULT NULL COMMENT '部署流程 ID',
    PRIMARY KEY (`id`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_identifier` (`identifier`),
    KEY `idx_deploy_creator` (`deploy_creator`),
    KEY `idx_deploy_id` (`deploy_id`),
    KEY `idx_deploy_process_id` (`deploy_process_id`),
    KEY `idx_deploy_status` (`deploy_status`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`),
    KEY `idx_namespace_id` (`namespace_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='部署工单 - ComponentPackage 表';

CREATE TABLE IF NOT EXISTS `am_deploy_component_attr`
(
    `id`                  bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`          datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`        datetime    DEFAULT NULL COMMENT '最后修改时间',
    `deploy_component_id` bigint NOT NULL COMMENT '所属部署 Component 单 ID',
    `attr_type`           varchar(32) DEFAULT NULL COMMENT '类型',
    `attr_value`          longtext COMMENT '值',
    PRIMARY KEY (`id`),
    KEY `idx_deploy_component_id` (`deploy_component_id`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='部署工单 - Component 属性存储表';
