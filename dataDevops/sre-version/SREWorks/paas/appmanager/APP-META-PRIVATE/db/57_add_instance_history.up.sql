CREATE TABLE `am_rt_app_instance_history`
(
    `id`              bigint      NOT NULL AUTO_INCREMENT,
    `gmt_create`      datetime    NOT NULL COMMENT '创建时间',
    `gmt_modified`    datetime    NOT NULL COMMENT '修改时间',
    `app_instance_id` varchar(64) NOT NULL COMMENT '应用实例 ID',
    `status`          varchar(16) NOT NULL COMMENT '状态',
    PRIMARY KEY (`id`),
    INDEX `idx_app_instance_id` (`app_instance_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='实时应用实例表_历史';

CREATE TABLE `am_rt_component_instance_history`
(
    `id`                    bigint      NOT NULL AUTO_INCREMENT,
    `gmt_create`            datetime    NOT NULL COMMENT '创建时间',
    `gmt_modified`          datetime    NOT NULL COMMENT '修改时间',
    `component_instance_id` varchar(64) NOT NULL COMMENT '组件实例 ID',
    `app_instance_id`       varchar(64) NOT NULL COMMENT '当前组件归属的应用实例 ID',
    `status`                varchar(16) NOT NULL COMMENT '状态',
    `conditions`            text        NULL COMMENT '当前状态详情 (JSON Array)',
    PRIMARY KEY (`id`),
    INDEX `idx_component_instance_id` (`component_instance_id`),
    INDEX `idx_app_instance_id` (`app_instance_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='实时组件实例表_历史';

CREATE TABLE `am_rt_trait_instance_history`
(
    `id`                    bigint      NOT NULL AUTO_INCREMENT,
    `gmt_create`            datetime    NOT NULL COMMENT '创建时间',
    `gmt_modified`          datetime    NOT NULL COMMENT '修改时间',
    `trait_instance_id`     varchar(64) NOT NULL COMMENT 'Trait 实例 ID',
    `component_instance_id` varchar(64) NOT NULL COMMENT '当前 Trait 实例归属的组件实例 ID',
    `app_instance_id`       varchar(64) NOT NULL COMMENT '当前 Trait 实例归属的应用实例 ID',
    `status`                varchar(16) NOT NULL COMMENT '状态',
    `conditions`            text        NULL COMMENT '当前状态详情 (JSON Array)',
    PRIMARY KEY (`id`),
    INDEX `idx_trait_instance_id` (`trait_instance_id`),
    INDEX `idx_app_instance_id` (`app_instance_id`),
    INDEX `idx_component_instance_id` (`component_instance_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='实时 Trait 实例表_历史';