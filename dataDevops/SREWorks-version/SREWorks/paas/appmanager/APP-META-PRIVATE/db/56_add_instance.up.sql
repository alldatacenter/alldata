CREATE TABLE `am_rt_app_instance`
(
    `id`              bigint      NOT NULL AUTO_INCREMENT,
    `gmt_create`      datetime    NOT NULL COMMENT '创建时间',
    `gmt_modified`    datetime    NOT NULL COMMENT '修改时间',
    `app_instance_id` varchar(64) NOT NULL COMMENT '应用实例 ID',
    `app_id`          varchar(64) NOT NULL COMMENT '应用 ID',
    `cluster_id`      varchar(32) NULL COMMENT 'Cluster ID',
    `namespace_id`    varchar(32) NULL COMMENT 'Namespace ID',
    `stage_id`        varchar(32) NULL COMMENT 'Stage ID',
    `version`         varchar(32) NOT NULL COMMENT '应用实例版本号 (最近一次应用包部署)',
    `status`          varchar(16) NOT NULL COMMENT '状态',
    `lock_version`    int         NOT NULL DEFAULT 0 COMMENT '锁版本',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_app_instance_id` (`app_instance_id`),
    UNIQUE INDEX `uk_app_instance_location` (`app_id`, `cluster_id`, `namespace_id`, `stage_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='实时应用实例表';

CREATE TABLE `am_rt_component_instance`
(
    `id`                    bigint      NOT NULL AUTO_INCREMENT,
    `gmt_create`            datetime    NOT NULL COMMENT '创建时间',
    `gmt_modified`          datetime    NOT NULL COMMENT '修改时间',
    `component_instance_id` varchar(64) NOT NULL COMMENT '组件实例 ID',
    `app_instance_id`       varchar(64) NOT NULL COMMENT '当前组件归属的应用实例 ID',
    `app_id`                varchar(64) NOT NULL COMMENT '应用 ID',
    `component_type`        varchar(32) NOT NULL COMMENT '组件类型',
    `component_name`        varchar(32) NOT NULL COMMENT '组件类型下的唯一组件标识',
    `cluster_id`            varchar(32) NULL COMMENT 'Cluster ID',
    `namespace_id`          varchar(32) NULL COMMENT 'Namespace ID',
    `stage_id`              varchar(32) NULL COMMENT 'Stage ID',
    `version`               varchar(32) NOT NULL COMMENT '应用实例版本号 (最近一次应用包部署)',
    `status`                varchar(16) NOT NULL COMMENT '状态',
    `conditions`            text        NULL COMMENT '当前状态详情 (Yaml Array)',
    `lock_version`    int         NOT NULL DEFAULT 0 COMMENT '锁版本',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_component_instance_id` (`component_instance_id`),

    UNIQUE INDEX `uk_component_instance_location` (`app_id`, `component_type`, `component_name`, `cluster_id`,
                                                   `namespace_id`, `stage_id`),
    INDEX `idx_app_instance_id` (`app_instance_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='实时组件实例表';

CREATE TABLE `am_rt_trait_instance`
(
    `id`                    bigint      NOT NULL AUTO_INCREMENT,
    `gmt_create`            datetime    NOT NULL COMMENT '创建时间',
    `gmt_modified`          datetime    NOT NULL COMMENT '修改时间',
    `trait_instance_id`     varchar(64) NOT NULL COMMENT 'Trait 实例 ID',
    `component_instance_id` varchar(64) NOT NULL COMMENT '当前 Trait 实例归属的组件实例 ID',
    `app_instance_id`       varchar(64) NOT NULL COMMENT '当前 Trait 实例归属的应用实例 ID',
    `trait_name`            varchar(64) NOT NULL COMMENT 'Trait 唯一名称 (含版本)',
    `status`                varchar(16) NOT NULL COMMENT '状态',
    `conditions`            text        NULL COMMENT '当前状态详情 (Yaml Array)',
    `lock_version`    int         NOT NULL DEFAULT 0 COMMENT '锁版本',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_trait_instance_id` (`trait_instance_id`),
    UNIQUE INDEX `uk_trait_instance_location` (`component_instance_id`, `trait_name`),
    INDEX `idx_app_instance_id` (`app_instance_id`),
    INDEX `idx_component_instance_id` (`component_instance_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='实时 Trait 实例表';