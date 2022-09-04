CREATE TABLE `am_addon_meta`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`    datetime     NULL COMMENT '创建时间',
    `gmt_modified`  datetime     NULL COMMENT '最后修改时间',
    `addon_id`      varchar(32)  NULL COMMENT '附加组件唯一标识',
    `addon_version` varchar(32)  NULL COMMENT '版本号',
    `addon_type`    varchar(16)  NULL COMMENT '类型（可选 CORE-SERVICE / THIRDPARTY）',
    `addon_name`    varchar(64)  NULL COMMENT '名称',
    `addon_path`    varchar(255) NULL COMMENT '存储相对路径',
    `addon_ext`     longtext     NULL COMMENT '扩展信息 JSON',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_addon_id_version` (`addon_id`, `addon_version`),
    KEY `idx_addon_type` (`addon_type`),
    KEY `idx_addon_name` (`addon_name`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='附加组件元信息';

CREATE TABLE `am_addon_instance`
(
    `id`                bigint      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`        datetime    NULL COMMENT '创建时间',
    `gmt_modified`      datetime    NULL COMMENT '最后修改时间',
    `addon_id`          varchar(32) NULL COMMENT '附加组件唯一标识',
    `namespace_id`      varchar(32) NULL COMMENT '该附加组件部署到的 Namespace 标识',
    `resource_ext`      longtext    NULL COMMENT '资源扩展信息',
    `addon_instance_id` varchar(64) NULL DEFAULT '' COMMENT '资源实例ID',
    `addon_version`     varchar(64) NULL DEFAULT '' COMMENT '附加组件唯一标识',
    PRIMARY KEY (`id`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='附加组件实例';

CREATE TABLE `am_template`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`       datetime     NULL COMMENT '创建时间',
    `gmt_modified`     datetime     NULL COMMENT '最后修改时间',
    `template_id`      varchar(32)  NULL COMMENT '模板全局唯一 ID',
    `template_version` varchar(32)  NULL COMMENT '模板版本',
    `template_type`    varchar(16)  NULL COMMENT '模板类型',
    `template_name`    varchar(64)  NULL COMMENT '模板名称',
    `template_path`    varchar(255) NULL COMMENT '存储位置相对路径',
    `template_ext`     longtext     NULL COMMENT '模板扩展信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_id_version` (`template_id`, `template_version`),
    KEY `idx_template_type` (`template_type`),
    KEY `idx_template_name` (`template_name`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='应用模板';

CREATE TABLE `am_namespace`
(
    `id`                 bigint      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`         datetime    NULL COMMENT '创建时间',
    `gmt_modified`       datetime    NULL COMMENT '最后修改时间',
    `namespace_id`       varchar(32) NULL COMMENT 'Namespace ID',
    `namespace_name`     varchar(64) NULL COMMENT 'Namespace Name',
    `namespace_creator`  varchar(64) NULL COMMENT 'Namespace 创建者',
    `namespace_modifier` varchar(64) NULL COMMENT 'Namespace 最后修改者',
    `namespace_ext`      longtext    NULL COMMENT 'Namespace 扩展信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_namespace_id` (`namespace_id`),
    KEY `idx_namespace_name` (`namespace_name`),
    KEY `idx_namespace_creator` (`namespace_creator`),
    KEY `idx_namespace_modifier` (`namespace_modifier`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='命名空间';

CREATE TABLE `am_env`
(
    `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`   datetime    NULL COMMENT '创建时间',
    `gmt_modified` datetime    NULL COMMENT '最后修改时间',
    `namespace_id` varchar(32) NULL COMMENT '所属 Namespace ID',
    `env_id`       varchar(32) NULL COMMENT '环境 ID',
    `env_name`     varchar(64) NULL COMMENT '环境名称',
    `env_creator`  varchar(64) NULL COMMENT '环境创建者',
    `env_modifier` varchar(64) NULL COMMENT '环境最后修改者',
    `env_ext`      longtext    NULL COMMENT '环境扩展信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_namespace_id_env_id` (`namespace_id`, `env_id`),
    KEY `idx_env_name` (`env_name`),
    KEY `idx_env_creator` (`env_creator`),
    KEY `idx_env_modifier` (`env_modifier`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='环境表';

CREATE TABLE `am_app_meta`
(
    `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`   datetime    NULL COMMENT '创建时间',
    `gmt_modified` datetime    NULL COMMENT '最后修改时间',
    `app_id`       varchar(32) NULL COMMENT '应用唯一标识',
    `app_type`     varchar(16) NULL COMMENT '应用类型',
    `app_name`     varchar(64) NULL COMMENT '应用名称',
    `app_ext`      longtext    NULL COMMENT '扩展信息 JSON',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_id` (`app_id`),
    KEY `idx_app_type` (`app_type`),
    KEY `idx_app_name` (`app_name`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='应用元信息';

CREATE TABLE `am_component_package`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`      datetime     NULL COMMENT '创建时间',
    `gmt_modified`    datetime     NULL COMMENT '最后修改时间',
    `app_id`          varchar(32)  NULL COMMENT '应用唯一标识',
    `component_type`  varchar(32)  NULL COMMENT '组件类型',
    `component_name`  varchar(32)  NULL COMMENT '组件类型下的唯一组件标识',
    `package_version` varchar(32)  NULL COMMENT '版本号',
    `package_path`    varchar(255) NULL COMMENT '存储位置相对路径',
    `package_creator` varchar(64)  NULL COMMENT '创建者',
    `package_md5`     varchar(32)  NULL COMMENT '包 MD5',
    `package_addon`   text         NULL COMMENT '包 Addon 描述信息',
    `package_options` text         NULL COMMENT '包配置选项信息',
    `package_ext`     longtext     NULL COMMENT '扩展信息 JSON',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_package_id` (`app_id`, `component_type`, `component_name`, `package_version`),
    KEY `idx_package_creator` (`package_creator`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='组件包详情表';

CREATE TABLE `am_app_package`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`      datetime     NULL COMMENT '创建时间',
    `gmt_modified`    datetime     NULL COMMENT '最后修改时间',
    `app_id`          varchar(32)  NULL COMMENT '应用唯一标识',
    `package_version` varchar(32)  NULL COMMENT '版本号',
    `package_path`    varchar(255) NULL COMMENT '存储位置相对路径',
    `package_creator` varchar(64)  NULL COMMENT '创建者',
    `package_md5`     varchar(32)  NULL COMMENT '包 MD5',
    `package_ext`     longtext     NULL COMMENT '扩展信息 JSON',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_package_id` (`app_id`, `package_version`),
    KEY `idx_package_creator` (`package_creator`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='应用包详情表';

CREATE TABLE `am_app_package_component_rel`
(
    `id`                   bigint   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime NULL COMMENT '创建时间',
    `gmt_modified`         datetime NULL COMMENT '最后修改时间',
    `app_package_id`       bigint   NOT NULL COMMENT '所属应用包 ID',
    `component_package_id` bigint   NOT NULL COMMENT '所属组件包 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_relation` (`app_package_id`, `component_package_id`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='应用包中组件引用关系表';

CREATE TABLE `am_app_package_tag`
(
    `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`     datetime    NULL COMMENT '创建时间',
    `gmt_modified`   datetime    NULL COMMENT '最后修改时间',
    `app_package_id` bigint      NOT NULL COMMENT '所属应用包 ID',
    `tag`            varchar(64) NULL COMMENT '标签',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_package_tag` (`app_package_id`, `tag`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='应用包标签表';

CREATE TABLE `am_deploy_app`
(
    `id`                     bigint      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`             datetime    NULL COMMENT '创建时间',
    `gmt_modified`           datetime    NULL COMMENT '最后修改时间',
    `app_package_id`         bigint      NOT NULL COMMENT '当前部署单使用的应用包 ID',
    `app_id`                 varchar(32) NULL COMMENT '应用唯一标识',
    `namespace_id`           varchar(32) NULL COMMENT '部署目标 Namespace ID',
    `env_id`                 varchar(32) NULL COMMENT '部署目标环境 ID',
    `gmt_start`              datetime    NULL COMMENT '部署开始时间',
    `gmt_end`                datetime    NULL COMMENT '部署结束时间',
    `deploy_status`          varchar(16) NULL COMMENT '状态',
    `deploy_error_message`   text        NULL COMMENT '错误信息',
    `deploy_creator`         varchar(64) NULL COMMENT '部署工单发起人',
    `deploy_component_count` bigint      NULL COMMENT '子 Component 计数',
    `deploy_ext`             longtext    NULL COMMENT '扩展信息 JSON',
    PRIMARY KEY (`id`),
    KEY `idx_app_package_id` (`app_package_id`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_namespace_id` (`namespace_id`),
    KEY `idx_env_id` (`env_id`),
    KEY `idx_deploy_status` (`deploy_status`),
    KEY `idx_deploy_creator` (`deploy_creator`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='部署工单 - AppPackage 表';

CREATE TABLE `am_deploy_component`
(
    `id`                   bigint      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime    NULL COMMENT '创建时间',
    `gmt_modified`         datetime    NULL COMMENT '最后修改时间',
    `deploy_id`            bigint      NOT NULL COMMENT '所属部署单 ID',
    `component_package_id` bigint      NOT NULL COMMENT '当前部署项对应的组件包 ID',
    `app_id`               varchar(32) NULL COMMENT '应用唯一标识',
    `namespace_id`         varchar(32) NULL COMMENT '部署目标 Namespace ID',
    `env_id`               varchar(32) NULL COMMENT '部署目标环境 ID',
    `gmt_start`            datetime    NULL COMMENT '部署开始时间',
    `gmt_end`              datetime    NULL COMMENT '部署结束时间',
    `deploy_status`        varchar(16) NULL COMMENT '状态',
    `deploy_error_message` text        NULL COMMENT '错误信息',
    `deploy_creator`       varchar(64) NULL COMMENT '部署工单发起人',
    `deploy_process_id`    varchar(64) NULL COMMENT '部署流程 ID',
    `deploy_ext`           longtext    NULL COMMENT '扩展信息 JSON',
    PRIMARY KEY (`id`),
    KEY `idx_deploy_id` (`deploy_id`),
    KEY `idx_component_package_id` (`component_package_id`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_namespace_id` (`namespace_id`),
    KEY `idx_env_id` (`env_id`),
    KEY `idx_deploy_process_id` (`deploy_process_id`),
    KEY `idx_deploy_status` (`deploy_status`),
    KEY `idx_deploy_creator` (`deploy_creator`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='部署工单 - ComponentPackage 表';

CREATE TABLE `am_component_package_task`
(
    `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime     NULL COMMENT '创建时间',
    `gmt_modified`         datetime     NULL COMMENT '最后修改时间',
    `app_id`               varchar(32)  NULL COMMENT '应用唯一标识',
    `component_type`       varchar(32)  NULL COMMENT '组件类型',
    `component_name`       varchar(32)  NULL COMMENT '组件类型下的唯一组件标识',
    `package_version`      varchar(32)  NULL COMMENT '版本号',
    `package_path`         varchar(255) NULL COMMENT '存储位置相对路径',
    `package_creator`      varchar(64)  NULL COMMENT '创建者',
    `package_md5`          varchar(32)  NULL COMMENT '包 MD5',
    `package_addon`        text         NULL COMMENT '包 Addon 描述信息',
    `package_options`      text         NULL COMMENT '包配置选项信息',
    `package_ext`          longtext     NULL COMMENT '扩展信息 JSON',
    `task_status`          varchar(16)  NULL COMMENT '任务状态',
    `task_log`             longtext     NULL COMMENT '任务日志',
    `component_package_id` bigint       NULL COMMENT '映射 component package 表主键 ID',
    PRIMARY KEY (`id`),
    KEY `idx_package_task_id` (`app_id`, `component_type`, `component_name`, `package_version`),
    KEY `idx_package_task_status` (`task_status`),
    KEY `idx_package_creator` (`package_creator`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='组件包创建任务详情表';
