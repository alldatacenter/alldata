DROP TABLE IF EXISTS `am_addon_component_rel`;
DROP TABLE IF EXISTS `am_addon_instance`;
DROP TABLE IF EXISTS `am_addon_meta`;
DROP TABLE IF EXISTS `am_app_meta`;
DROP TABLE IF EXISTS `am_app_package`;
DROP TABLE IF EXISTS `am_app_package_component_rel`;
DROP TABLE IF EXISTS `am_app_package_tag`;
DROP TABLE IF EXISTS `am_component_package_task`;
DROP TABLE IF EXISTS `am_component_package`;
DROP TABLE IF EXISTS `am_deploy_app`;
DROP TABLE IF EXISTS `am_deploy_component`;
DROP TABLE IF EXISTS `am_env`;
DROP TABLE IF EXISTS `am_micro_service_host`;
DROP TABLE IF EXISTS `am_ms_port_resource`;
DROP TABLE IF EXISTS `am_namespace`;
DROP TABLE IF EXISTS `am_template`;
DROP TABLE IF EXISTS `channel_task`;
DROP TABLE IF EXISTS `channel_task_host`;
DROP TABLE IF EXISTS `tc_dag`;
DROP TABLE IF EXISTS `tc_dag_config`;
DROP TABLE IF EXISTS `tc_dag_inst`;
DROP TABLE IF EXISTS `tc_dag_inst_edge`;
DROP TABLE IF EXISTS `tc_dag_inst_node`;
DROP TABLE IF EXISTS `tc_dag_inst_node_std`;
DROP TABLE IF EXISTS `tc_dag_node`;
DROP TABLE IF EXISTS `tc_dag_options`;
DROP TABLE IF EXISTS `tc_dag_service_node`;


