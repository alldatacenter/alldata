alter table am_component
    modify component_adapter_value text null comment '适配器值，core 时可为空，groovy 时为脚本';
alter table am_component_history
    modify component_adapter_value text null comment '适配器值，core 时可为空，groovy 时为脚本';

CREATE TABLE `am_dynamic_script`
(
    `id`               bigint      NOT NULL AUTO_INCREMENT,
    `name`             varchar(32) NOT NULL COMMENT '标识名称',
    `code`             longtext COMMENT '代码',
    `current_revision` int         NOT NULL COMMENT '当前版本',
    `gmt_create`       datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`     datetime DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_name` (`name`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`),
    INDEX `idx_current_revision` (`current_revision`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='动态脚本表';

CREATE TABLE `am_dynamic_script_history`
(
    `id`           bigint      NOT NULL AUTO_INCREMENT,
    `name`         varchar(32) NOT NULL COMMENT '标识名称',
    `code`         longtext COMMENT '代码',
    `revision`     int         NOT NULL COMMENT '代码版本',
    `gmt_create`   datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_name` (`name`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`),
    INDEX `idx_revision` (`revision`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='动态脚本表_历史';
