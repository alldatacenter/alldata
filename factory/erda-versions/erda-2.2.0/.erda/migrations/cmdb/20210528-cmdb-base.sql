-- MIGRATION_BASE

CREATE TABLE `cm_containers`
(
    `id`                    bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`            timestamp NULL DEFAULT NULL,
    `updated_at`            timestamp NULL DEFAULT NULL,
    `container_id`          varchar(64)  DEFAULT NULL,
    `deleted`               tinyint(1) DEFAULT NULL,
    `started_at`            varchar(255) DEFAULT NULL,
    `finished_at`           varchar(255) DEFAULT NULL,
    `exit_code`             int(11) DEFAULT NULL,
    `privileged`            tinyint(1) DEFAULT NULL,
    `cluster`               varchar(255) DEFAULT NULL,
    `host_private_ip_addr`  varchar(255) DEFAULT NULL,
    `ip_address`            varchar(255) DEFAULT NULL,
    `image`                 varchar(255) DEFAULT NULL,
    `cpu`                   double       DEFAULT NULL,
    `memory`                bigint(20) DEFAULT NULL,
    `disk`                  bigint(20) DEFAULT NULL,
    `dice_org`              varchar(255) DEFAULT NULL,
    `dice_project`          varchar(40)  DEFAULT NULL,
    `dice_application`      varchar(255) DEFAULT NULL,
    `dice_runtime`          varchar(40)  DEFAULT NULL,
    `dice_service`          varchar(255) DEFAULT NULL,
    `edas_app_id`           varchar(64)  DEFAULT NULL,
    `edas_app_name`         varchar(128) DEFAULT NULL,
    `edas_group_id`         varchar(64)  DEFAULT NULL,
    `dice_project_name`     varchar(255) DEFAULT NULL,
    `dice_application_name` varchar(255) DEFAULT NULL,
    `dice_runtime_name`     varchar(255) DEFAULT NULL,
    `dice_component`        varchar(255) DEFAULT NULL,
    `dice_addon`            varchar(255) DEFAULT NULL,
    `dice_addon_name`       varchar(255) DEFAULT NULL,
    `dice_workspace`        varchar(255) DEFAULT NULL,
    `dice_shared_level`     varchar(255) DEFAULT NULL,
    `status`                varchar(255) DEFAULT NULL,
    `time_stamp`            bigint(20) DEFAULT NULL,
    `task_id`               varchar(180) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY                     `idx_project_id` (`dice_project`),
    KEY                     `idx_runtime_id` (`dice_runtime`),
    KEY                     `idx_edas_app_id` (`edas_app_id`),
    KEY                     `task_id` (`task_id`),
    KEY                     `container_id` (`container_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='容器实例元数据';

CREATE TABLE `cm_deployments`
(
    `id`               bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`       timestamp NULL DEFAULT NULL,
    `updated_at`       timestamp NULL DEFAULT NULL,
    `org_id`           bigint(20) unsigned DEFAULT NULL,
    `project_id`       bigint(20) unsigned DEFAULT NULL,
    `application_id`   bigint(20) unsigned DEFAULT NULL,
    `pipeline_id`      bigint(20) unsigned DEFAULT NULL,
    `task_id`          bigint(20) unsigned DEFAULT NULL,
    `queue_time_sec`   bigint(20) DEFAULT NULL,
    `cost_time_sec`    bigint(20) DEFAULT NULL,
    `project_name`     varchar(255) DEFAULT NULL,
    `application_name` varchar(255) DEFAULT NULL,
    `task_name`        varchar(255) DEFAULT NULL,
    `status`           varchar(255) DEFAULT NULL,
    `env`              varchar(255) DEFAULT NULL,
    `cluster_name`     varchar(255) DEFAULT NULL,
    `user_id`          varchar(255) DEFAULT NULL,
    `runtime_id`       varchar(255) DEFAULT NULL,
    `release_id`       varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY                `org_id` (`org_id`),
    KEY                `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部署的服务信息';

CREATE TABLE `cm_hosts`
(
    `id`             bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`     timestamp NULL DEFAULT NULL,
    `updated_at`     timestamp NULL DEFAULT NULL,
    `name`           varchar(255) DEFAULT NULL,
    `org_name`       varchar(100) DEFAULT NULL,
    `cluster`        varchar(100) DEFAULT NULL,
    `private_addr`   varchar(255) DEFAULT NULL,
    `cpus`           double       DEFAULT NULL,
    `cpu_usage`      double       DEFAULT NULL,
    `memory`         bigint(20) DEFAULT NULL,
    `memory_usage`   bigint(20) DEFAULT NULL,
    `disk`           bigint(20) DEFAULT NULL,
    `disk_usage`     bigint(20) DEFAULT NULL,
    `load5`          double       DEFAULT NULL,
    `labels`         varchar(255) DEFAULT NULL,
    `os`             varchar(255) DEFAULT NULL,
    `kernel_version` varchar(255) DEFAULT NULL,
    `system_time`    varchar(255) DEFAULT NULL,
    `birthday`       bigint(20) DEFAULT NULL,
    `time_stamp`     bigint(20) DEFAULT NULL,
    `deleted`        tinyint(1) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY              `org_name` (`org_name`),
    KEY              `cluster` (`cluster`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主机元数据';

CREATE TABLE `cm_jobs`
(
    `id`               bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`       timestamp NULL DEFAULT NULL,
    `updated_at`       timestamp NULL DEFAULT NULL,
    `org_id`           bigint(20) unsigned DEFAULT NULL,
    `project_id`       bigint(20) unsigned DEFAULT NULL,
    `application_id`   bigint(20) unsigned DEFAULT NULL,
    `pipeline_id`      bigint(20) unsigned DEFAULT NULL,
    `task_id`          bigint(20) unsigned DEFAULT NULL,
    `queue_time_sec`   bigint(20) DEFAULT NULL,
    `cost_time_sec`    bigint(20) DEFAULT NULL,
    `project_name`     varchar(255) DEFAULT NULL,
    `application_name` varchar(255) DEFAULT NULL,
    `task_name`        varchar(255) DEFAULT NULL,
    `status`           varchar(255) DEFAULT NULL,
    `env`              varchar(255) DEFAULT NULL,
    `cluster_name`     varchar(255) DEFAULT NULL,
    `task_type`        varchar(255) DEFAULT NULL,
    `user_id`          varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY                `org_id` (`org_id`),
    KEY                `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运行的job信息';

CREATE TABLE `co_clusters`
(
    `id`              int(11) NOT NULL AUTO_INCREMENT,
    `org_id`          int(11) NOT NULL,
    `name`            varchar(41)  NOT NULL,
    `display_name`    varchar(255) NOT NULL DEFAULT '',
    `type`            enum('dcos','edas','k8s','localdocker','swarm') NOT NULL,
    `cloud_vendor`    varchar(255) NOT NULL DEFAULT '',
    `logo`            text         NOT NULL,
    `description`     text         NOT NULL,
    `wildcard_domain` varchar(255) NOT NULL DEFAULT '',
    `config`          text,
    `urls`            text,
    `settings`        text,
    `scheduler`       text,
    `opsconfig`       text COMMENT 'OPS配置',
    `resource`        text,
    `sys`             text,
    `created_at`      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `co_clusters_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集群详细配置信息';

CREATE TABLE `dice_app`
(
    `id`               bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `name`             varchar(50) NOT NULL DEFAULT '' COMMENT '名称',
    `logo`             varchar(512)         DEFAULT NULL COMMENT '图标',
    `desc`             varchar(2048)        DEFAULT NULL COMMENT '描述',
    `public`           tinyint(1) DEFAULT NULL COMMENT '是否公开',
    `org_id`           bigint(20) NOT NULL COMMENT '所属组织',
    `solution_id`      bigint(20) DEFAULT NULL COMMENT '解决方案id',
    `solution_name`    varchar(100)         DEFAULT NULL COMMENT '解决方案 名称',
    `capacity_plan_id` bigint(20) DEFAULT NULL COMMENT '解决方案 对应容量方案 id',
    `source_group`     varchar(200)         DEFAULT NULL COMMENT '代码工程所在的 git group,  方案中应用都会在这个group下。',
    `creator`          varchar(255)         DEFAULT NULL COMMENT '创建者',
    `created_at`       datetime    NOT NULL COMMENT '创建时间',
    `updated_at`       datetime             DEFAULT NULL COMMENT '更新时间',
    `git_repo`         varchar(200)         DEFAULT NULL COMMENT 'git repo 地址',
    `git_repo_abbrev`  varchar(200)         DEFAULT NULL COMMENT 'git repo 精简地址',
    `cluster_id`       bigint(20) DEFAULT NULL COMMENT '废弃字段',
    `quota`            varchar(255)         DEFAULT NULL COMMENT '废弃字段',
    `version`          varchar(128)         DEFAULT NULL COMMENT 'schema 版本',
    `project_id`       bigint(20) DEFAULT NULL COMMENT 'the real projectId',
    `config`           varchar(2048)        DEFAULT NULL COMMENT '应用配置',
    `mode`             varchar(125)         DEFAULT NULL COMMENT 'application mode',
    `extra`            varchar(4096)        DEFAULT NULL COMMENT '扩展字段',
    `project_name`     varchar(128)         DEFAULT NULL COMMENT '项目名称',
    `display_name`     varchar(64)          DEFAULT NULL COMMENT '应用展示名称',
    `is_external_repo` tinyint(1) DEFAULT '0',
    `repo_config`      text,
    `unblock_start`    timestamp NULL DEFAULT NULL COMMENT '解封开始时间',
    `unblock_end`      timestamp NULL DEFAULT NULL COMMENT '解封结束时间',
    `is_public`        tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否公开',
    PRIMARY KEY (`id`),
    KEY                `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用信息';

CREATE TABLE `dice_app_certificates`
(
    `id`             bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_id`         bigint(20) NOT NULL COMMENT '所属应用',
    `certificate_id` bigint(20) NOT NULL COMMENT '证书',
    `status`         varchar(45) NOT NULL DEFAULT '' COMMENT '证书审批状态',
    `operator`       varchar(255)         DEFAULT NULL COMMENT '操作者',
    `push_config`    text COMMENT '证书推送信息',
    `created_at`     datetime    NOT NULL COMMENT '创建时间',
    `updated_at`     datetime             DEFAULT NULL COMMENT '更新时间',
    `approval_id`    bigint(20) NOT NULL COMMENT '审批ID',
    PRIMARY KEY (`id`),
    KEY              `certificate_id` (`certificate_id`),
    KEY              `app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用证书信息';

CREATE TABLE `dice_approves`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `org_id`        bigint(20) NOT NULL COMMENT '所属企业',
    `title`         varchar(255) NOT NULL DEFAULT '' COMMENT '审批标题',
    `target_id`     bigint(20) NOT NULL COMMENT '审批对象',
    `entity_id`     bigint(20) NOT NULL COMMENT '审批实体',
    `target_name`   varchar(255) NOT NULL DEFAULT '' COMMENT '审批对象名字',
    `extra`         text COMMENT '其它字段',
    `status`        varchar(45)  NOT NULL DEFAULT '' COMMENT '审批状态',
    `priority`      varchar(45)  NOT NULL DEFAULT '' COMMENT '审批优先级',
    `type`          varchar(64)  NOT NULL DEFAULT '' COMMENT '审批类型',
    `desc`          varchar(2048)         DEFAULT NULL COMMENT '审批描述',
    `approval_time` datetime     NOT NULL COMMENT '审批时间',
    `approver`      varchar(64)           DEFAULT NULL COMMENT '审批人',
    `submitter`     varchar(64)           DEFAULT NULL COMMENT '提交人',
    `created_at`    datetime     NOT NULL COMMENT '创建时间',
    `updated_at`    datetime              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批信息';

CREATE TABLE `dice_audit`
(
    `id`             bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`     datetime     DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at`     datetime     DEFAULT NULL COMMENT '表记录更新时间',
    `start_time`     datetime    NOT NULL COMMENT '事件发生的时间',
    `end_time`       datetime    NOT NULL COMMENT '事件结束的时间',
    `user_id`        varchar(40) NOT NULL COMMENT '事件的操作人',
    `scope_type`     varchar(40) NOT NULL COMMENT '事件发生的scope类型',
    `scope_id`       varchar(40) NOT NULL COMMENT '事件发生的scope类型',
    `app_id`         bigint(20) DEFAULT NULL COMMENT '事件发生的上下文信息，appId，用于前端渲染',
    `project_id`     bigint(20) DEFAULT NULL COMMENT '事件发生的上下文信息，projectId，用于前端渲染',
    `org_id`         bigint(20) DEFAULT NULL COMMENT '事件发生的上下文信息，orgId',
    `context`        text COMMENT '事件发生的自定义上下文信息，用于前端渲染',
    `template_name`  varchar(40) NOT NULL COMMENT '前端渲染事件模版的key',
    `audit_level`    varchar(40)  DEFAULT NULL COMMENT '事件的等级',
    `result`         varchar(40)  DEFAULT NULL COMMENT '事件的结果',
    `error_msg`      text COMMENT '事件的结果为失败时的错误信息',
    `client_ip`      varchar(40)  DEFAULT NULL COMMENT '事件的客户端地址',
    `user_agent`     text COMMENT '事件的客户端类型',
    `deleted`        varchar(40)  DEFAULT '0' COMMENT '事件进入归档表前的软删除标记',
    `fdp_project_id` varchar(128) DEFAULT NULL COMMENT 'fdp项目id',
    PRIMARY KEY (`id`),
    KEY              `start_time` (`start_time`),
    KEY              `end_time` (`end_time`),
    KEY              `org_id` (`org_id`),
    KEY              `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计事件';

CREATE TABLE `dice_audit_history`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`    datetime    DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at`    datetime    DEFAULT NULL COMMENT '表记录更新时间',
    `start_time`    datetime    NOT NULL COMMENT '事件发生的时间',
    `end_time`      datetime    NOT NULL COMMENT '事件结束的时间',
    `user_id`       varchar(40) NOT NULL COMMENT '事件的操作人',
    `scope_type`    varchar(40) NOT NULL COMMENT '事件发生的scope类型',
    `scope_id`      varchar(40) NOT NULL COMMENT '事件发生的scope类型',
    `app_id`        bigint(20) DEFAULT NULL COMMENT '事件发生的上下文信息，appId，用于前端渲染',
    `project_id`    bigint(20) DEFAULT NULL COMMENT '事件发生的上下文信息，projectId，用于前端渲染',
    `org_id`        bigint(20) DEFAULT NULL COMMENT '事件发生的上下文信息，orgId',
    `context`       text COMMENT '事件发生的自定义上下文信息，用于前端渲染',
    `template_name` varchar(40) NOT NULL COMMENT '前端渲染事件模版的key',
    `audit_level`   varchar(40) DEFAULT NULL COMMENT '事件的等级',
    `result`        varchar(40) DEFAULT NULL COMMENT '事件的结果',
    `error_msg`     text COMMENT '事件的结果为失败时的错误信息',
    `client_ip`     varchar(40) DEFAULT NULL COMMENT '事件的客户端地址',
    `user_agent`    tinytext COMMENT '事件的客户端类型',
    `deleted`       varchar(40) DEFAULT '0' COMMENT '事件进入归档表前的软删除标记',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计事件历史';

CREATE TABLE `dice_branch_rules`
(
    `id`                  bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`          timestamp NULL DEFAULT NULL,
    `updated_at`          timestamp NULL DEFAULT NULL,
    `rule`                varchar(150) DEFAULT NULL,
    `desc`                varchar(150) DEFAULT NULL,
    `workspace`           varchar(150) DEFAULT NULL,
    `artifact_workspace`  varchar(150) DEFAULT NULL,
    `is_protect`          tinyint(1) DEFAULT NULL,
    `is_trigger_pipeline` tinyint(1) DEFAULT NULL,
    `need_approval`       tinyint(1) DEFAULT NULL,
    `scope_type`          varchar(50)  DEFAULT NULL,
    `scope_id`            bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分支规则';

CREATE TABLE `dice_certificates`
(
    `id`         bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `org_id`     bigint(20) NOT NULL COMMENT '所属企业',
    `name`       varchar(255) NOT NULL DEFAULT '' COMMENT '证书自定义名称',
    `message`    text,
    `ios`        text,
    `android`    text,
    `status`     varchar(45)  NOT NULL DEFAULT '' COMMENT '企业审批状态',
    `type`       varchar(64)  NOT NULL DEFAULT '' COMMENT '企业类型',
    `desc`       varchar(2048)         DEFAULT NULL COMMENT 'publisher描述',
    `creator`    varchar(255) NOT NULL DEFAULT '' COMMENT '创建用户',
    `operator`   varchar(255)          DEFAULT NULL COMMENT '操作者',
    `created_at` datetime     NOT NULL COMMENT '创建时间',
    `updated_at` datetime              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书信息';

CREATE TABLE `dice_cloud_accounts`
(
    `id`                bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`        timestamp NULL DEFAULT NULL,
    `updated_at`        timestamp NULL DEFAULT NULL,
    `cloud_provider`    varchar(255) DEFAULT NULL,
    `name`              varchar(255) DEFAULT NULL,
    `access_key_id`     varchar(255) DEFAULT NULL,
    `access_key_secret` varchar(255) DEFAULT NULL,
    `org_id`            bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云账号信息，老表';

CREATE TABLE `dice_config_item`
(
    `id`            int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '配置项ID',
    `namespace_id`  int(10) unsigned NOT NULL DEFAULT '0' COMMENT '配置命名空间ID',
    `item_key`      varchar(128) NOT NULL DEFAULT 'default' COMMENT '配置项Key',
    `item_value`    longtext     NOT NULL COMMENT '配置项值',
    `item_comment`  varchar(1024)         DEFAULT '' COMMENT '注释',
    `is_sync`       tinyint(1) DEFAULT '0' COMMENT '是否同步到配置中心',
    `delete_remote` tinyint(1) DEFAULT '0' COMMENT '是否删除远程配置',
    `create_time`   datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '修改时间',
    `is_deleted`    varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    `status`        varchar(32)           DEFAULT 'PUBLISHED' COMMENT '配置状态',
    `source`        varchar(32)           DEFAULT 'DEPLOY_WEB' COMMENT '配置来源',
    `dynamic`       tinyint(1) DEFAULT '1' COMMENT '是否为动态配置',
    `encrypt`       tinyint(1) DEFAULT '0' COMMENT '配置项是否加密',
    `item_type`     varchar(32)           DEFAULT 'ENV' COMMENT '配置类型',
    PRIMARY KEY (`id`),
    KEY             `idx_namespaceid` (`namespace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置项';

CREATE TABLE `dice_config_namespace`
(
    `id`             int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `name`           varchar(255) NOT NULL COMMENT '配置命名空间名称',
    `dynamic`        tinyint(1) DEFAULT '1' COMMENT '存储配置是否为动态配置',
    `is_default`     tinyint(1) DEFAULT '0' COMMENT '是否为默认命名空间',
    `project_id`     varchar(45)  NOT NULL COMMENT '项目ID',
    `env`            varchar(45)           DEFAULT NULL COMMENT '所属部署环境',
    `application_id` varchar(45)           DEFAULT NULL COMMENT '应用ID',
    `runtime_id`     varchar(45)           DEFAULT NULL COMMENT 'runtime ID',
    `create_time`    datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '修改时间',
    `is_deleted`     varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY              `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='配置项namespace';

CREATE TABLE `dice_config_namespace_relation`
(
    `id`                int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `namespace`         varchar(255) NOT NULL COMMENT '配置命名空间名称',
    `default_namespace` varchar(255) NOT NULL COMMENT '默认配置命名空间名称',
    `create_time`       datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
    `update_time`       datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '修改时间',
    `is_deleted`        varchar(1)   NOT NULL DEFAULT 'N' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `namespace` (`namespace`),
    KEY                 `idx_default_namespace` (`default_namespace`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='配置项namespace关联表';

CREATE TABLE `dice_error_box`
(
    `id`              bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`      datetime    DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at`      datetime    DEFAULT NULL COMMENT '表记录更新时间',
    `resource_type`   varchar(40)  NOT NULL COMMENT '资源类型',
    `resource_id`     varchar(40)  NOT NULL COMMENT '资源id',
    `occurrence_time` datetime     NOT NULL COMMENT '日志发生时间',
    `human_log`       text COMMENT '处理过的日志和提示',
    `primeval_log`    text         NOT NULL COMMENT '原生错误',
    `dedup_id`        varchar(190) NOT NULL COMMENT '去重id',
    `level`           varchar(50) DEFAULT 'error' COMMENT '日志级别',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_rtype_rid_did` (`resource_type`,`resource_id`,`dedup_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  COMMENT='错误信息透出记录';

CREATE TABLE `dice_files`
(
    `id`                 bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `uuid`               varchar(32)   NOT NULL DEFAULT '',
    `display_name`       varchar(1024) NOT NULL DEFAULT '',
    `ext`                varchar(32)            DEFAULT '',
    `byte_size`          bigint(20) NOT NULL,
    `storage_type`       varchar(32)   NOT NULL DEFAULT '',
    `full_relative_path` varchar(2048) NOT NULL DEFAULT '',
    `from`               varchar(64)            DEFAULT NULL,
    `creator`            varchar(255)           DEFAULT NULL,
    `extra`              varchar(2048)          DEFAULT NULL,
    `created_at`         datetime               DEFAULT NULL,
    `updated_at`         datetime               DEFAULT NULL,
    `expired_at`         datetime               DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY                  `idx_uuid` (`uuid`),
    KEY                  `idx_storageType` (`storage_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dice 文件表';

CREATE TABLE `dice_issue_app_relations`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at` datetime DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at` datetime DEFAULT NULL COMMENT '表记录更新时间',
    `issue_id`   bigint(20) NOT NULL COMMENT '关联关系源id eg:issue_id',
    `comment_id` bigint(20) NOT NULL COMMENT 'MR评论 id',
    `app_id`     bigint(20) NOT NULL COMMENT '应用 id',
    `mr_id`      bigint(20) NOT NULL COMMENT '关联关系目标id eg:mr_id',
    PRIMARY KEY (`id`),
    KEY          `idx_app` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件应用关联表';

CREATE TABLE `dice_issue_panel`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at` datetime     DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at` datetime     DEFAULT NULL COMMENT '表记录更新时间',
    `project_id` bigint(20) DEFAULT NULL,
    `panel_name` varchar(255) DEFAULT NULL,
    `issue_id`   bigint(20) DEFAULT NULL,
    `relation`   bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件看板表';

CREATE TABLE `dice_issue_property`
(
    `id`                  bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`          datetime     DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at`          datetime     DEFAULT NULL COMMENT '表记录更新时间',
    `scope_type`          varchar(255) DEFAULT NULL,
    `scope_id`            bigint(20) DEFAULT NULL,
    `org_id`              bigint(20) DEFAULT NULL,
    `required`            tinyint(1) DEFAULT NULL,
    `property_type`       varchar(255) DEFAULT NULL,
    `property_name`       varchar(255) DEFAULT NULL,
    `display_name`        varchar(255) DEFAULT NULL,
    `property_issue_type` varchar(255) DEFAULT NULL,
    `relation`            bigint(20) DEFAULT NULL,
    `index`               bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=127 DEFAULT CHARSET=utf8mb4 COMMENT='事件属性表';

CREATE TABLE `dice_issue_property_relation`
(
    `id`                bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`        datetime     DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at`        datetime     DEFAULT NULL COMMENT '表记录更新时间',
    `org_id`            bigint(20) DEFAULT NULL,
    `project_id`        bigint(20) DEFAULT NULL,
    `issue_id`          bigint(20) DEFAULT NULL,
    `property_id`       bigint(20) DEFAULT NULL,
    `property_value_id` bigint(20) DEFAULT NULL,
    `arbitrary_value`   varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=155 DEFAULT CHARSET=utf8mb4 COMMENT='事件属性关联表';

CREATE TABLE `dice_issue_property_value`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`  datetime     DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at`  datetime     DEFAULT NULL COMMENT '表记录更新时间',
    `property_id` bigint(20) DEFAULT NULL,
    `value`       varchar(255) DEFAULT NULL,
    `name`        varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='事件属性值表';

CREATE TABLE `dice_issue_relation`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`    datetime DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at`    datetime DEFAULT NULL COMMENT '表记录更新时间',
    `issue_id`      bigint(20) NOT NULL COMMENT '事件id',
    `related_issue` bigint(20) NOT NULL COMMENT '关联事件id',
    `comment`       text COMMENT '关联描述',
    PRIMARY KEY (`id`),
    UNIQUE KEY `issue_related` (`issue_id`,`related_issue`),
    KEY             `idx_issue_id` (`issue_id`),
    KEY             `idx_related_issue` (`related_issue`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件关联表';

CREATE TABLE `dice_issue_stage`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at` datetime     DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at` datetime     DEFAULT NULL COMMENT '表记录更新时间',
    `org_id`     bigint(20) DEFAULT NULL,
    `name`       varchar(255) DEFAULT NULL,
    `value`      varchar(255) DEFAULT NULL,
    `issue_type` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COMMENT='事件任务阶段+引入源';

CREATE TABLE `dice_issue_state`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at` datetime     DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at` datetime     DEFAULT NULL COMMENT '表记录更新时间',
    `project_id` bigint(20) DEFAULT NULL,
    `issue_type` varchar(255) DEFAULT NULL,
    `name`       varchar(255) DEFAULT NULL,
    `belong`     varchar(255) DEFAULT NULL,
    `index`      bigint(20) DEFAULT NULL,
    `role`       varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=303 DEFAULT CHARSET=utf8mb4 COMMENT='事件状态表';

CREATE TABLE `dice_issue_state_relations`
(
    `id`             bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`     datetime     DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at`     datetime     DEFAULT NULL COMMENT '表记录更新时间',
    `start_state_id` bigint(20) DEFAULT NULL,
    `end_state_id`   bigint(20) DEFAULT NULL,
    `project_id`     bigint(20) DEFAULT NULL,
    `issue_type`     varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=674 DEFAULT CHARSET=utf8mb4 COMMENT='事件状态关联表';

CREATE TABLE `dice_issue_streams`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`    datetime     DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at`    datetime     DEFAULT NULL COMMENT '表记录更新时间',
    `issue_id`      bigint(20) NOT NULL COMMENT '所属 issue ID',
    `operator`      varchar(255) DEFAULT NULL COMMENT '操作者',
    `stream_type`   varchar(255) DEFAULT NULL COMMENT '操作记录类型',
    `stream_params` text COMMENT '操作记录参数',
    PRIMARY KEY (`id`),
    KEY             `issue_id_index` (`issue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件活动记录表';

CREATE TABLE `dice_issues`
(
    `id`               bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`       datetime              DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at`       datetime              DEFAULT NULL COMMENT '表记录更新时间',
    `plan_started_at`  datetime              DEFAULT NULL COMMENT '计划开始时间',
    `plan_finished_at` datetime              DEFAULT NULL COMMENT '计划结束时间',
    `project_id`       bigint(20) NOT NULL COMMENT '所属项目 ID',
    `iteration_id`     bigint(20) NOT NULL COMMENT '所属迭代 ID',
    `app_id`           bigint(20) DEFAULT NULL COMMENT '所属应用 ID',
    `requirement_id`   bigint(20) DEFAULT NULL COMMENT '所属需求 ID',
    `type`             varchar(32)           DEFAULT NULL COMMENT 'issue 类型',
    `title`            varchar(255)          DEFAULT NULL COMMENT '标题',
    `content`          text COMMENT '内容',
    `state`            varchar(32)  NOT NULL DEFAULT '' COMMENT '状态',
    `priority`         varchar(32)           DEFAULT NULL COMMENT '优先级',
    `complexity`       varchar(32)           DEFAULT NULL COMMENT '复杂度',
    `bug_type`         varchar(32)           DEFAULT NULL COMMENT '缺陷类型',
    `creator`          varchar(255)          DEFAULT NULL COMMENT '创建人',
    `assignee`         varchar(255) NOT NULL DEFAULT '' COMMENT '处理人',
    `deleted`          tinyint(4) DEFAULT '0',
    `man_hour`         text COMMENT '事件工时信息',
    `source`           varchar(32)           DEFAULT 'user' COMMENT '事件来源',
    `severity`         varchar(32)           DEFAULT NULL COMMENT '事件严重程度',
    `external`         tinyint(1) DEFAULT '1' COMMENT '是否是外部创建的issue',
    `stage`            varchar(80)           DEFAULT NULL COMMENT 'bug所属阶段和任务类型',
    `owner`            varchar(255)          DEFAULT NULL COMMENT 'bug责任人',
    `finish_time`      datetime              DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件表';

CREATE TABLE `dice_iterations`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`  datetime              DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at`  datetime              DEFAULT NULL COMMENT '表记录更新时间',
    `started_at`  datetime              DEFAULT NULL COMMENT '迭代开始时间',
    `finished_at` datetime              DEFAULT NULL COMMENT '迭代结束时间',
    `project_id`  bigint(20) DEFAULT NULL COMMENT '所属项目 ID',
    `title`       varchar(255)          DEFAULT NULL COMMENT '标题',
    `content`     text COMMENT '内容',
    `creator`     varchar(255)          DEFAULT NULL COMMENT '创建人',
    `state`       varchar(255) NOT NULL DEFAULT 'UNFILED',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='迭代表';

CREATE TABLE `dice_label_relations`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at` datetime DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at` datetime DEFAULT NULL COMMENT '表记录更新时间',
    `label_id`   bigint(20) NOT NULL,
    `ref_type`   varchar(40) NOT NULL COMMENT '标签作用类型, 与 dice_labels type 相同, eg: issue',
    `ref_id`     bigint(20) NOT NULL COMMENT '标签关联目标 id, eg: issue_id',
    PRIMARY KEY (`id`),
    KEY          `idx_label_id` (`label_id`),
    KEY          `idx_ref_id` (`ref_type`,`ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签关联表';

CREATE TABLE `dice_labels`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at` datetime     DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at` datetime     DEFAULT NULL COMMENT '表记录更新时间',
    `name`       varchar(50) NOT NULL COMMENT '标签名称',
    `type`       varchar(40) NOT NULL COMMENT '标签作用类型, eg: issue',
    `color`      varchar(40) NOT NULL COMMENT '标签颜色',
    `project_id` bigint(20) NOT NULL COMMENT '标签所属项目',
    `creator`    varchar(255) DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_project_name` (`project_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

CREATE TABLE `dice_library_references`
(
    `id`              bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_id`          bigint(20) NOT NULL COMMENT '应用 id',
    `lib_id`          bigint(20) NOT NULL COMMENT '库 id',
    `lib_name`        varchar(255) NOT NULL COMMENT '库名称',
    `lib_desc`        text COMMENT '库描述',
    `approval_id`     bigint(20) NOT NULL COMMENT '审批流 id',
    `approval_status` varchar(100) NOT NULL COMMENT '状态: 待审核/已通过/已拒绝',
    `creator`         varchar(255) NOT NULL COMMENT '创建者',
    `created_at`      datetime     NOT NULL COMMENT '创建时间',
    `updated_at`      datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY               `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库引用信息';

CREATE TABLE `dice_manual_review`
(
    `id`               bigint(20) NOT NULL AUTO_INCREMENT COMMENT '审核Id',
    `build_id`         bigint(20) NOT NULL COMMENT '流水线Id',
    `project_id`       bigint(20) NOT NULL COMMENT '项目Id',
    `application_id`   bigint(20) NOT NULL COMMENT '应用Id',
    `sponsor_id`       bigint(20) NOT NULL COMMENT '发起人Id',
    `commit_id`        varchar(50) NOT NULL COMMENT '提交Id',
    `org_id`           bigint(20) NOT NULL COMMENT '企业Id',
    `task_id`          bigint(20) NOT NULL COMMENT 'taskId 为action的唯一标示',
    `project_name`     varchar(50) NOT NULL COMMENT '项目名字',
    `application_name` varchar(50) NOT NULL COMMENT '应用名字',
    `branch_name`      varchar(50) NOT NULL COMMENT '代码分支',
    `approval_status`  varchar(50) NOT NULL COMMENT '审查是否通过 初值为null,no是失败,yes是成功',
    `commit_message`   varchar(50)  DEFAULT NULL COMMENT '评论',
    `created_at`       datetime    NOT NULL COMMENT '创建时间',
    `updated_at`       datetime     DEFAULT NULL COMMENT '更新时间',
    `approval_reason`  varchar(250) DEFAULT NULL COMMENT '拒绝原因',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批列表';

CREATE TABLE `dice_manual_review_user`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增Id',
    `org_id`     bigint(20) NOT NULL COMMENT '企业Id',
    `operator`   bigint(20) NOT NULL COMMENT '用户id',
    `task_id`    bigint(20) NOT NULL COMMENT 'taskId 为action的唯一标示',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批用户列表';

CREATE TABLE `dice_mboxs`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id`    varchar(100) DEFAULT NULL COMMENT '用户id',
    `title`      text COMMENT '站内信标题',
    `content`    text COMMENT '站内信内容',
    `label`      varchar(200) DEFAULT NULL COMMENT '站内信所属模块',
    `status`     varchar(50)  DEFAULT NULL COMMENT '状态 readed:已读 unread:未读',
    `org_id`     bigint(20) DEFAULT NULL,
    `read_at`    datetime     DEFAULT NULL,
    `created_at` timestamp NULL DEFAULT NULL,
    `updated_at` timestamp NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY          `idx_user_id` (`user_id`),
    KEY          `idx_org_id` (`org_id`),
    KEY          `idx_status` (`status`),
    KEY          `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信';

CREATE TABLE `dice_member`
(
    `id`               bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `scope_type`       varchar(10)  NOT NULL DEFAULT '' COMMENT 'scope可选值: sys/org/project/app',
    `scope_id`         bigint(20) DEFAULT NULL COMMENT 'scope id',
    `scope_name`       varchar(200)          DEFAULT NULL COMMENT '企业/项目/应用名称',
    `parent_id`        bigint(20) DEFAULT NULL,
    `org_id`           bigint(20) DEFAULT NULL COMMENT '企业Id',
    `project_id`       bigint(20) DEFAULT NULL COMMENT '项目Id',
    `project_name`     varchar(64)           DEFAULT NULL COMMENT '项目名称',
    `application_id`   bigint(20) DEFAULT NULL COMMENT '应用Id',
    `application_name` varchar(64)           DEFAULT NULL COMMENT '应用名称',
    `role`             varchar(20)  NOT NULL DEFAULT '' COMMENT '角色: Manager/Developer/Tester/Guest',
    `user_id`          varchar(255) NOT NULL DEFAULT '' COMMENT '用户Id',
    `email`            varchar(255)          DEFAULT NULL COMMENT '用户邮箱',
    `mobile`           varchar(40)           DEFAULT NULL,
    `nick`             varchar(128)          DEFAULT NULL COMMENT '用户昵称',
    `avatar`           varchar(1024)         DEFAULT NULL COMMENT '用户头像链接',
    `user_sync_at`     datetime              DEFAULT NULL COMMENT '用户信息同步时间',
    `created_at`       datetime     NOT NULL COMMENT '创建时间',
    `updated_at`       datetime              DEFAULT NULL COMMENT '更新时间',
    `name`             varchar(255)          DEFAULT NULL COMMENT '用户名 (唯一)',
    `token`            varchar(100)          DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_unique_scope_type_id_user_id` (`scope_type`,`scope_id`,`user_id`),
    KEY                `idx_user_id_org_id` (`user_id`,`org_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COMMENT='成员信息';


INSERT INTO `dice_member` (`id`, `scope_type`, `scope_id`, `scope_name`, `parent_id`, `org_id`, `project_id`,
                           `project_name`, `application_id`, `application_name`, `role`, `user_id`, `email`, `mobile`,
                           `nick`, `avatar`, `user_sync_at`, `created_at`, `updated_at`, `name`, `token`)
VALUES (1, 'sys', 0, '', 0, 0, 0, '', 0, '', 'Owner', '1', 'admin@dice.terminus.io', NULL, 'admin', '',
        '2019-08-16 14:55:15', '2019-08-16 14:55:15', '2019-08-16 14:55:15', 'admin', NULL);

CREATE TABLE `dice_member_extra`
(
    `id`             bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`     datetime    DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at`     datetime    DEFAULT NULL COMMENT '表记录更新时间',
    `user_id`        varchar(40) NOT NULL COMMENT '成员的用户id',
    `parent_id`      varchar(40) DEFAULT '0' COMMENT '成员的父scope id',
    `scope_id`       varchar(40) NOT NULL COMMENT '成员所属scope id',
    `scope_type`     varchar(40) NOT NULL COMMENT '成员所属scope类型',
    `resource_key`   varchar(40) NOT NULL COMMENT '成员关联资源的键',
    `resource_value` varchar(40) NOT NULL COMMENT '成员关联资源的值',
    PRIMARY KEY (`id`),
    KEY              `idx_user_id_scope_id_scope_type` (`user_id`,`scope_id`,`scope_type`),
    KEY              `idx_resource_key` (`resource_key`),
    KEY              `idx_resource_value` (`resource_value`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4  COMMENT='用户额外信息kv表';


INSERT INTO `dice_member_extra` (`id`, `created_at`, `updated_at`, `user_id`, `parent_id`, `scope_id`, `scope_type`,
                                 `resource_key`, `resource_value`)
VALUES (1, '2019-08-16 14:55:15', '2019-08-16 14:55:15', '1', '0', '0', 'sys', 'role', 'Owner');

CREATE TABLE `dice_nexus_repositories`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `org_id`       bigint(20) DEFAULT NULL,
    `publisher_id` bigint(20) DEFAULT NULL,
    `cluster_name` varchar(128) DEFAULT NULL,
    `name`         varchar(128) DEFAULT NULL,
    `format`       varchar(32)  DEFAULT NULL,
    `type`         varchar(32)  DEFAULT NULL,
    `config`       text,
    `created_at`   datetime NOT NULL,
    `updated_at`   datetime NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nexus 仓库表';

CREATE TABLE `dice_nexus_users`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `repo_id`      bigint(20) DEFAULT NULL,
    `org_id`       bigint(20) DEFAULT NULL,
    `publisher_id` bigint(20) DEFAULT NULL,
    `cluster_name` varchar(128)           DEFAULT NULL,
    `name`         varchar(128)  NOT NULL DEFAULT '',
    `password`     varchar(4096) NOT NULL DEFAULT '',
    `config`       text,
    `created_at`   datetime      NOT NULL,
    `updated_at`   datetime      NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nexus 仓库用户表';

CREATE TABLE `dice_notices`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `org_id`     bigint(20) NOT NULL COMMENT '企业 id',
    `content`    text         NOT NULL COMMENT '公告内容',
    `status`     varchar(50)  NOT NULL COMMENT '状态: 待发布/已发布/已停用',
    `creator`    varchar(255) NOT NULL COMMENT '创建者',
    `created_at` datetime     NOT NULL COMMENT '创建时间',
    `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY          `idx_org_id` (`org_id`),
    KEY          `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台公告';

CREATE TABLE `dice_notifies`
(
    `id`              bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`      timestamp NULL DEFAULT NULL,
    `updated_at`      timestamp NULL DEFAULT NULL,
    `name`            varchar(150) DEFAULT NULL,
    `scope_type`      varchar(150) DEFAULT NULL,
    `scope_id`        varchar(150) DEFAULT NULL,
    `label`           varchar(150) DEFAULT NULL,
    `channels`        text,
    `notify_group_id` bigint(20) DEFAULT NULL,
    `org_id`          bigint(20) DEFAULT NULL,
    `creator`         varchar(255) DEFAULT NULL,
    `enabled`         tinyint(1) DEFAULT NULL,
    `data`            text,
    `cluster_name`    varchar(150) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY               `idx_scope_type` (`scope_type`),
    KEY               `idx_scope_id` (`scope_id`),
    KEY               `idx_notify_group_id` (`notify_group_id`),
    KEY               `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知';

CREATE TABLE `dice_notify_groups`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`   timestamp NULL DEFAULT NULL,
    `updated_at`   timestamp NULL DEFAULT NULL,
    `name`         varchar(150) DEFAULT NULL,
    `scope_type`   varchar(150) DEFAULT NULL,
    `scope_id`     varchar(150) DEFAULT NULL,
    `org_id`       bigint(20) DEFAULT NULL,
    `target_data`  text,
    `label`        varchar(200) DEFAULT NULL,
    `auto_create`  tinyint(1) DEFAULT NULL,
    `creator`      varchar(150) DEFAULT NULL,
    `cluster_name` varchar(150) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY            `idx_scope_type` (`scope_type`),
    KEY            `idx_scope_id` (`scope_id`),
    KEY            `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知组';

CREATE TABLE `dice_notify_histories`
(
    `id`                       bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`               timestamp NULL DEFAULT NULL,
    `updated_at`               timestamp NULL DEFAULT NULL,
    `notify_name`              varchar(150) DEFAULT NULL,
    `notify_item_display_name` varchar(150) DEFAULT NULL,
    `channel`                  varchar(150) DEFAULT NULL,
    `target_data`              text,
    `source_data`              text,
    `status`                   varchar(150) DEFAULT NULL,
    `error_msg`                text,
    `org_id`                   bigint(20) DEFAULT NULL,
    `label`                    varchar(150) DEFAULT NULL,
    `source_type`              varchar(150) DEFAULT NULL,
    `source_id`                varchar(150) DEFAULT NULL,
    `cluster_name`             varchar(150) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY                        `idx_notify_name` (`notify_name`),
    KEY                        `idx_org_id` (`org_id`),
    KEY                        `idx_source_type` (`source_type`),
    KEY                        `idx_source_id` (`source_id`),
    KEY                        `idx_module` (`label`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知历史';

CREATE TABLE `dice_notify_item_relation`
(
    `id`             bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`     timestamp NULL DEFAULT NULL,
    `updated_at`     timestamp NULL DEFAULT NULL,
    `notify_id`      bigint(20) DEFAULT NULL,
    `notify_item_id` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY              `idx_notify_id` (`notify_id`),
    KEY              `idx_notify_item_id` (`notify_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知项通知关联关系';

CREATE TABLE `dice_notify_items`
(
    `id`                 bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`         timestamp NULL DEFAULT NULL,
    `updated_at`         timestamp NULL DEFAULT NULL,
    `name`               varchar(150) DEFAULT NULL,
    `display_name`       varchar(150) DEFAULT NULL,
    `category`           varchar(150) DEFAULT NULL,
    `mobile_template`    text,
    `mbox_template`      text,
    `email_template`     text,
    `dingding_template`  text,
    `scope_type`         varchar(150) DEFAULT NULL,
    `label`              varchar(150) DEFAULT NULL,
    `params`             text,
    `vms_template`       text COMMENT '语音通知模版',
    `called_show_number` text COMMENT '语音通知被叫显号',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COMMENT='通知项';


INSERT INTO `dice_notify_items` (`id`, `created_at`, `updated_at`, `name`, `display_name`, `category`,
                                 `mobile_template`, `mbox_template`, `email_template`, `dingding_template`,
                                 `scope_type`, `label`, `params`, `vms_template`, `called_show_number`)
VALUES (1, '2021-05-28 03:30:23', '2021-05-28 03:30:23', 'git_push', '代码推送', 'git', NULL,
        '### {{projectName}}/{{appName}} 代码推送\n- 推送前commit：{{beforeID}}\n- 推送后commit：{{afterID}}\n- 推送引用：{{ref}}\n- 提交人：{{pusherName}}',
        '<p>{{projectName}}/{{appName}} 代码推送 </p>\n<p>推送前commit： {{beforeID}}</p>\n<p>推送后commit： {{afterID}}</p>\n<p>推送引用 ：{{ref}}</p>\n<p>提交人：{{pusherName}}</p>',
        '### {{projectName}}/{{appName}} 代码推送\n- 推送前commit：{{beforeID}}\n- 推送后commit：{{afterID}}\n- 推送引用：{{ref}}\n- 提交人：{{pusherName}}',
        'app', 'workbench', 'projectName,appName,beforeID,afterID,ref,pusherName', NULL, NULL),
       (2, '2021-05-28 03:30:23', '2021-05-28 03:30:23', 'Failed', '工作流运行失败', 'workflow', NULL, NULL,
        '<p>工作流名:{{workflowName}}</p>\n<p>工作流ID: {{workflowID}}</p>\n<p>pipelineID:{{pipelineID}}</p>\n<p>事件名:{{notifyItemName}}</p>\n{{failedDetail}}',
        NULL, 'org', 'cdp', 'workflowName,workflowID,pipelineID,notifyItemName,failedDetail', NULL, NULL),
       (3, '2021-05-28 03:30:23', '2021-05-28 03:30:23', 'Running', '工作流开始运行', 'workflow', NULL, NULL,
        '<p>工作流名:{{workflowName}}</p>\n<p>工作流ID: {{workflowID}}</p>\n<p>pipelineID:{{pipelineID}}</p>\n<p>事件名:{{notifyItemName}}</p>',
        NULL, 'org', 'cdp', 'workflowName,workflowID,pipelineID,notifyItemName', NULL, NULL),
       (4, '2021-05-28 03:30:23', '2021-05-28 03:30:23', 'Success', '工作流运行成功', 'workflow', NULL, NULL,
        '<p>工作流名:{{workflowName}}</p>\n<p>工作流ID: {{workflowID}}</p>\n<p>pipelineID:{{pipelineID}}</p>\n<p>事件名:{{notifyItemName}}</p>',
        NULL, 'org', 'cdp', 'workflowName,workflowID,pipelineID,notifyItemName', NULL, NULL),
       (5, '2021-05-28 03:54:58', NULL, 'pipeline_running', NULL, 'pipeline', NULL, NULL, NULL, NULL, 'app',
        'workbench', NULL, NULL, NULL),
       (6, '2021-05-28 03:54:58', NULL, 'pipeline_success', NULL, 'pipeline', NULL, NULL, NULL, NULL, 'app',
        'workbench', NULL, NULL, NULL),
       (7, '2021-05-28 03:54:58', NULL, 'pipeline_failed', NULL, 'pipeline', NULL, NULL, NULL, NULL, 'app', 'workbench',
        NULL, NULL, NULL),
       (8, '2021-05-28 03:54:58', NULL, 'issue_create', NULL, 'issue', NULL, NULL, NULL, NULL, 'project', 'workbench',
        NULL, NULL, NULL),
       (9, '2021-05-28 03:54:58', NULL, 'issue_update', NULL, 'issue', NULL, NULL, NULL, NULL, 'project', 'workbench',
        NULL, NULL, NULL),
       (10, '2021-05-28 03:54:58', NULL, 'git_create_mr', NULL, 'git', NULL, NULL, NULL, NULL, 'app', 'workbench', NULL,
        NULL, NULL),
       (11, NULL, NULL, 'git_merge_mr', NULL, 'git', NULL, NULL, NULL, NULL, 'app', 'workbench', NULL, NULL, NULL),
       (12, NULL, NULL, 'git_close_mr', NULL, 'git', NULL, NULL, NULL, NULL, 'app', 'workbench', NULL, NULL, NULL),
       (13, NULL, NULL, 'git_comment_mr', NULL, 'git', NULL, NULL, NULL, NULL, 'app', 'workbench', NULL, NULL, NULL),
       (14, NULL, NULL, 'git_delete_branch', NULL, 'git', NULL, NULL, NULL, NULL, 'app', 'workbanch', NULL, NULL, NULL),
       (15, NULL, NULL, 'git_delete_tag', NULL, 'git', NULL, NULL, NULL, NULL, 'app', 'workbanch', NULL, NULL, NULL);

CREATE TABLE `dice_notify_sources`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`  timestamp NULL DEFAULT NULL,
    `updated_at`  timestamp NULL DEFAULT NULL,
    `name`        varchar(150) DEFAULT NULL,
    `notify_id`   bigint(20) DEFAULT NULL,
    `source_type` varchar(255) DEFAULT NULL,
    `source_id`   varchar(255) DEFAULT NULL,
    `org_id`      bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY           `idx_notify_id` (`notify_id`),
    KEY           `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知（dice_notifies）扩展表';

CREATE TABLE `dice_org`
(
    `id`              bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`            varchar(50)  NOT NULL DEFAULT '' COMMENT '企业名称',
    `logo`            varchar(512)          DEFAULT NULL COMMENT '企业logo',
    `config`          text,
    `locale`          varchar(50)           DEFAULT 'zh_CN',
    `desc`            varchar(2048)         DEFAULT NULL COMMENT '企业描述',
    `type`            varchar(45)  NOT NULL DEFAULT '' COMMENT '企业类型',
    `creator`         varchar(255) NOT NULL DEFAULT '' COMMENT '创建用户',
    `operation`       varchar(255)          DEFAULT NULL COMMENT '操作者',
    `status`          varchar(45)  NOT NULL DEFAULT '' COMMENT '企业状态',
    `open_fdp`        tinyint(1) DEFAULT '0' COMMENT '是否打开fdp服务，默认为false',
    `created_at`      datetime     NOT NULL COMMENT '创建时间',
    `updated_at`      datetime              DEFAULT NULL COMMENT '更新时间',
    `version`         varchar(128)          DEFAULT NULL COMMENT '版本',
    `display_name`    varchar(64)           DEFAULT NULL COMMENT '企业展示名称',
    `blockout_config` text COMMENT '封网配置',
    `is_public`       tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否公开',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业信息';

CREATE TABLE `dice_org_cluster_relation`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`   timestamp NULL DEFAULT NULL,
    `updated_at`   timestamp NULL DEFAULT NULL,
    `org_id`       bigint(20) unsigned DEFAULT NULL,
    `org_name`     varchar(255) DEFAULT NULL,
    `cluster_id`   bigint(20) unsigned DEFAULT NULL,
    `cluster_name` varchar(255) DEFAULT NULL,
    `creator`      varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_org_cluster_id` (`org_id`,`cluster_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业集群关联关系';

CREATE TABLE `dice_publishers`
(
    `id`             bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           varchar(50)  NOT NULL DEFAULT '' COMMENT 'publisher名称',
    `publisher_type` varchar(50)  NOT NULL DEFAULT '' COMMENT 'publisher类型',
    `logo`           varchar(512)          DEFAULT NULL COMMENT 'publisher Logo',
    `desc`           varchar(2048)         DEFAULT NULL COMMENT 'publisher描述',
    `creator`        varchar(255) NOT NULL COMMENT '创建者',
    `org_id`         bigint(20) NOT NULL COMMENT '所属企业',
    `created_at`     datetime     NOT NULL COMMENT '创建时间',
    `updated_at`     datetime              DEFAULT NULL COMMENT '更新时间',
    `publisher_key`  varchar(64)  NOT NULL DEFAULT '' COMMENT 'publisher key',
    PRIMARY KEY (`id`),
    KEY              `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='publisher信息';

CREATE TABLE `dice_role_permission`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `created_at`    timestamp NULL DEFAULT NULL,
    `updated_at`    timestamp NULL DEFAULT NULL,
    `role`          varchar(30)  DEFAULT NULL,
    `resource`      varchar(40)  DEFAULT NULL,
    `action`        varchar(30)  DEFAULT NULL,
    `creator`       varchar(255) DEFAULT NULL,
    `resource_role` varchar(30)  DEFAULT NULL COMMENT '角色: Creator/Assignee',
    `scope`         varchar(30)  DEFAULT NULL COMMENT '角色所属的scope',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_resource_action` (`role`,`resource`,`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限表';

CREATE TABLE `favorited_resources`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at` timestamp NULL DEFAULT NULL,
    `updated_at` timestamp NULL DEFAULT NULL,
    `target`     varchar(255) DEFAULT NULL,
    `target_id`  bigint(20) unsigned DEFAULT NULL,
    `user_id`    varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='最喜欢的资源表';

CREATE TABLE `ps_activities`
(
    `id`             bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`     timestamp NULL DEFAULT NULL,
    `updated_at`     timestamp NULL DEFAULT NULL,
    `org_id`         bigint(20) DEFAULT NULL,
    `project_id`     bigint(20) DEFAULT NULL,
    `application_id` bigint(20) DEFAULT NULL,
    `build_id`       bigint(20) DEFAULT NULL,
    `runtime_id`     bigint(20) DEFAULT NULL,
    `operator`       varchar(255) DEFAULT NULL,
    `type`           varchar(255) DEFAULT NULL,
    `action`         varchar(255) DEFAULT NULL,
    `desc`           varchar(255) DEFAULT NULL,
    `context`        text,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dice 活动表';

CREATE TABLE `ps_comments`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`   timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `updated_at`   timestamp NULL DEFAULT NULL COMMENT '更新时间',
    `ticket_id`    bigint(20) DEFAULT NULL COMMENT '工单id',
    `comment_type` varchar(20)  DEFAULT 'normal' COMMENT '评论类型: normal/issueRelation',
    `content`      text COMMENT '评论内容',
    `ir_comment`   text COMMENT '关联事件评论内容',
    `user_id`      varchar(255) DEFAULT NULL COMMENT '用户Id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单评论';

CREATE TABLE `ps_group_projects`
(
    `id`              bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`            varchar(50)  NOT NULL DEFAULT '' COMMENT '项目名称',
    `display_name`    varchar(50)           DEFAULT NULL COMMENT '项目显示名称',
    `logo`            varchar(512)          DEFAULT NULL COMMENT '项目Logo',
    `desc`            varchar(2048)         DEFAULT NULL COMMENT '项目描述',
    `cluster_config`  varchar(1000)         DEFAULT NULL COMMENT '项目集群配置',
    `cpu_quota`       decimal(65, 2)        DEFAULT NULL COMMENT '项目 cpu 配额',
    `mem_quota`       decimal(65, 2)        DEFAULT NULL COMMENT '项目 mem 配额',
    `creator`         varchar(255) NOT NULL COMMENT '创建者',
    `org_id`          bigint(20) NOT NULL COMMENT '所属企业',
    `version`         varchar(128)          DEFAULT NULL COMMENT '版本',
    `created_at`      datetime     NOT NULL COMMENT '创建时间',
    `updated_at`      datetime              DEFAULT NULL COMMENT '更新时间',
    `dd_hook`         text COMMENT 'dingding hook',
    `email`           text COMMENT 'email 地址',
    `functions`       varchar(255)          DEFAULT '{"projectCooperative": true, "testManagement": true, "codeQuality": true, "codeBase": true,"branchRule": true, "cicd": true, "productLibManagement": true, "notify": true}',
    `active_time`     datetime              DEFAULT NULL,
    `rollback_config` varchar(1000)         DEFAULT NULL COMMENT '回滚点配置',
    `enable_ns`       tinyint(1) DEFAULT '0' COMMENT '是否开启项目级命名空间',
    `is_public`       tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否公开',
    PRIMARY KEY (`id`),
    KEY               `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目信息';

CREATE TABLE `ps_tickets`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`    timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `updated_at`    timestamp NULL DEFAULT NULL COMMENT '更新时间',
    `title`         varchar(255) DEFAULT NULL COMMENT '工单标题',
    `content`       text COMMENT '工单内容',
    `type`          varchar(20)  DEFAULT NULL COMMENT '工单类型',
    `priority`      varchar(255) DEFAULT NULL COMMENT '工单优先级',
    `status`        varchar(255) DEFAULT NULL COMMENT '工单状态',
    `request_id`    varchar(60)  DEFAULT NULL COMMENT '请求id',
    `key`           varchar(64)  DEFAULT NULL COMMENT '唯一key,告警工单使用',
    `org_id`        varchar(255) DEFAULT NULL COMMENT '所属企业',
    `metric`        varchar(255) DEFAULT NULL COMMENT '度量类型',
    `metric_id`     varchar(255) DEFAULT NULL COMMENT '度量id',
    `count`         bigint(20) DEFAULT NULL COMMENT '工单计数',
    `creator`       varchar(255) DEFAULT NULL COMMENT '创建用户',
    `last_operator` varchar(255) DEFAULT NULL COMMENT '最近操作用户',
    `label`         text COMMENT '工单label',
    `target_type`   varchar(40)  DEFAULT NULL COMMENT '目标类型',
    `target_id`     varchar(255) DEFAULT NULL COMMENT '目标id',
    `triggered_at`  timestamp NULL DEFAULT NULL COMMENT '工单触发时间',
    `closed_at`     timestamp NULL DEFAULT NULL COMMENT '工单关闭时间',
    PRIMARY KEY (`id`),
    KEY             `idx_type` (`type`),
    KEY             `idx_request_id` (`request_id`),
    KEY             `idx_target_type` (`target_type`),
    KEY             `idx_key` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单信息';

CREATE TABLE `ps_user_current_org`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `created_at` datetime     NOT NULL COMMENT '创建时间',
    `updated_at` datetime              DEFAULT NULL COMMENT '更新时间',
    `user_id`    varchar(256) NOT NULL DEFAULT '' COMMENT '用户 ID',
    `org_id`     bigint(20) NOT NULL COMMENT '组织 ID',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户当前所属企业';