/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_addon_component_rel   */
/******************************************/
CREATE TABLE `am_addon_component_rel`
(
    `id`                bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`        datetime            NOT NULL COMMENT '创建时间',
    `gmt_modified`      datetime            NOT NULL COMMENT '修改时间',
    `namespace_id`      varchar(64)         NOT NULL DEFAULT '' COMMENT 'namespace',
    `env_id`            varchar(64)         NOT NULL DEFAULT '' COMMENT '环境ID',
    `app_id`            varchar(64)         NOT NULL DEFAULT '' COMMENT '应用ID',
    `component_type`    varchar(64)         NOT NULL DEFAULT '' COMMENT '组件类型',
    `component_name`    varchar(64)         NOT NULL DEFAULT '' COMMENT '组件名称',
    `addon_instance_id` varchar(64)                  DEFAULT '' COMMENT 'addon实例ID',
    `var_mapping`       varchar(2048)                DEFAULT NULL COMMENT '环境光变量映射',
    `addon_name`        varchar(64)                  DEFAULT '' COMMENT 'addon资源名称',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 80
  DEFAULT CHARSET = utf8mb4 COMMENT ='addon和组件关联关系'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_addon_instance   */
/******************************************/
CREATE TABLE `am_addon_instance`
(
    `id`                bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`        datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`      datetime    DEFAULT NULL COMMENT '最后修改时间',
    `addon_id`          varchar(32) DEFAULT NULL COMMENT '附加组件唯一标识',
    `namespace_id`      varchar(32) DEFAULT NULL COMMENT '该附加组件部署到的 Namespace 标识',
    `resource_ext`      longtext COMMENT '资源扩展信息',
    `addon_instance_id` varchar(64) DEFAULT '' COMMENT '资源实例ID',
    `addon_version`     varchar(64) DEFAULT '' COMMENT '附加组件唯一标识',
    PRIMARY KEY (`id`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 73
  DEFAULT CHARSET = utf8mb4 COMMENT ='附加组件实例'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_addon_meta   */
/******************************************/
CREATE TABLE `am_addon_meta`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`    datetime     DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`  datetime     DEFAULT NULL COMMENT '最后修改时间',
    `addon_id`      varchar(32)  DEFAULT NULL COMMENT '附加组件唯一标识',
    `addon_version` varchar(32)  DEFAULT NULL COMMENT '版本号',
    `addon_type`    varchar(16)  DEFAULT NULL COMMENT '类型（可选 CORE-SERVICE / THIRDPARTY）',
    `addon_name`    varchar(64)  DEFAULT NULL COMMENT '名称',
    `addon_path`    varchar(255) DEFAULT NULL COMMENT '存储相对路径',
    `addon_ext`     longtext COMMENT '扩展信息 JSON',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_addon_id_version` (`addon_id`, `addon_version`),
    KEY `idx_addon_type` (`addon_type`),
    KEY `idx_addon_name` (`addon_name`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 20
  DEFAULT CHARSET = utf8mb4 COMMENT ='附加组件元信息'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_app_meta   */
/******************************************/
CREATE TABLE `am_app_meta`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`   datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime    DEFAULT NULL COMMENT '最后修改时间',
    `app_id`       varchar(32) DEFAULT NULL COMMENT '应用唯一标识',
    `app_type`     varchar(16) DEFAULT NULL COMMENT '应用类型',
    `app_name`     varchar(64) DEFAULT NULL COMMENT '应用名称',
    `app_ext`      longtext COMMENT '扩展信息 JSON',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_id` (`app_id`),
    KEY `idx_app_type` (`app_type`),
    KEY `idx_app_name` (`app_name`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='应用元信息'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_app_package   */
/******************************************/
CREATE TABLE `am_app_package`
(
    `id`              bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`      datetime     DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`    datetime     DEFAULT NULL COMMENT '最后修改时间',
    `app_id`          varchar(32)  DEFAULT NULL COMMENT '应用唯一标识',
    `package_version` varchar(32)  DEFAULT NULL COMMENT '版本号',
    `package_path`    varchar(255) DEFAULT NULL COMMENT '存储位置相对路径',
    `package_creator` varchar(64)  DEFAULT NULL COMMENT '创建者',
    `package_ext`     longtext COMMENT '扩展信息 JSON',
    `package_md5`     varchar(32)  DEFAULT NULL COMMENT '包 MD5',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_package_id` (`app_id`, `package_version`),
    KEY `idx_package_creator` (`package_creator`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10
  DEFAULT CHARSET = utf8mb4 COMMENT ='应用包详情表'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_app_package_component_rel   */
/******************************************/
CREATE TABLE `am_app_package_component_rel`
(
    `id`                   bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`         datetime DEFAULT NULL COMMENT '最后修改时间',
    `app_package_id`       bigint(20) NOT NULL COMMENT '所属应用包 ID',
    `component_package_id` bigint(20) NOT NULL COMMENT '所属组件包 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_relation` (`app_package_id`, `component_package_id`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 8
  DEFAULT CHARSET = utf8mb4 COMMENT ='应用包中组件引用关系表'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_app_package_tag   */
/******************************************/
CREATE TABLE `am_app_package_tag`
(
    `id`             bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`     datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`   datetime    DEFAULT NULL COMMENT '最后修改时间',
    `app_package_id` bigint(20) NOT NULL COMMENT '所属应用包 ID',
    `tag`            varchar(64) DEFAULT NULL COMMENT '标签',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_package_tag` (`app_package_id`, `tag`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='应用包标签表'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_component_package   */
/******************************************/
CREATE TABLE `am_component_package`
(
    `id`              bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`      datetime     DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`    datetime     DEFAULT NULL COMMENT '最后修改时间',
    `app_id`          varchar(32)  DEFAULT NULL COMMENT '应用唯一标识',
    `component_type`  varchar(32)  DEFAULT NULL COMMENT '组件类型',
    `package_version` varchar(32)  DEFAULT NULL COMMENT '版本号',
    `package_path`    varchar(255) DEFAULT NULL COMMENT '存储位置相对路径',
    `package_creator` varchar(64)  DEFAULT NULL COMMENT '创建者',
    `package_ext`     longtext COMMENT '扩展信息 JSON',
    `component_name`  varchar(32)  DEFAULT NULL COMMENT '组件类型下的唯一组件标识',
    `package_md5`     varchar(32)  DEFAULT NULL COMMENT '包 MD5',
    `package_addon`   text COMMENT '包 Addon 描述信息',
    `package_options` text COMMENT '包配置选项信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_package_id` (`app_id`, `component_type`, `component_name`, `package_version`),
    KEY `idx_package_creator` (`package_creator`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 12
  DEFAULT CHARSET = utf8mb4 COMMENT ='组件包详情表'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_component_package_task   */
/******************************************/
CREATE TABLE `am_component_package_task`
(
    `id`                   bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime     DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`         datetime     DEFAULT NULL COMMENT '最后修改时间',
    `app_id`               varchar(32)  DEFAULT NULL COMMENT '应用唯一标识',
    `component_type`       varchar(32)  DEFAULT NULL COMMENT '组件类型',
    `component_name`       varchar(32)  DEFAULT NULL COMMENT '组件类型下的唯一组件标识',
    `package_version`      varchar(32)  DEFAULT NULL COMMENT '版本号',
    `package_path`         varchar(255) DEFAULT NULL COMMENT '存储位置相对路径',
    `package_creator`      varchar(64)  DEFAULT NULL COMMENT '创建者',
    `package_md5`          varchar(32)  DEFAULT NULL COMMENT '包 MD5',
    `package_addon`        text COMMENT '包 Addon 描述信息',
    `package_options`      text COMMENT '包配置选项信息',
    `package_ext`          longtext COMMENT '扩展信息 JSON',
    `task_status`          varchar(16)  DEFAULT NULL COMMENT '任务状态',
    `task_log`             longtext COMMENT '任务日志',
    `component_package_id` bigint(20)   DEFAULT NULL COMMENT '映射 component package 表主键 ID',
    PRIMARY KEY (`id`),
    KEY `idx_package_task_id` (`app_id`, `component_type`, `component_name`, `package_version`),
    KEY `idx_package_task_status` (`task_status`),
    KEY `idx_package_creator` (`package_creator`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 17
  DEFAULT CHARSET = utf8mb4 COMMENT ='组件包创建任务详情表'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_deploy_app   */
/******************************************/
CREATE TABLE `am_deploy_app`
(
    `id`                     bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`             datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`           datetime    DEFAULT NULL COMMENT '最后修改时间',
    `app_package_id`         bigint(20) NOT NULL COMMENT '当前部署单使用的应用包 ID',
    `app_id`                 varchar(32) DEFAULT NULL COMMENT '应用唯一标识',
    `namespace_id`           varchar(32) DEFAULT NULL COMMENT '部署目标 Namespace ID',
    `env_id`                 varchar(32) DEFAULT NULL COMMENT '部署目标环境 ID',
    `gmt_start`              datetime    DEFAULT NULL COMMENT '部署开始时间',
    `gmt_end`                datetime    DEFAULT NULL COMMENT '部署结束时间',
    `deploy_status`          varchar(16) DEFAULT NULL COMMENT '状态',
    `deploy_error_message`   text COMMENT '错误信息',
    `deploy_creator`         varchar(64) DEFAULT NULL COMMENT '部署工单发起人',
    `deploy_ext`             longtext COMMENT '扩展信息 JSON',
    `deploy_component_count` bigint(20)  DEFAULT NULL COMMENT '子 Component 计数',
    PRIMARY KEY (`id`),
    KEY `idx_app_package_id` (`app_package_id`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_namespace_id` (`namespace_id`),
    KEY `idx_env_id` (`env_id`),
    KEY `idx_deploy_status` (`deploy_status`),
    KEY `idx_deploy_creator` (`deploy_creator`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 77
  DEFAULT CHARSET = utf8mb4 COMMENT ='部署工单 - AppPackage 表'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_deploy_component   */
/******************************************/
CREATE TABLE `am_deploy_component`
(
    `id`                   bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`           datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`         datetime    DEFAULT NULL COMMENT '最后修改时间',
    `deploy_id`            bigint(20) NOT NULL COMMENT '所属部署单 ID',
    `component_package_id` bigint(20) NOT NULL COMMENT '当前部署项对应的组件包 ID',
    `app_id`               varchar(32) DEFAULT NULL COMMENT '应用唯一标识',
    `namespace_id`         varchar(32) DEFAULT NULL COMMENT '部署目标 Namespace ID',
    `env_id`               varchar(32) DEFAULT NULL COMMENT '部署目标环境 ID',
    `gmt_start`            datetime    DEFAULT NULL COMMENT '部署开始时间',
    `gmt_end`              datetime    DEFAULT NULL COMMENT '部署结束时间',
    `deploy_status`        varchar(16) DEFAULT NULL COMMENT '状态',
    `deploy_error_message` longtext COMMENT '错误信息',
    `deploy_creator`       varchar(64) DEFAULT NULL COMMENT '部署工单发起人',
    `deploy_process_id`    varchar(64) DEFAULT NULL COMMENT '部署流程 ID',
    `deploy_ext`           longtext COMMENT '扩展信息 JSON',
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
) ENGINE = InnoDB
  AUTO_INCREMENT = 45
  DEFAULT CHARSET = utf8mb4 COMMENT ='部署工单 - ComponentPackage 表'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_env   */
/******************************************/
CREATE TABLE `am_env`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`   datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime    DEFAULT NULL COMMENT '最后修改时间',
    `namespace_id` varchar(32) DEFAULT NULL COMMENT '所属 Namespace ID',
    `env_id`       varchar(32) DEFAULT NULL COMMENT '环境 ID',
    `env_name`     varchar(64) DEFAULT NULL COMMENT '环境名称',
    `env_creator`  varchar(64) DEFAULT NULL COMMENT '环境创建者',
    `env_modifier` varchar(64) DEFAULT NULL COMMENT '环境最后修改者',
    `env_ext`      longtext COMMENT '环境扩展信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_namespace_id_env_id` (`namespace_id`, `env_id`),
    KEY `idx_env_name` (`env_name`),
    KEY `idx_env_creator` (`env_creator`),
    KEY `idx_env_modifier` (`env_modifier`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8mb4 COMMENT ='环境表'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_micro_service_host   */
/******************************************/
CREATE TABLE `am_micro_service_host`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`   datetime            NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime            NOT NULL COMMENT '修改时间',
    `namespace_id` varchar(64)         NOT NULL DEFAULT '' COMMENT 'namespace',
    `env_id`       varchar(64)         NOT NULL DEFAULT '' COMMENT '环境id',
    `app_id`       varchar(32)         NOT NULL DEFAULT '' COMMENT '应用ID',
    `ip`           varchar(32)         NOT NULL DEFAULT '' COMMENT 'ip',
    `deploy_type`  varchar(32)         NOT NULL DEFAULT '' COMMENT '部署方式，独占，共享',
    `service_id`   varchar(64)         NOT NULL DEFAULT '' COMMENT '微服务id',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 49
  DEFAULT CHARSET = utf8mb4 COMMENT ='微服务关联机器信息'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_ms_port_resource   */
/******************************************/
CREATE TABLE `am_ms_port_resource`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`   datetime            NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime            NOT NULL COMMENT '修改时间',
    `port`         int(10) unsigned DEFAULT NULL COMMENT '端口',
    `namespace_id` varchar(64)      DEFAULT '' COMMENT '环境隔离空间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 3
  DEFAULT CHARSET = utf8mb4 COMMENT ='已使用port资源'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_namespace   */
/******************************************/
CREATE TABLE `am_namespace`
(
    `id`                 bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`         datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`       datetime    DEFAULT NULL COMMENT '最后修改时间',
    `namespace_id`       varchar(32) DEFAULT NULL COMMENT 'Namespace ID',
    `namespace_name`     varchar(64) DEFAULT NULL COMMENT 'Namespace Name',
    `namespace_creator`  varchar(64) DEFAULT NULL COMMENT 'Namespace 创建者',
    `namespace_modifier` varchar(64) DEFAULT NULL COMMENT 'Namespace 最后修改者',
    `namespace_ext`      longtext COMMENT 'Namespace 扩展信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_namespace_id` (`namespace_id`),
    KEY `idx_namespace_name` (`namespace_name`),
    KEY `idx_namespace_creator` (`namespace_creator`),
    KEY `idx_namespace_modifier` (`namespace_modifier`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 13
  DEFAULT CHARSET = utf8mb4 COMMENT ='命名空间'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = am_template   */
/******************************************/
CREATE TABLE `am_template`
(
    `id`               bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`       datetime     DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`     datetime     DEFAULT NULL COMMENT '最后修改时间',
    `template_id`      varchar(32)  DEFAULT NULL COMMENT '模板全局唯一 ID',
    `template_version` varchar(32)  DEFAULT NULL COMMENT '模板版本',
    `template_type`    varchar(16)  DEFAULT NULL COMMENT '模板类型',
    `template_name`    varchar(64)  DEFAULT NULL COMMENT '模板名称',
    `template_path`    varchar(255) DEFAULT NULL COMMENT '存储位置相对路径',
    `template_ext`     longtext COMMENT '模板扩展信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_id_version` (`template_id`, `template_version`),
    KEY `idx_template_type` (`template_type`),
    KEY `idx_template_name` (`template_name`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='应用模板'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = channel_task   */
/******************************************/
CREATE TABLE `channel_task`
(
    `id`             bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`     bigint(20)   DEFAULT NULL COMMENT '任务创建时间',
    `gmt_modified`   bigint(20)   DEFAULT NULL COMMENT '任务更新时间',
    `task_uuid`      varchar(45)         NOT NULL COMMENT '任务标识',
    `command`        text COMMENT '执行命令',
    `working_dir`    varchar(128) DEFAULT NULL COMMENT '工作目录',
    `timeout`        bigint(20)   DEFAULT NULL COMMENT '超时时间',
    `exec_user`      varchar(128) DEFAULT NULL COMMENT '执行用户',
    `task_status`    int(11)      DEFAULT NULL COMMENT '任务状态',
    `task_name`      varchar(128) DEFAULT NULL COMMENT '任务名',
    `user_name`      varchar(128) DEFAULT NULL COMMENT '任务发起者',
    `host_count`     int(11)      DEFAULT NULL COMMENT '机器总数',
    `task_remarks`   longtext COMMENT '任务备注',
    `end_time`       bigint(20)   DEFAULT NULL COMMENT '任务结束时间',
    `command_type`   int(11)      DEFAULT NULL COMMENT '命令类型',
    `start_time`     bigint(20)   DEFAULT NULL COMMENT '任务开始时间',
    `script_path`    varchar(128) DEFAULT NULL COMMENT '上传文件存储路径',
    `script_content` longtext COMMENT '上传文件内容',
    `end_point`      varchar(128) DEFAULT NULL COMMENT '任务回调地址',
    `file_name`      varchar(128) DEFAULT NULL COMMENT '脚本名',
    `file_mode`      int(11)      DEFAULT NULL COMMENT '文件权限',
    `exist_write`    tinyint(4)   DEFAULT NULL COMMENT '文件存在是否写入',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_uuid` (`task_uuid`),
    KEY `idx_user_status` (`user_name`, `task_status`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 281
  DEFAULT CHARSET = utf8mb4 COMMENT ='通道任务表'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = channel_task_host   */
/******************************************/
CREATE TABLE `channel_task_host`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`    bigint(20)   DEFAULT NULL COMMENT 'host任务创建时间',
    `gmt_modified`  bigint(20)   DEFAULT NULL COMMENT 'host任务更新时间',
    `task_uuid`     varchar(128)        NOT NULL COMMENT '任务唯一标识',
    `host_uuid`     varchar(128)        NOT NULL COMMENT 'host唯一标识',
    `host_status`   int(11)      DEFAULT NULL COMMENT 'host状态',
    `host_address`  varchar(128) DEFAULT NULL COMMENT 'host地址',
    `host_type`     int(11)      DEFAULT NULL COMMENT 'host地址类型',
    `error_code`    varchar(128) DEFAULT NULL COMMENT '错误码',
    `error_message` longtext COMMENT '错误信息',
    `exit_code`     bigint(20)   DEFAULT NULL COMMENT '退出码',
    `end_time`      bigint(20)   DEFAULT NULL COMMENT 'host完成时间',
    `host_output`   longtext COMMENT '命令输出内容',
    `start_time`    bigint(20)   DEFAULT NULL COMMENT '任务开始时间',
    `agent_uuid`    varchar(128) DEFAULT NULL COMMENT 'agent执行uuid',
    `stderr`        longtext COMMENT '错误输出',
    PRIMARY KEY (`id`),
    KEY `idx_task_status` (`task_uuid`, `host_status`),
    KEY `idx_host_uuid` (`host_uuid`),
    KEY `idx_agent_uuid` (`agent_uuid`),
    KEY `idx_task_host_status` (`task_uuid`, `host_uuid`, `host_status`) COMMENT 'task&host&status联合索引'
) ENGINE = InnoDB
  AUTO_INCREMENT = 422
  DEFAULT CHARSET = utf8mb4 COMMENT ='通道任务host表'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = tc_dag   */
/******************************************/
CREATE TABLE `tc_dag`
(
    `id`                   bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`           bigint(20)   NOT NULL COMMENT '创建时间',
    `gmt_modified`         bigint(20)   NOT NULL COMMENT '更新时间',
    `app_id`               varchar(128) NOT NULL DEFAULT 'tesla' COMMENT '应用',
    `name`                 varchar(128) NOT NULL COMMENT '名称',
    `alias`                varchar(128)          DEFAULT NULL COMMENT '别名',
    `content`              longtext     NOT NULL COMMENT '图',
    `input_params`         longtext COMMENT '输入参数',
    `has_feedback`         tinyint(4)            DEFAULT '0' COMMENT '是否打开反馈',
    `has_history`          tinyint(4)            DEFAULT '1' COMMENT '是否保存历史',
    `description`          longtext COMMENT '描述',
    `entity`               longtext COMMENT '实体信息',
    `notice`               varchar(128)          DEFAULT NULL COMMENT '通知',
    `creator`              varchar(128)          DEFAULT NULL COMMENT '创建人',
    `modifier`             varchar(128)          DEFAULT NULL COMMENT '最后修改人',
    `last_update_by`       varchar(128) NOT NULL DEFAULT 'WEB' COMMENT '最后更新方',
    `ex_schedule_task_id`  varchar(128)          DEFAULT NULL COMMENT '外部执行的taskid, 如果为空，则不执行',
    `default_show_history` tinyint(4)            DEFAULT '0' COMMENT '默认展示历史',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_has_history` (`app_id`, `name`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_name` (`name`),
    KEY `idx_creator` (`creator`),
    KEY `idx_modifier` (`modifier`),
    KEY `idx_feedback` (`has_feedback`),
    KEY `idx_is_history` (`has_history`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2042
  DEFAULT CHARSET = utf8mb4 COMMENT ='场景定义'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = tc_dag_config   */
/******************************************/
CREATE TABLE `tc_dag_config`
(
    `id`           bigint(20)   NOT NULL COMMENT '主键',
    `gmt_create`   bigint(20)   NOT NULL COMMENT '创建时间',
    `gmt_modified` bigint(20)   NOT NULL COMMENT '最后修改时间',
    `name`         varchar(128) NOT NULL COMMENT '名称',
    `content`      longtext COMMENT '内容',
    `comment`      longtext COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='配置'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = tc_dag_inst   */
/******************************************/
CREATE TABLE `tc_dag_inst`
(
    `id`                           bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                   bigint(20)   NOT NULL COMMENT '创建时间',
    `gmt_modified`                 bigint(20)   NOT NULL COMMENT '修改时间',
    `gmt_access`                   bigint(20)   NOT NULL COMMENT '上次调度的时间',
    `app_id`                       varchar(128) NOT NULL DEFAULT 'tesla' COMMENT '启动的产品方',
    `dag_id`                       bigint(20)   NOT NULL COMMENT 'dag的id，用于关联',
    `tc_dag_detail`                longtext     NOT NULL COMMENT '启动时，将dag的全部信息拷贝过来',
    `status`                       varchar(128) NOT NULL COMMENT '执行过程中的状态',
    `status_detail`                longtext COMMENT '状态对应的详细说明，一般是错误信息',
    `global_params`                longtext COMMENT '全局参数，提供给faas使用，允许用户修改',
    `global_object`                longtext COMMENT '全局对象，提供给faas使用，允许用户修改',
    `global_variable`              longtext COMMENT '全局变量，启动时提供的，不允许用户修改',
    `global_result`                longtext COMMENT '全局结果，key是node_id，value只有result和output，没有data，因为data太大了，使用data需要实时从执行侧获取',
    `lock_id`                      varchar(128)          DEFAULT NULL COMMENT '锁，分布式调度使用',
    `creator`                      varchar(128) NOT NULL COMMENT '启动人，工号',
    `is_sub`                       tinyint(4)   NOT NULL DEFAULT '0' COMMENT '是否归属于另外一个dag',
    `tag`                          varchar(128)          DEFAULT NULL COMMENT '标签，目前使用于entityValue',
    `ex_schedule_task_instance_id` varchar(128)          DEFAULT NULL COMMENT '外部调度的作业id',
    `standalone_ip`                varchar(128)          DEFAULT NULL COMMENT '单独调度指定的机器ip',
    `evaluation_create_ret`        longtext COMMENT '评价服务创建后的反馈',
    `channel`                      varchar(128)          DEFAULT NULL COMMENT '调用渠道',
    `env`                          varchar(128)          DEFAULT NULL COMMENT '调用环境',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_lock_id` (`lock_id`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_is_sub` (`is_sub`),
    KEY `idx_tag` (`tag`),
    KEY `idx_standalone_ip` (`standalone_ip`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10096
  DEFAULT CHARSET = utf8mb4 COMMENT ='xxxx'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = tc_dag_inst_edge   */
/******************************************/
CREATE TABLE `tc_dag_inst_edge`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`   bigint(20)   NOT NULL COMMENT '创建时间',
    `gmt_modified` bigint(20)   NOT NULL COMMENT '最后修改时间',
    `dag_inst_id`  bigint(20)   NOT NULL COMMENT '场景实例',
    `source`       varchar(128) NOT NULL COMMENT '源节点',
    `target`       varchar(128) NOT NULL COMMENT '目前节点',
    `label`        varchar(128)          DEFAULT NULL COMMENT '前端标识',
    `shape`        varchar(128) NOT NULL COMMENT '前端标识',
    `style`        longtext     NOT NULL COMMENT '前端标识',
    `data`         longtext     NOT NULL COMMENT '内容，包含判断条件',
    `is_pass`      int(11)               DEFAULT NULL COMMENT '是否通过',
    `exception`    longtext COMMENT '错误信息',
    `status`       varchar(128) NOT NULL DEFAULT 'INIT' COMMENT '判断状态',
    PRIMARY KEY (`id`),
    KEY `idx_dag_inst_id` (`dag_inst_id`),
    KEY `idx_is_pass` (`is_pass`),
    KEY `idx_source` (`source`),
    KEY `idx_target` (`target`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 540
  DEFAULT CHARSET = utf8mb4 COMMENT ='边实例'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = tc_dag_inst_node   */
/******************************************/
CREATE TABLE `tc_dag_inst_node`
(
    `id`                       bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`               bigint(20)   NOT NULL COMMENT '创建时间',
    `gmt_modified`             bigint(20)   NOT NULL COMMENT '修改时间',
    `gmt_start`                bigint(20)   NOT NULL DEFAULT '999999999999999' COMMENT '开始执行时间',
    `dag_inst_id`              bigint(20)   NOT NULL COMMENT '场景实例id',
    `node_id`                  varchar(128) NOT NULL COMMENT '节点id',
    `status`                   varchar(32)  NOT NULL COMMENT '状态',
    `status_detail`            longtext COMMENT '状态详情，一般为错误详情',
    `task_id`                  varchar(128)          DEFAULT NULL COMMENT '执行id，本地的id或者作业的id',
    `stop_task_id`             varchar(128)          DEFAULT NULL COMMENT '停止的执行id，只有本地的才有',
    `lock_id`                  varchar(128)          DEFAULT NULL COMMENT '锁',
    `sub_dag_inst_id`          bigint(20)            DEFAULT NULL COMMENT '如果node是场景的映射，该场景的实例id',
    `tc_dag_or_node_detail`    longtext     NOT NULL COMMENT '映射场景或者node的详情',
    `tc_dag_content_node_spec` longtext     NOT NULL COMMENT '映射的是node，node在场景的content定义中的信息',
    `retry_times`              bigint(20)            DEFAULT '0' COMMENT '重试次数，只执行了一次，重试的次数是0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lock_id` (`dag_inst_id`, `node_id`),
    KEY `idx_dag_inst_id` (`dag_inst_id`),
    KEY `idx_node_id` (`node_id`),
    KEY `idx_status` (`status`),
    KEY `idx_lock_id` (`lock_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 658
  DEFAULT CHARSET = utf8mb4 COMMENT ='节点实例'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = tc_dag_inst_node_std   */
/******************************************/
CREATE TABLE `tc_dag_inst_node_std`
(
    `id`            bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`    bigint(20)  NOT NULL COMMENT '创建时间',
    `gmt_modified`  bigint(20)  NOT NULL COMMENT '修改时间',
    `status`        varchar(32) NOT NULL COMMENT '状态 RUNNING/SUCCESS/EXCEPTION',
    `stdout`        longtext COMMENT '标准输出',
    `stderr`        longtext COMMENT '错误输出',
    `global_params` longtext COMMENT '全局参数',
    `ip`            varchar(128) DEFAULT NULL COMMENT '执行机器的ip',
    `comment`       longtext COMMENT '执行备注',
    `is_stop`       tinyint(4)   DEFAULT NULL COMMENT '是否为停止操作',
    `stop_id`       bigint(20)   DEFAULT NULL COMMENT '执行停止的目标id',
    PRIMARY KEY (`id`),
    KEY `idx_ip` (`ip`),
    KEY `idx_is_stop` (`is_stop`),
    KEY `idx_status` (`status`),
    KEY `idx_stop_id` (`stop_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 348
  DEFAULT CHARSET = utf8mb4 COMMENT ='执行输出'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = tc_dag_node   */
/******************************************/
CREATE TABLE `tc_dag_node`
(
    `id`                 bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`         bigint(20)   NOT NULL COMMENT '创建时间',
    `gmt_modified`       bigint(20)   NOT NULL COMMENT '最后修改时间',
    `app_id`             varchar(128) NOT NULL COMMENT '产品',
    `name`               varchar(128) NOT NULL COMMENT '名称，产品下唯一',
    `alias`              varchar(128)          DEFAULT NULL COMMENT '别名',
    `description`        longtext COMMENT '说明',
    `is_share`           tinyint(4)   NOT NULL DEFAULT '0' COMMENT '是否共享',
    `input_params`       longtext COMMENT '输入参数',
    `output_params`      longtext COMMENT '输出参数',
    `type`               varchar(128) NOT NULL COMMENT '执行类型 API FAAS TASK',
    `detail`             longtext COMMENT '根据不同的执行类型，有不同的内容',
    `is_show`            tinyint(4)   NOT NULL DEFAULT '1' COMMENT '是否展示',
    `format_type`        varchar(128)          DEFAULT NULL COMMENT '展示类型',
    `format_detail`      longtext COMMENT '展示详情',
    `creator`            varchar(128)          DEFAULT NULL COMMENT '创建人',
    `modifier`           varchar(128)          DEFAULT NULL COMMENT '修改人',
    `is_support_chatops` tinyint(4)            DEFAULT '0' COMMENT '是否支持机器人',
    `chatops_detail`     longtext COMMENT '机器人展示详情',
    `last_update_by`     varchar(128) NOT NULL DEFAULT 'WEB' COMMENT '最后由谁修改',
    `run_timeout`        bigint(20)            DEFAULT '0' COMMENT '执行超时时间',
    `max_retry_times`    bigint(20)            DEFAULT '0' COMMENT '重试次数上限，如果是-1则没有上限，0就是不重试',
    `retry_expression`   longtext COMMENT '重试判断，如果为空，则重试判断不通过',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_id_name` (`app_id`, `name`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_name` (`name`),
    KEY `idx_is_share` (`is_share`),
    KEY `idx_last_update_by` (`last_update_by`),
    KEY `idx_creator` (`creator`),
    KEY `idx_modifier` (`modifier`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 14744
  DEFAULT CHARSET = utf8mb4 COMMENT ='节点'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = tc_dag_options   */
/******************************************/
CREATE TABLE `tc_dag_options`
(
    `id`     bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `locale` varchar(32)  NOT NULL COMMENT '地区标识',
    `name`   varchar(128) NOT NULL COMMENT '名称',
    `value`  longtext COMMENT '内容',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_locale_name` (`locale`, `name`),
    KEY `idx_locale` (`locale`),
    KEY `idx_name` (`name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 33408
  DEFAULT CHARSET = utf8mb4 COMMENT ='国产化'
;

/******************************************/
/*   DatabaseName = teslafaas   */
/*   TableName = tc_dag_service_node   */
/******************************************/
CREATE TABLE `tc_dag_service_node`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`   bigint(20)   NOT NULL COMMENT '创建时间',
    `gmt_modified` bigint(20)   NOT NULL COMMENT '修改时间',
    `ip`           varchar(128) NOT NULL COMMENT '机器ip',
    `enable`       tinyint(4)   NOT NULL DEFAULT '1' COMMENT '是够允许',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ip` (`ip`),
    KEY `idx_enable` (`enable`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 24
  DEFAULT CHARSET = utf8mb4 COMMENT ='节点状态'
;
