-- MIGRATION_BASE

CREATE TABLE `dice_db_migration_log`
(
    `id`                    bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `project_id`            bigint(20) NOT NULL COMMENT '项目id',
    `application_id`        bigint(20) NOT NULL COMMENT '应用id',
    `runtime_id`            bigint(20) NOT NULL COMMENT 'runtime id',
    `deployment_id`         bigint(20) NOT NULL COMMENT 'deployment id',
    `release_id`            varchar(64)  NOT NULL DEFAULT '' COMMENT 'release id',
    `operator_id`           bigint(20) NOT NULL COMMENT '执行人',
    `status`                varchar(128) NOT NULL COMMENT '执行结果状态',
    `addon_instance_id`     varchar(64)  NOT NULL COMMENT '所要执行migration的addon实例Id',
    `addon_instance_config` varchar(4096)         DEFAULT NULL COMMENT '需要使用的config',
    `created_at`            timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='migration执行日志记录表';

CREATE TABLE `ps_runtime_instances`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `instance_id` varchar(255) NOT NULL DEFAULT '' COMMENT '实例Id',
    `runtime_id`  bigint(20) NOT NULL COMMENT 'runtime ID',
    `service_id`  bigint(20) NOT NULL COMMENT '服务 ID',
    `ip`          varchar(50)           DEFAULT NULL COMMENT '实例IP',
    `status`      varchar(50)           DEFAULT NULL COMMENT '实例状态',
    `stage`       varchar(100)          DEFAULT NULL COMMENT '容器阶段',
    `created_at`  datetime     NOT NULL COMMENT '创建时间',
    `updated_at`  datetime              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY           `idx_service_id` (`service_id`),
    KEY           `idx_instance_id` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='runtime对应instance信息';

CREATE TABLE `ps_runtime_services`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `runtime_id`   bigint(20) NOT NULL COMMENT 'runtime ID',
    `service_name` varchar(100) NOT NULL COMMENT '服务名称',
    `cpu`          varchar(10)  DEFAULT NULL COMMENT 'CPU',
    `mem`          bigint(10) DEFAULT NULL COMMENT '内存',
    `environment`  text COMMENT '环境变量',
    `ports`        varchar(256) DEFAULT NULL COMMENT '端口',
    `replica`      int(20) DEFAULT NULL COMMENT '副本数',
    `status`       varchar(50)  DEFAULT NULL COMMENT 'Service状态',
    `errors`       text COMMENT '错误列表',
    `created_at`   datetime     NOT NULL COMMENT '创建时间',
    `updated_at`   datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_runtime_id_service_name` (`runtime_id`,`service_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='runtime对应service信息';

CREATE TABLE `ps_v2_deployments`
(
    `id`                  bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `created_at`          timestamp NULL DEFAULT NULL,
    `updated_at`          timestamp NULL DEFAULT NULL,
    `runtime_id`          bigint(20) unsigned NOT NULL,
    `release_id`          varchar(255) DEFAULT NULL,
    `outdated`            tinyint(1) DEFAULT NULL,
    `dice`                text,
    `built_docker_images` text,
    `operator`            varchar(255) NOT NULL,
    `status`              varchar(255) NOT NULL,
    `step`                varchar(255) DEFAULT NULL,
    `fail_cause`          text,
    `extra`               text,
    `finished_at`         timestamp NULL DEFAULT NULL,
    `build_id`            bigint(20) unsigned DEFAULT NULL,
    `dice_type`           int(1) DEFAULT '0' COMMENT 'dice字段类型，0: legace, 1: diceyml',
    `type`                varchar(32)  DEFAULT '' COMMENT 'build类型，REDEPLOY、RELEASE、BUILD',
    `need_approval`       tinyint(1) DEFAULT NULL,
    `approved_by_user`    varchar(255) DEFAULT NULL,
    `approved_at`         timestamp NULL DEFAULT NULL,
    `approval_status`     varchar(255) DEFAULT NULL,
    `approval_reason`     varchar(255) DEFAULT NULL,
    `skip_push_by_orch`   tinyint(1) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY                   `idx_runtime_id` (`runtime_id`),
    KEY                   `idx_status` (`status`),
    KEY                   `idx_operator` (`operator`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT DYNAMIC COMMENT='部署单';

CREATE TABLE `ps_v2_domains`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `created_at`    timestamp NULL DEFAULT NULL,
    `updated_at`    timestamp NULL DEFAULT NULL,
    `runtime_id`    bigint(20) unsigned NOT NULL,
    `domain`        varchar(255) DEFAULT NULL,
    `domain_type`   varchar(255) DEFAULT NULL,
    `endpoint_name` varchar(255) DEFAULT NULL,
    `use_https`     tinyint(1) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_domain_key` (`domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT DYNAMIC COMMENT='Dice 域名表';

CREATE TABLE `ps_v2_pre_builds`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `created_at`   datetime     NOT NULL COMMENT '创建时间',
    `updated_at`   datetime              DEFAULT NULL COMMENT '更新时间',
    `project_id`   bigint(20) NOT NULL COMMENT '应用 ID',
    `git_branch`   varchar(128) NOT NULL DEFAULT '' COMMENT '代码分支',
    `env`          varchar(10)  NOT NULL DEFAULT '' COMMENT '环境：DEV、TEST、STAGING、PROD',
    `git_commit`   varchar(128)          DEFAULT NULL COMMENT '代码 commit 号',
    `dice`         text COMMENT 'dice json 缓存',
    `dice_overlay` text COMMENT '覆盖层',
    `dice_type`    int(1) DEFAULT '0' COMMENT 'dice字段类型，0: legace, 1: diceyml',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_unique_project_env_branch` (`project_id`,`env`,`git_branch`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ps_v2_pre_builds(废弃)';

CREATE TABLE `ps_v2_project_runtimes`
(
    `id`                  bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `created_at`          timestamp NULL DEFAULT NULL,
    `updated_at`          timestamp NULL DEFAULT NULL,
    `name`                varchar(255) NOT NULL,
    `application_id`      bigint(20) unsigned NOT NULL,
    `workspace`           varchar(255) NOT NULL,
    `git_branch`          varchar(255) DEFAULT NULL,
    `project_id`          bigint(20) unsigned NOT NULL,
    `env`                 varchar(255) DEFAULT NULL,
    `cluster_name`        varchar(255) DEFAULT NULL,
    `cluster_id`          bigint(20) unsigned DEFAULT NULL,
    `creator`             varchar(255) NOT NULL,
    `schedule_name`       varchar(255) DEFAULT NULL,
    `runtime_status`      varchar(255) DEFAULT NULL,
    `status`              varchar(255) DEFAULT NULL,
    `deployed`            tinyint(1) DEFAULT NULL,
    `version`             varchar(255) DEFAULT NULL,
    `source`              varchar(255) DEFAULT NULL,
    `dice_version`        varchar(255) DEFAULT NULL,
    `config_updated_date` timestamp NULL DEFAULT NULL,
    `readable_unique_id`  varchar(255) DEFAULT NULL,
    `git_repo_abbrev`     varchar(255) DEFAULT NULL,
    `cpu`                 double(8, 2
) NOT NULL COMMENT 'cpu核数',
  `mem` double(8,2) NOT NULL COMMENT '内存大小（M）',
  `org_id` bigint(20) NOT NULL COMMENT '企业ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_unique_app_id_name` (`name`,`application_id`,`workspace`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='runtime信息';

CREATE TABLE `tb_addon_instance_tenant`
(
    `id`                        varchar(45)  NOT NULL,
    `name`                      varchar(128) NOT NULL COMMENT 'addon租户名称',
    `addon_instance_id`         varchar(64)  NOT NULL DEFAULT '' COMMENT '对应addon实例id',
    `addon_instance_routing_id` varchar(64)  NOT NULL DEFAULT '' COMMENT '对应addonrouting id',
    `app_id`                    varchar(45)           DEFAULT NULL COMMENT 'appID',
    `project_id`                varchar(45)           DEFAULT NULL COMMENT '项目ID',
    `org_id`                    varchar(45)           DEFAULT NULL COMMENT 'orgID',
    `workspace`                 varchar(45)  NOT NULL COMMENT '所属部署环境',
    `config`                    varchar(4096)         DEFAULT NULL COMMENT '需要使用的config',
    `create_time`               datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
    `update_time`               datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '修改时间',
    `is_deleted`                varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `kms_key`                   varchar(64)           DEFAULT NULL COMMENT 'kms key id',
    `reference`                 int(11) DEFAULT '0' COMMENT '被引用数',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='addon租户';

CREATE TABLE `tb_middle_instance_extra`
(
    `id`          varchar(64) NOT NULL DEFAULT '' COMMENT '数据库唯一id',
    `instance_id` varchar(64) NOT NULL DEFAULT '' COMMENT '实例id',
    `field`       varchar(32) NOT NULL DEFAULT '' COMMENT '域',
    `value`       varchar(1024)        DEFAULT NULL,
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  varchar(1)  NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='addon实例额外信息';

CREATE TABLE `tb_middle_instance_relation`
(
    `id`                  varchar(64) NOT NULL DEFAULT '' COMMENT '数据库唯一id',
    `outside_instance_id` varchar(64) NOT NULL COMMENT '外部实例id',
    `inside_instance_id`  varchar(64) NOT NULL COMMENT '内部实例id',
    `create_time`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`          varchar(1)  NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='addon实例依赖关系';

CREATE TABLE `tb_middle_node`
(
    `id`          varchar(64)  NOT NULL DEFAULT '' COMMENT '数据库唯一id',
    `instance_id` varchar(64)  NOT NULL DEFAULT '' COMMENT '实例id',
    `namespace`   varchar(128) NOT NULL COMMENT '容器逻辑隔离空间',
    `node_name`   varchar(128) NOT NULL COMMENT '节点名称',
    `cpu`         double(8, 2
) NOT NULL COMMENT 'cpu核数',
  `mem` int(11) NOT NULL COMMENT '内存大小（M）',
  `disk_size` int(11) DEFAULT NULL COMMENT '磁盘大小（M）',
  `disk_type` varchar(32) DEFAULT '' COMMENT '磁盘类型',
  `count` int(11) DEFAULT '1' COMMENT '节点数',
  `vip` varchar(256) DEFAULT '' COMMENT '节点vip地址',
  `ports` varchar(256) DEFAULT '' COMMENT '节点服务端口',
  `create_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '修改时间',
  `is_deleted` varchar(1) NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
  `proxy` varchar(256) DEFAULT '' COMMENT '节点proxy地址',
  `proxy_ports` varchar(256) DEFAULT '' COMMENT '节点proxy端口',
  `node_role` varchar(32) DEFAULT NULL COMMENT 'node节点角色',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='addon 节点信息';

