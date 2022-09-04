CREATE TABLE IF NOT EXISTS `am_plugin_definition`
(
    `id`                  bigint               NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`          datetime   DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`        datetime   DEFAULT NULL COMMENT '最后修改时间',
    `plugin_name`         varchar(64)          NOT NULL COMMENT 'Plugin 唯一标识',
    `plugin_version`      varchar(32)          NOT NULL COMMENT 'Plugin 版本 (SemVer)',
    `plugin_registered`   tinyint(1) default 0 not null comment '是否已安装注册',
    `plugin_description`  longtext COMMENT 'Plugin 描述',
    `plugin_dependencies` text COMMENT 'Plugin 依赖 (JSON Array)',
    `plugin_extra`        longtext COMMENT 'Plugin 附加信息 (JSON Object)',
    `package_path`        varchar(255)         NOT NULL COMMENT 'Plugin 包路径',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_name_version` (`plugin_name`, `plugin_version`) USING BTREE,
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Plugin 定义表';

CREATE TABLE IF NOT EXISTS `am_plugin_tag`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`   datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime    DEFAULT NULL COMMENT '最后修改时间',
    `plugin_id`    bigint(20) NOT NULL COMMENT '所属 Plugin ID',
    `tag`          varchar(64) DEFAULT NULL COMMENT '标签',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plugin_tag` (`plugin_id`, `tag`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Plugin 标签表';

CREATE TABLE `am_plugin_frontend`
(
    `id`             bigint(20)  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`     datetime    NOT NULL COMMENT '创建时间',
    `gmt_modified`   datetime    NOT NULL COMMENT '修改时间',
    `plugin_name`    varchar(64) NOT NULL COMMENT 'Plugin 唯一标识',
    `plugin_version` varchar(32) NOT NULL COMMENT 'Plugin 版本 (SemVer)',
    `name`           varchar(64) NOT NULL COMMENT '名称',
    `config`         longtext COMMENT '页面配置',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Plugin 前端配置表';

CREATE TABLE IF NOT EXISTS `am_plugin_resource`
(
    `id`                     bigint(20)  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`             datetime             DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`           datetime             DEFAULT NULL COMMENT '最后修改时间',
    `plugin_name`            bigint(20)  NOT NULL COMMENT 'Plugin 唯一标识',
    `plugin_version`         varchar(32) NOT NULL COMMENT 'Plugin 版本 (SemVer)',
    `cluster_id`             varchar(32) NULL COMMENT 'Plugin 资源部署到的目标 Cluster ID',
    `instance_status`        varchar(16) NOT NULL COMMENT 'Plugin 资源状态 (PENDING/RUNNING/SUCCESS/FAILURE/REMOVED)',
    `instance_error_message` longtext COMMENT 'Plugin 资源部署错误信息',
    `instance_registered`    tinyint(1)           default 0 not null comment '是否已安装注册',
    `lock_version`           int         NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Plugin 资源表';

CREATE TABLE IF NOT EXISTS `am_plugin_resource_workflow_rel`
(
    `id`                     bigint(20)  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`             datetime             DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`           datetime             DEFAULT NULL COMMENT '最后修改时间',
    `plugin_resource_id`     bigint(20)  NOT NULL COMMENT 'Plugin Resource ID',
    `cluster_id`             varchar(32) NULL COMMENT 'Plugin 资源部署到的目标 Cluster ID',
    `workflow_type`          varchar(16) NOT NULL COMMENT 'Plugin 资源对应的 Workflow 类型 (INSTALL / UNINSTALL)',
    `workflow_id`            bigint(20)  NOT NULL COMMENT 'Plugin 资源对应的 Workflow ID',
    `workflow_status`        varchar(16) NOT NULL COMMENT 'Plugin 资源对应的 Workflow 状态',
    `workflow_error_message` longtext COMMENT 'Plugin 资源对应的 Workflow 错误信息',
    `lock_version`           int         NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Plugin 资源关联 Workflow 表';
