-- MIGRATION_BASE

CREATE TABLE `tb_addon_attachment`
(
    `id`                  bigint(20) NOT NULL AUTO_INCREMENT,
    `app_id`              varchar(45)         DEFAULT NULL COMMENT 'appID',
    `instance_id`         varchar(64)         DEFAULT NULL,
    `create_time`         datetime   NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
    `update_time`         datetime   NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '修改时间',
    `is_deleted`          varchar(1) NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `options`             varchar(1024)       DEFAULT NULL COMMENT '可选字段',
    `org_id`              varchar(64)         DEFAULT NULL COMMENT '组织id',
    `project_id`          varchar(64)         DEFAULT NULL COMMENT '项目id',
    `application_id`      varchar(64)         DEFAULT NULL COMMENT '应用id',
    `routing_instance_id` varchar(64)         DEFAULT NULL COMMENT '路由表addon实例ID',
    `runtime_name`        varchar(64)         DEFAULT '' COMMENT 'runtime名称',
    `inside_addon`        varchar(1)          DEFAULT 'N' COMMENT '是否为内部依赖addon，N:否，Y:是',
    `tenant_instance_id`  varchar(64)         DEFAULT NULL COMMENT 'addon tenant ID',
    PRIMARY KEY (`id`),
    KEY `idx_app_id` (`app_id`, `is_deleted`),
    KEY `idx_application_id` (`application_id`, `is_deleted`),
    KEY `idx_instance_id` (`instance_id`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='addon attach信息';

CREATE TABLE `tb_addon_audit`
(
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `org_id`      varchar(16)  NOT NULL COMMENT '企业ID',
    `project_id`  varchar(16)   DEFAULT NULL COMMENT '项目ID',
    `workspace`   varchar(16)  NOT NULL COMMENT '环境',
    `operator`    varchar(255) NOT NULL COMMENT '操作人',
    `op_name`     varchar(50)  NOT NULL COMMENT '操作类型',
    `addon_name`  varchar(128) NOT NULL COMMENT 'addon名称',
    `ins_id`      varchar(128) NOT NULL COMMENT 'addon实例ID',
    `ins_name`    varchar(128) NOT NULL COMMENT 'addon实例名称',
    `params`      varchar(4096) DEFAULT NULL COMMENT '修改参数',
    `is_deleted`  varchar(1)    DEFAULT 'N' COMMENT '逻辑删除',
    `create_time` datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_projectid_insname` (`project_id`, `ins_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='addon操作审计信息';

CREATE TABLE `tb_addon_extra`
(
    `id`          varchar(64) NOT NULL DEFAULT '' COMMENT '数据库唯一Id',
    `addon_id`    varchar(64) NOT NULL DEFAULT '' COMMENT 'addon id',
    `field`       varchar(64) NOT NULL DEFAULT '' COMMENT '字段名称',
    `value`       text        NOT NULL COMMENT '字段value',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `is_deleted`  varchar(1)  NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `addon_name`  varchar(64)          DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_addon_field` (`addon_id`, `field`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='addon额外信息';


INSERT INTO `tb_addon_extra` (`id`, `addon_id`, `field`, `value`, `create_time`, `update_time`, `is_deleted`,
                              `addon_name`)
VALUES ('5f17832361a54247a3272d7b49a1ae78', '', 'DICE_CORE_CLUSTER', 'terminus-test', '2018-11-08 19:22:51',
        '2018-11-08 20:25:05', 'N', NULL),
       ('5f17832361a54247a3272d7b49a1ae79', '955aa1e091204232b4de927d92681638', 'RESOURCE_URL',
        'http://terminus-dice.oss-cn-hangzhou.aliyuncs.com/addon/sql/config_center.sql\n', '2018-11-08 19:22:51',
        '2018-11-08 20:25:05', 'N', NULL);

CREATE TABLE `tb_addon_instance`
(
    `id`                    varchar(45)  NOT NULL COMMENT 'addon实例ID',
    `name`                  varchar(128) NOT NULL COMMENT 'addon实例名称',
    `addon_id`              varchar(64)  NOT NULL DEFAULT '' COMMENT 'addon编号',
    `plan`                  varchar(128)          DEFAULT NULL COMMENT 'addon规格',
    `version`               varchar(128)          DEFAULT NULL COMMENT 'addon版本',
    `app_id`                varchar(45)           DEFAULT NULL COMMENT 'appID',
    `project_id`            varchar(45)           DEFAULT NULL COMMENT '项目ID',
    `org_id`                varchar(45)           DEFAULT NULL COMMENT '组织ID',
    `share_scope`           varchar(45)           DEFAULT NULL COMMENT '共享范围',
    `env`                   varchar(45)  NOT NULL COMMENT '所属部署环境',
    `options`               varchar(1024)         DEFAULT NULL COMMENT '请求参数中可选字段',
    `status`                varchar(45)  NOT NULL COMMENT 'instance状态',
    `config`                varchar(4096)         DEFAULT NULL COMMENT '需要使用的config',
    `az`                    varchar(45)           DEFAULT NULL COMMENT '所属集群',
    `admin_create`          tinyint(1)   NOT NULL DEFAULT '0' COMMENT '是否后台创建',
    `category`              varchar(32)  NOT NULL COMMENT '分类名称',
    `create_time`           datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
    `update_time`           datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '修改时间',
    `is_deleted`            varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `is_migrate`            varchar(1)            DEFAULT 'N' COMMENT '是否迁移数据',
    `attach_count`          int(11)               DEFAULT '0' COMMENT '被引用数',
    `application_id`        varchar(64)           DEFAULT NULL COMMENT '应用id',
    `is_platform`           tinyint(1)            DEFAULT '0' COMMENT '是否为平台Addon实例',
    `is_default`            int(1)                DEFAULT '0' COMMENT '是否默认addon创建',
    `addon_name`            varchar(64)           DEFAULT NULL,
    `namespace`             varchar(255)          DEFAULT NULL,
    `schedule_name`         varchar(255)          DEFAULT NULL,
    `platform_service_type` int(1)                DEFAULT NULL,
    `label`                 varchar(4096)         DEFAULT NULL COMMENT '需要使用的label',
    `kms_key`               varchar(64)           DEFAULT NULL COMMENT 'kms key id',
    `cpu_request`           double                DEFAULT NULL,
    `cpu_limit`             double                DEFAULT NULL,
    `mem_request`           int(11)               DEFAULT NULL,
    `mem_limit`             int(11)               DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_appid_name` (`app_id`, `name`, `addon_id`, `az`),
    KEY `idx_org_status` (`org_id`, `status`, `share_scope`, `is_deleted`),
    KEY `idx_project_status` (`project_id`, `status`, `share_scope`, `is_deleted`),
    KEY `idx_project_addon` (`project_id`, `status`, `addon_id`, `is_deleted`),
    KEY `idx_appid` (`app_id`, `status`, `env`, `share_scope`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='addon实例信息';

CREATE TABLE `tb_addon_instance_routing`
(
    `id`                    varchar(64)  NOT NULL COMMENT 'addon实例ID',
    `name`                  varchar(128) NOT NULL COMMENT 'addon实例名称',
    `addon_id`              varchar(64)  NOT NULL DEFAULT '' COMMENT 'addon编号',
    `plan`                  varchar(128)          DEFAULT NULL COMMENT 'addon规格',
    `version`               varchar(128)          DEFAULT NULL COMMENT 'addon版本',
    `app_id`                varchar(45)           DEFAULT NULL COMMENT 'appID',
    `application_id`        varchar(64)           DEFAULT NULL COMMENT '应用id',
    `project_id`            varchar(45)           DEFAULT NULL COMMENT '项目ID',
    `org_id`                varchar(45)           DEFAULT NULL COMMENT '组织ID',
    `share_scope`           varchar(45)           DEFAULT NULL COMMENT '共享范围',
    `env`                   varchar(45)  NOT NULL COMMENT '所属部署环境',
    `options`               varchar(1024)         DEFAULT NULL COMMENT '请求参数中可选字段',
    `status`                varchar(45)  NOT NULL COMMENT 'instance状态',
    `az`                    varchar(45)           DEFAULT NULL COMMENT '所属集群',
    `category`              varchar(32)  NOT NULL COMMENT '分类名称',
    `is_migrate`            varchar(1)            DEFAULT 'N' COMMENT '是否迁移数据',
    `attach_count`          int(11)               DEFAULT '0' COMMENT '被引用数',
    `is_platform`           tinyint(1)            DEFAULT '0' COMMENT '是否为平台Addon实例',
    `real_instance`         varchar(64)  NOT NULL COMMENT '真实insId',
    `create_time`           datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
    `update_time`           datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '修改时间',
    `is_deleted`            varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `addon_name`            varchar(64)           DEFAULT NULL,
    `inside_addon`          varchar(1)            DEFAULT NULL,
    `platform_service_type` int(1)                DEFAULT NULL,
    `tag`                   varchar(64)           DEFAULT '' COMMENT '实例标签',
    PRIMARY KEY (`id`),
    KEY `idx_appid_name` (`app_id`, `name`, `addon_id`, `az`),
    KEY `idx_org_status` (`org_id`, `status`, `share_scope`, `is_deleted`),
    KEY `idx_addon_id_and_env` (`addon_id`, `env`, `project_id`, `status`),
    KEY `idx_project_status` (`project_id`, `status`, `share_scope`, `is_deleted`),
    KEY `idx_project_addon` (`project_id`, `status`, `addon_id`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='addon实例信息';

CREATE TABLE `tb_addon_micro_attach`
(
    `id`                  bigint(20)  NOT NULL AUTO_INCREMENT,
    `addon_name`          varchar(64) NOT NULL DEFAULT '' COMMENT 'addon名称，平台内唯一标识',
    `routing_instance_id` varchar(64)          DEFAULT NULL COMMENT '路由表addon实例ID',
    `instance_id`         varchar(64)          DEFAULT NULL,
    `project_id`          varchar(64)          DEFAULT NULL COMMENT '项目id',
    `env`                 varchar(16)          DEFAULT NULL COMMENT '环境',
    `org_id`              varchar(64)          DEFAULT NULL COMMENT '组织id',
    `count`               int(11)     NOT NULL DEFAULT '1' COMMENT '引用数量',
    `create_time`         datetime    NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
    `update_time`         datetime    NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '修改时间',
    `is_deleted`          varchar(1)  NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_addon_name` (`addon_name`, `is_deleted`),
    KEY `idx_routing_instance_id` (`routing_instance_id`, `is_deleted`),
    KEY `idx_project_id` (`project_id`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='microservice addon attach信息';

CREATE TABLE `tb_addon_prebuild`
(
    `id`                  int(11)      NOT NULL AUTO_INCREMENT COMMENT '数据库自增id',
    `application_id`      varchar(32)  NOT NULL COMMENT 'app id',
    `git_branch`          varchar(128) NOT NULL COMMENT 'git分支',
    `env`                 varchar(10)  NOT NULL COMMENT '环境',
    `runtime_id`          varchar(32)           DEFAULT NULL COMMENT 'runtimeId',
    `instance_id`         varchar(64)           DEFAULT NULL COMMENT 'addon实例id',
    `instance_name`       varchar(128)          DEFAULT NULL COMMENT 'addon实例名称',
    `addon_name`          varchar(128)          DEFAULT '' COMMENT 'addon名称',
    `addon_class`         varchar(64)           DEFAULT '' COMMENT '规格信息',
    `options`             varchar(1024)         DEFAULT '' COMMENT '额外信息',
    `config`              varchar(1024)         DEFAULT NULL COMMENT '环境变量信息',
    `build_from`          int(1)       NOT NULL DEFAULT '0' COMMENT '创建来源，0:dice.yml，1:重新分析',
    `delete_status`       int(1)       NOT NULL DEFAULT '0' COMMENT '删除状态，0:未删除，1:diceyml删除，2:重新分析删除',
    `create_time`         datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
    `update_time`         datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '修改时间',
    `is_deleted`          varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `routing_instance_id` varchar(64)           DEFAULT NULL COMMENT '路由表addon实例ID',
    `use_type`            varchar(32)           DEFAULT 'NORMAL' COMMENT 'addon使用类型，NORMAL(添加使用), DEFAULT(默认使用)',
    PRIMARY KEY (`id`),
    KEY `idx_app_branch_env` (`application_id`, `git_branch`, `env`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_runtime_id` (`runtime_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='addon创建流程记录信息';

