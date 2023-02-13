CREATE TABLE `am_component`
(
    `id`                      bigint      NOT NULL AUTO_INCREMENT,
    `component_type`          varchar(32) NOT NULL COMMENT '组件类型，全局唯一',
    `component_adapter_type`  varchar(16) NOT NULL COMMENT '适配类型，可选 core, groovy',
    `component_adapter_value` longtext DEFAULT NULL COMMENT '适配器值，core 时可为空，groovy 时为脚本',
    `current_revision`        int         NOT NULL COMMENT '当前版本',
    `gmt_create`              datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`            datetime DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_component_type` (`component_type`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`),
    INDEX `idx_current_revision` (`current_revision`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='组件定义表';

CREATE TABLE `am_component_history`
(
    `id`                      bigint      NOT NULL AUTO_INCREMENT,
    `component_type`          varchar(32) NOT NULL COMMENT '组件类型，全局唯一',
    `component_adapter_type`  varchar(16) NOT NULL COMMENT '适配类型，可选 core, groovy',
    `component_adapter_value` longtext    DEFAULT NULL COMMENT '适配器值，core 时可为空，groovy 时为脚本',
    `revision`                int         NOT NULL COMMENT '组件版本',
    `modifier`                varchar(64) DEFAULT NULL COMMENT '修改者',
    `gmt_create`              datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`            datetime    DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_component_type` (`component_type`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`),
    INDEX `idx_revision` (`revision`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='组件定义表_历史';