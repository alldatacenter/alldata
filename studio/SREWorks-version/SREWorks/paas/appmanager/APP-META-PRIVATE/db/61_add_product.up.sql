CREATE TABLE IF NOT EXISTS `am_product`
(
    `id`                   bigint(20)  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime     DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`         datetime     DEFAULT NULL COMMENT '最后修改时间',
    `product_id`           varchar(32) NOT NULL COMMENT '产品 ID',
    `product_name`         varchar(64) NOT NULL COMMENT '产品名称',
    `baseline_git_address` varchar(255) DEFAULT NULL COMMENT '基线 Git 地址',
    `baseline_git_user`    varchar(64)  DEFAULT NULL COMMENT '基线 Git User',
    `baseline_git_token`   varchar(64)  DEFAULT NULL COMMENT '基线 Git Token',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_id` (`product_id`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`),
    INDEX `idx_product_name` (`product_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品表';

CREATE TABLE IF NOT EXISTS `am_release`
(
    `id`           bigint(20)  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`   datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime DEFAULT NULL COMMENT '最后修改时间',
    `release_id`   varchar(32) NOT NULL COMMENT '发布版本 ID',
    `release_name` varchar(64) NOT NULL COMMENT '发布版本名称',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_release_id` (`release_id`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`),
    INDEX `idx_release_name` (`release_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='版本发布表';

CREATE TABLE IF NOT EXISTS `am_product_release_rel`
(
    `id`              bigint(20)  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`      datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`    datetime DEFAULT NULL COMMENT '最后修改时间',
    `product_id`      varchar(32) NOT NULL COMMENT '产品 ID',
    `release_id`      varchar(32) NOT NULL COMMENT '发布版本 ID',
    `app_package_tag` varchar(64) NOT NULL COMMENT '应用包默认 Tag 值',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_release_id` (`product_id`, `release_id`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品版本引用表';

CREATE TABLE IF NOT EXISTS `am_product_release_scheduler`
(
    `id`              bigint(20)  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`      datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`    datetime    DEFAULT NULL COMMENT '最后修改时间',
    `product_id`      varchar(32) NOT NULL COMMENT '产品 ID',
    `release_id`      varchar(32) NOT NULL COMMENT '发布版本 ID',
    `scheduler_type`  varchar(16) NOT NULL COMMENT '调度类型 (可选 CRON / MANUAL)',
    `scheduler_value` varchar(32) DEFAULT NULL COMMENT '调度配置 (仅 CRON 时内容为 CRON 表达式, MANUAL 时为空)',
    `enabled`         tinyint(1)  DEFAULT '0' COMMENT '是否开启',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_release_id` (`product_id`, `release_id`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品版本计划调度表';

CREATE TABLE IF NOT EXISTS `am_product_release_task`
(
    `id`              bigint(20)  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`      datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`    datetime    DEFAULT NULL COMMENT '最后修改时间',
    `product_id`      varchar(32) NOT NULL COMMENT '产品 ID',
    `release_id`      varchar(32) NOT NULL COMMENT '发布版本 ID',
    `task_id`         varchar(32) NOT NULL COMMENT '任务 ID',
    `scheduler_type`  varchar(16) NOT NULL COMMENT '调度类型 (可选 CRON / MANUAL)',
    `scheduler_value` varchar(32) DEFAULT NULL COMMENT '调度配置 (仅 CRON 时内容为 CRON 表达式, MANUAL 时为空)',
    PRIMARY KEY (`id`),
    INDEX `idx_product_release_id` (`product_id`, `release_id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品版本任务实例表';

CREATE TABLE IF NOT EXISTS `am_product_release_task_tag`
(
    `id`           bigint(20)  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`   datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime DEFAULT NULL COMMENT '最后修改时间',
    `task_id`      varchar(32) NOT NULL COMMENT '任务 ID',
    `tag`          varchar(64) NULL COMMENT '标签',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_tag` (`tag`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品版本任务实例表标签';

CREATE TABLE IF NOT EXISTS `am_product_release_task_app_package_task_rel`
(
    `id`                  bigint(20)  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`          datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`        datetime DEFAULT NULL COMMENT '最后修改时间',
    `task_id`             varchar(32) NOT NULL COMMENT '任务 ID',
    `app_package_task_id` bigint(20)  NOT NULL COMMENT '应用包任务 ID',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_app_package_task_id` (`app_package_task_id`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品版本任务实例关联应用包任务表';

CREATE TABLE IF NOT EXISTS `am_product_release_app_rel`
(
    `id`                   bigint(20)  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime     DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`         datetime     DEFAULT NULL COMMENT '最后修改时间',
    `product_id`           varchar(32) NOT NULL COMMENT '产品 ID',
    `release_id`           varchar(32) NOT NULL COMMENT '发布版本 ID',
    `app_id`               varchar(64) NOT NULL COMMENT '应用 ID',
    `tag`                  varchar(64)  DEFAULT NULL COMMENT '覆盖默认 tag (用于定制场景)',
    `baseline_git_branch`  varchar(64)  DEFAULT NULL COMMENT '基线 Git 分支',
    `baseline_build_path`  varchar(255) DEFAULT NULL COMMENT 'build.yaml 相对路径',
    `baseline_launch_path` varchar(255) DEFAULT NULL COMMENT 'launch.yaml 相对路径',
    PRIMARY KEY (`id`),
    INDEX `idx_product_release_id` (`product_id`, `release_id`),
    INDEX `idx_app_id` (`app_id`),
    INDEX `idx_tag` (`tag`),
    INDEX `idx_gmt_create` (`gmt_create`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品版本映射应用表';