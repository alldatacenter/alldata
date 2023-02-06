-- MIGRATION_BASE

CREATE TABLE `ci_v3_build_artifacts`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`    datetime              DEFAULT NULL COMMENT '创建时间',
    `updated_at`    datetime              DEFAULT NULL COMMENT '更新时间',
    `sha_256`       varchar(128) NOT NULL COMMENT '构建产物的 SHA256',
    `identity_text` text         NOT NULL COMMENT '构建产物用于计算 SHA256 的唯一标识内容',
    `type`          varchar(128) NOT NULL COMMENT '构建产物类型',
    `content`       text         NOT NULL COMMENT '构建产物的内容',
    `cluster_name`  varchar(255) NOT NULL DEFAULT '' COMMENT '集群名',
    `pipeline_id`   bigint(20) NOT NULL COMMENT '关联的流水线 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `sha_256` (`sha_256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  COMMENT='buildpack action 使用的构建产物表';

CREATE TABLE `ci_v3_build_caches`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         varchar(200) DEFAULT NULL COMMENT '缓存名',
    `cluster_name` varchar(200) DEFAULT NULL COMMENT '集群名',
    `last_pull_at` datetime     DEFAULT NULL COMMENT '缓存最近一次被拉取的时间',
    `created_at`   datetime     DEFAULT NULL COMMENT '创建时间',
    `updated_at`   datetime     DEFAULT NULL COMMENT '更新时间',
    `deleted_at`   datetime     DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    KEY            `idx_name` (`name`),
    KEY            `idx_cluster_name` (`cluster_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='buildpack action 使用的构建缓存';

CREATE TABLE `dice_pipeline_cms_configs`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `ns_id`        bigint(20) NOT NULL,
    `key`          varchar(191) NOT NULL DEFAULT '',
    `value`        text,
    `encrypt`      tinyint(1) NOT NULL,
    `type`         varchar(32)           DEFAULT NULL,
    `extra`        text,
    `time_created` datetime     NOT NULL,
    `time_updated` datetime     NOT NULL,
    PRIMARY KEY (`id`),
    KEY            `idx_ns_key` (`ns_id`,`key`),
    KEY            `idx_key` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线配置项表';

CREATE TABLE `dice_pipeline_cms_ns`
(
    `id`              bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `pipeline_source` varchar(191) NOT NULL DEFAULT '',
    `ns`              varchar(191) NOT NULL DEFAULT '',
    `time_created`    datetime     NOT NULL,
    `time_updated`    datetime     NOT NULL,
    PRIMARY KEY (`id`),
    KEY               `idx_source_ns` (`pipeline_source`,`ns`),
    KEY               `idx_source` (`pipeline_source`),
    KEY               `idx_ns` (`ns`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线配置命名空间表';

CREATE TABLE `dice_pipeline_lifecycle_hook_clients`
(
    `id`     bigint(20) NOT NULL COMMENT '主键',
    `host`   varchar(255) NOT NULL COMMENT '域名',
    `name`   varchar(255) NOT NULL COMMENT '来源名称',
    `prefix` varchar(255) NOT NULL COMMENT '访问前缀',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


INSERT INTO `dice_pipeline_lifecycle_hook_clients` (`id`, `host`, `name`, `prefix`)
VALUES (1, 'fdp-master.default.svc.cluster.local:8080', 'FDP', '/api/fdp/workflows');

CREATE TABLE `dice_pipeline_reports`
(
    `id`          bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `pipeline_id` bigint(20) NOT NULL COMMENT '关联的流水线 ID',
    `type`        varchar(32) NOT NULL DEFAULT '' COMMENT '报告类型',
    `meta`        text        NOT NULL COMMENT '报告元数据',
    `creator_id`  varchar(191)         DEFAULT '' COMMENT '创建人',
    `updater_id`  varchar(191)         DEFAULT NULL COMMENT '更新人',
    `created_at`  datetime    NOT NULL COMMENT '表记录创建时间',
    `updated_at`  datetime    NOT NULL COMMENT '表记录更新时间',
    PRIMARY KEY (`id`),
    KEY           `idx_pipelineid_type` (`pipeline_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线报告表';

CREATE TABLE `dice_pipeline_snippet_clients`
(
    `id`    bigint(20) NOT NULL,
    `name`  varchar(255) NOT NULL,
    `host`  varchar(255) NOT NULL,
    `extra` text         NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线 snippet 客户端表';


INSERT INTO `dice_pipeline_snippet_clients` (`id`, `name`, `host`, `extra`)
VALUES (1, 'local', 'gittar-adaptor.default.svc.cluster.local:1086',
        '{\n \"urlPathPrefix\": \"/api/pipeline-snippets\"\n}'),
       (2, 'autotest', 'qa.default.svc.cluster.local:3033',
        '{\n \"urlPathPrefix\": \"/api/autotests/pipeline-snippets\"\n}');

CREATE TABLE `pipeline_archives`
(
    `id`                bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `time_created`      datetime     NOT NULL,
    `time_updated`      datetime     NOT NULL,
    `pipeline_id`       bigint(20) NOT NULL,
    `pipeline_source`   varchar(32)  NOT NULL DEFAULT '',
    `pipeline_yml_name` varchar(191) NOT NULL DEFAULT '',
    `status`            varchar(191) NOT NULL DEFAULT '',
    `dice_version`      varchar(32)  NOT NULL DEFAULT '',
    `content`           longtext     NOT NULL,
    PRIMARY KEY (`id`),
    KEY                 `idx_pipelineID` (`pipeline_id`),
    KEY                 `idx_source_ymlName` (`pipeline_source`,`pipeline_yml_name`),
    KEY                 `idx_source_ymlName_status` (`pipeline_source`,`pipeline_yml_name`,`status`),
    KEY                 `idx_source_status` (`pipeline_source`,`status`),
    KEY                 `idx_source_pipelineID` (`pipeline_source`,`pipeline_id`),
    KEY                 `idx_source_ymlName_pipelineID` (`pipeline_source`,`pipeline_yml_name`,`pipeline_id`),
    KEY                 `idx_source_status_pipelineID` (`pipeline_source`,`status`,`pipeline_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线归档表';

CREATE TABLE `pipeline_bases`
(
    `id`                 bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `pipeline_source`    varchar(191) NOT NULL DEFAULT '',
    `pipeline_yml_name`  varchar(191) NOT NULL DEFAULT '',
    `cluster_name`       varchar(191) NOT NULL DEFAULT '',
    `status`             varchar(32)  NOT NULL DEFAULT '',
    `type`               varchar(32)  NOT NULL DEFAULT '',
    `trigger_mode`       varchar(32)  NOT NULL DEFAULT '',
    `cron_id`            bigint(20) DEFAULT NULL,
    `is_snippet`         tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否是嵌套流水线',
    `parent_pipeline_id` bigint(20) DEFAULT NULL COMMENT '当前嵌套流水线对应的父流水线 ID',
    `parent_task_id`     bigint(20) DEFAULT NULL COMMENT '当前嵌套流水线对应的父流水线任务 ID',
    `cost_time_sec`      bigint(20) NOT NULL,
    `time_begin`         datetime              DEFAULT NULL,
    `time_end`           datetime              DEFAULT NULL,
    `time_created`       datetime     NOT NULL,
    `time_updated`       datetime     NOT NULL,
    PRIMARY KEY (`id`),
    KEY                  `idx_source_ymlName` (`pipeline_source`,`pipeline_yml_name`),
    KEY                  `idx_status` (`status`),
    KEY                  `idx_source_status` (`pipeline_source`,`status`),
    KEY                  `idx_source_ymlName_status` (`pipeline_source`,`pipeline_yml_name`,`status`),
    KEY                  `idx_id_source_cluster_status` (`id`,`pipeline_source`,`cluster_name`,`status`),
    KEY                  `idx_source_status_cluster_timebegin_timeend_id` (`pipeline_source`,`status`,`cluster_name`,`time_begin`,`time_end`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10000000 DEFAULT CHARSET=utf8mb4 COMMENT='流水线基础信息表';

CREATE TABLE `pipeline_configs`
(
    `id`    bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `type`  varchar(255) NOT NULL DEFAULT '',
    `value` text         NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='(内部)流水线内部配置表';


INSERT INTO `pipeline_configs` (`id`, `type`, `value`)
VALUES (1, 'action_executor',
        '{\n	\"kind\": \"SCHEDULER\",\n	\"name\": \"scheduler\",\n	\"options\": {\n		\"ADDR\": \"scheduler.marathon.l4lb.thisdcos.directory:9091\"\n	}\n}');

CREATE TABLE `pipeline_crons`
(
    `id`                bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `application_id`    bigint(20) NOT NULL,
    `branch`            varchar(255) NOT NULL,
    `cron_expr`         varchar(255) NOT NULL DEFAULT '',
    `enable`            tinyint(1) NOT NULL,
    `pipeline_source`   varchar(32)           DEFAULT NULL,
    `pipeline_yml_name` varchar(255) NOT NULL DEFAULT '',
    `base_pipeline_id`  bigint(20) NOT NULL,
    `extra`             mediumtext,
    `time_created`      datetime     NOT NULL,
    `time_updated`      datetime     NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  COMMENT='定时流水线';

CREATE TABLE `pipeline_extras`
(
    `pipeline_id`   bigint(20) unsigned NOT NULL,
    `pipeline_yml`  mediumtext NOT NULL,
    `extra`         mediumtext NOT NULL,
    `normal_labels` mediumtext NOT NULL COMMENT '这里存储的 label 仅用于展示，不做筛选。用于筛选的 label 存储在 pipeline_labels 表中',
    `snapshot`      mediumtext NOT NULL,
    `commit_detail` text       NOT NULL,
    `progress`      int(3) NOT NULL DEFAULT '-1' COMMENT '0-100，-1 表示未设置',
    `time_created`  datetime   NOT NULL,
    `time_updated`  datetime   NOT NULL,
    `commit`        varchar(64)  DEFAULT NULL,
    `org_name`      varchar(191) DEFAULT NULL,
    `snippets`      mediumtext COMMENT 'snippet 历史',
    PRIMARY KEY (`pipeline_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线额外信息表';

CREATE TABLE `pipeline_labels`
(
    `id`                bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `type`              varchar(16)           DEFAULT NULL,
    `pipeline_source`   varchar(32)  NOT NULL DEFAULT '',
    `pipeline_yml_name` varchar(191) NOT NULL DEFAULT '',
    `target_id`         bigint(20) DEFAULT NULL,
    `key`               varchar(191) NOT NULL DEFAULT '',
    `value`             varchar(191) NOT NULL DEFAULT '' COMMENT '标签值',
    `time_created`      datetime     NOT NULL,
    `time_updated`      datetime     NOT NULL,
    PRIMARY KEY (`id`),
    KEY                 `idx_source` (`pipeline_source`),
    KEY                 `idx_pipeline_yml_name` (`pipeline_yml_name`),
    KEY                 `idx_namespace` (`pipeline_source`,`pipeline_yml_name`),
    KEY                 `idx_key` (`key`),
    KEY                 `idx_target_id` (`target_id`),
    KEY                 `idx_type_source_key_value_targetid` (`type`,`pipeline_source`,`key`,`value`,`target_id`),
    KEY                 `idx_type_source_ymlname_key_value_targetid` (`type`,`pipeline_source`,`pipeline_yml_name`,`key`,`value`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  COMMENT='流水线标签表';

CREATE TABLE `pipeline_stages`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `pipeline_id`   bigint(20) NOT NULL,
    `name`          varchar(255) NOT NULL DEFAULT '',
    `extra`         text         NOT NULL,
    `status`        varchar(255) NOT NULL DEFAULT '',
    `cost_time_sec` bigint(20) NOT NULL,
    `time_begin`    datetime              DEFAULT NULL,
    `time_end`      datetime              DEFAULT NULL,
    `time_created`  datetime     NOT NULL,
    `time_updated`  datetime     NOT NULL,
    PRIMARY KEY (`id`),
    KEY             `idx_pipeline_id` (`pipeline_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  COMMENT='流水线阶段(stage)表';

CREATE TABLE `pipeline_tasks`
(
    `id`                      bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `pipeline_id`             bigint(20) NOT NULL,
    `stage_id`                bigint(20) NOT NULL,
    `name`                    varchar(255) NOT NULL DEFAULT '',
    `op_type`                 varchar(255) NOT NULL DEFAULT '',
    `type`                    varchar(255) NOT NULL DEFAULT '',
    `executor_kind`           varchar(255) NOT NULL DEFAULT '',
    `status`                  varchar(128) NOT NULL,
    `extra`                   mediumtext   NOT NULL,
    `context`                 text         NOT NULL,
    `result`                  mediumtext         NOT NULL,
    `is_snippet`              tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否是嵌套流水线任务',
    `snippet_pipeline_id`     bigint(20) DEFAULT NULL COMMENT '当前任务对应的嵌套流水线 ID',
    `snippet_pipeline_detail` mediumtext COMMENT '当前任务对应的嵌套流水线汇总后的详情',
    `cost_time_sec`           bigint(20) NOT NULL,
    `queue_time_sec`          bigint(20) NOT NULL,
    `time_begin`              datetime              DEFAULT NULL,
    `time_end`                datetime              DEFAULT NULL,
    `time_created`            datetime     NOT NULL,
    `time_updated`            datetime     NOT NULL,
    PRIMARY KEY (`id`),
    KEY                       `idx_pipeline_id` (`pipeline_id`),
    KEY                       `idx_stage_id` (`stage_id`),
    KEY                       `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  COMMENT='流水线任务(task)表';

