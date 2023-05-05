-- MIGRATION_BASE

CREATE TABLE `dice_api_access`
(
    `id`                bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `asset_id`          varchar(191) DEFAULT NULL COMMENT 'asset id',
    `asset_name`        varchar(191) DEFAULT NULL COMMENT 'asset name',
    `org_id`            bigint(20) DEFAULT NULL COMMENT 'organization id',
    `swagger_version`   varchar(16)  DEFAULT NULL COMMENT 'swagger version',
    `major`             int(11) DEFAULT NULL COMMENT 'version major number',
    `minor`             int(11) DEFAULT NULL COMMENT 'version minor number',
    `project_id`        bigint(20) DEFAULT NULL COMMENT 'project id',
    `app_id`            bigint(20) DEFAULT NULL COMMENT 'application id',
    `workspace`         varchar(32)  DEFAULT NULL COMMENT 'DEV, TEST, STAGING, PROD',
    `endpoint_id`       varchar(32)  DEFAULT NULL COMMENT 'gateway endpoint id',
    `authentication`    varchar(32)  DEFAULT NULL COMMENT 'api-key, parameter-sign, auth2',
    `authorization`     varchar(32)  DEFAULT NULL COMMENT 'auto, manual',
    `addon_instance_id` varchar(128) DEFAULT NULL COMMENT 'addon instance id',
    `bind_domain`       varchar(256) DEFAULT NULL COMMENT 'bind domains',
    `creator_id`        varchar(191) DEFAULT NULL COMMENT 'creator user id',
    `updater_id`        varchar(191) DEFAULT NULL COMMENT 'updater user id',
    `created_at`        datetime     DEFAULT NULL COMMENT 'created datetime',
    `updated_at`        datetime     DEFAULT NULL COMMENT 'last updated datetime',
    `project_name`      varchar(191) DEFAULT NULL COMMENT 'project name',
    `app_name`          varchar(191) DEFAULT NULL COMMENT 'app name',
    `default_sla_id`    bigint(20) DEFAULT NULL COMMENT 'default SLA id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  COMMENT='API 集市资源访问管理表';

CREATE TABLE `dice_api_asset_version_instances`
(
    `id`              bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `name`            varchar(191)      DEFAULT NULL COMMENT '实例名',
    `asset_id`        varchar(191)      DEFAULT NULL COMMENT 'API 集市资源 id',
    `version_id`      bigint(20) DEFAULT NULL COMMENT 'dice_api_asset_versions primary key',
    `type`            varchar(32)       DEFAULT NULL COMMENT '实例类型',
    `runtime_id`      bigint(20) DEFAULT NULL COMMENT 'runtime id',
    `service_name`    varchar(191)      DEFAULT NULL COMMENT '服务名称',
    `endpoint_id`     varchar(191)      DEFAULT NULL COMMENT '流量入口 endpoint id',
    `url`             varchar(1024)     DEFAULT NULL COMMENT '实例 url',
    `creator_id`      varchar(191)      DEFAULT NULL COMMENT '创建者 user id',
    `updater_id`      varchar(191)      DEFAULT NULL COMMENT '更新者 user id',
    `created_at`      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `swagger_version` varchar(16)       DEFAULT NULL COMMENT 'swagger version',
    `major`           int(11) DEFAULT NULL COMMENT 'major',
    `minor`           int(11) DEFAULT NULL COMMENT 'minor',
    `project_id`      bigint(20) DEFAULT NULL COMMENT 'project id',
    `app_id`          bigint(20) DEFAULT NULL COMMENT 'application id',
    `org_id`          bigint(20) DEFAULT NULL COMMENT 'organization id',
    `workspace`       varchar(16)       DEFAULT NULL COMMENT 'env',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特定版本的 API 集市资源绑定的实例表';

CREATE TABLE `dice_api_asset_version_specs`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `org_id`        bigint(20) DEFAULT NULL COMMENT 'organization id',
    `asset_id`      varchar(191)      DEFAULT NULL COMMENT 'API 集市资源 id',
    `version_id`    bigint(20) DEFAULT NULL COMMENT 'dice_api_asset_versions primary key',
    `spec_protocol` varchar(32)       DEFAULT NULL COMMENT 'swagger protocol',
    `spec`          longtext COMMENT 'swagger text',
    `creator_id`    varchar(191)      DEFAULT NULL COMMENT 'creator user id',
    `updater_id`    varchar(191)      DEFAULT NULL COMMENT 'updater user id',
    `created_at`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `asset_name`    varchar(191)      DEFAULT NULL COMMENT 'asset name',
    PRIMARY KEY (`id`),
    FULLTEXT KEY `ft_specs` (`spec`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特定版本的 API 集市资源的 swagger specification 内容';

CREATE TABLE `dice_api_asset_versions`
(
    `id`              bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `org_id`          bigint(20) DEFAULT NULL COMMENT 'organization id',
    `asset_id`        varchar(191)          DEFAULT NULL COMMENT 'API 集市资源 id',
    `major`           int(11) DEFAULT NULL COMMENT 'version major number',
    `minor`           int(11) DEFAULT NULL COMMENT 'version minor number',
    `patch`           int(11) DEFAULT NULL COMMENT 'version patch number',
    `desc`            varchar(1024)         DEFAULT NULL COMMENT 'description',
    `spec_protocol`   varchar(32)           DEFAULT NULL COMMENT 'swagger protocol',
    `creator_id`      varchar(191)          DEFAULT NULL COMMENT 'creator user id',
    `updater_id`      varchar(191)          DEFAULT NULL COMMENT 'updater user id',
    `created_at`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `swagger_version` varchar(16)           DEFAULT NULL COMMENT '用户自定义的版本号, 相当于一个 tag',
    `asset_name`      varchar(191)          DEFAULT NULL COMMENT 'asset name',
    `deprecated`      tinyint(1) DEFAULT '0' COMMENT 'is the asset version deprecated',
    `source`          varchar(16)  NOT NULL COMMENT '该版本文档来源',
    `app_id`          bigint(20) NOT NULL COMMENT '应用 id',
    `branch`          varchar(191) NOT NULL COMMENT '分支名',
    `service_name`    varchar(191) NOT NULL COMMENT '服务名',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 集市资源的版本列表';

CREATE TABLE `dice_api_assets`
(
    `id`             bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `asset_id`       varchar(191)      DEFAULT NULL COMMENT 'API 集市资源 id',
    `asset_name`     varchar(191)      DEFAULT NULL COMMENT '集市名称',
    `desc`           varchar(1024)     DEFAULT NULL COMMENT '描述信息',
    `logo`           varchar(1024)     DEFAULT NULL COMMENT 'logo 地址',
    `org_id`         bigint(20) DEFAULT NULL COMMENT 'organization id',
    `project_id`     bigint(20) DEFAULT NULL COMMENT '项目 id',
    `app_id`         bigint(20) DEFAULT NULL COMMENT '应用 id',
    `creator_id`     varchar(191)      DEFAULT NULL COMMENT 'creator user id',
    `updater_id`     varchar(191)      DEFAULT NULL COMMENT 'updater user id',
    `created_at`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `public`         tinyint(1) DEFAULT '0' COMMENT 'public',
    `cur_version_id` bigint(20) DEFAULT NULL COMMENT 'latest version id',
    `cur_major`      int(11) DEFAULT NULL COMMENT 'latest version major',
    `cur_minor`      int(11) DEFAULT NULL COMMENT 'latest version minor',
    `cur_patch`      int(11) DEFAULT NULL COMMENT 'latest version patch',
    `project_name`   varchar(191)      DEFAULT NULL COMMENT 'project name',
    `app_name`       varchar(191)      DEFAULT NULL COMMENT 'app name',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 集市资源表';

CREATE TABLE `dice_api_clients`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `org_id`       bigint(20) DEFAULT NULL COMMENT 'organization id',
    `name`         varchar(64)       DEFAULT NULL COMMENT 'client name',
    `desc`         varchar(1024)     DEFAULT NULL COMMENT 'describe',
    `client_id`    varchar(32)       DEFAULT NULL COMMENT 'client id',
    `creator_id`   varchar(191)      DEFAULT NULL COMMENT 'creator user id',
    `updater_id`   varchar(191)      DEFAULT NULL COMMENT 'updater user id',
    `created_at`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `alias_name`   varchar(64)       DEFAULT NULL COMMENT 'alias name',
    `display_name` varchar(191)      DEFAULT NULL COMMENT 'client display name',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  COMMENT='API 集市资源访问管理表';

CREATE TABLE `dice_api_contract_records`
(
    `id`          bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `org_id`      bigint(20) DEFAULT NULL COMMENT 'organization id',
    `contract_id` bigint(20) DEFAULT NULL COMMENT 'dice_api_contracts primary key',
    `action`      varchar(64)       DEFAULT NULL COMMENT 'operation describe',
    `creator_id`  varchar(191)      DEFAULT NULL COMMENT 'operation user id',
    `created_at`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  COMMENT='API 集市资源访问管理合约操作记录表';

CREATE TABLE `dice_api_contracts`
(
    `id`               bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `asset_id`         varchar(191)      DEFAULT NULL COMMENT 'asset id',
    `asset_name`       varchar(191)      DEFAULT NULL COMMENT 'asset name',
    `org_id`           bigint(20) DEFAULT NULL COMMENT 'organization id',
    `swagger_version`  varchar(16)       DEFAULT NULL COMMENT 'swagger version',
    `client_id`        bigint(20) DEFAULT NULL COMMENT 'primary key of table dice_api_client',
    `status`           varchar(16)       DEFAULT NULL COMMENT 'proved:已授权, proving:待审批, disproved:已撤销',
    `creator_id`       varchar(191)      DEFAULT NULL COMMENT 'creator user id',
    `updater_id`       varchar(191)      DEFAULT NULL COMMENT 'updater user id',
    `created_at`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `cur_sla_id`       bigint(20) DEFAULT NULL COMMENT 'contract current SLA id',
    `request_sla_id`   bigint(20) DEFAULT NULL COMMENT 'contract request SLA',
    `sla_committed_at` datetime          DEFAULT NULL COMMENT 'current SLA committed time',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  COMMENT='API 集市资源访问管理合约表';

CREATE TABLE `dice_api_doc_lock`
(
    `id`             bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `created_at`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `session_id`     char(36)     NOT NULL COMMENT '会话标识',
    `is_locked`      tinyint(1) NOT NULL DEFAULT '0' COMMENT '会话所有者是否持有文档锁',
    `expired_at`     datetime     NOT NULL COMMENT '会话过期时间',
    `application_id` bigint(20) NOT NULL COMMENT '应用 id',
    `branch_name`    varchar(191) NOT NULL COMMENT '分支名',
    `doc_name`       varchar(191) NOT NULL COMMENT '文档名, 也即服务名',
    `creator_id`     varchar(191) NOT NULL COMMENT '创建者 id',
    `updater_id`     varchar(191) NOT NULL COMMENT '更新者 id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc` (`application_id`,`branch_name`,`doc_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 设计中心文档锁表';

CREATE TABLE `dice_api_doc_tmp_content`
(
    `id`             bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `created_at`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    `updated_at`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    `application_id` bigint(20) NOT NULL COMMENT '应用 id',
    `branch_name`    varchar(191) NOT NULL COMMENT '分支名',
    `doc_name`       varchar(64)  NOT NULL COMMENT '文档名',
    `content`        longtext     NOT NULL COMMENT 'API doc text',
    `creator_id`     varchar(191) NOT NULL COMMENT 'creator id',
    `updater_id`     varchar(191) NOT NULL COMMENT 'updater id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inode` (`application_id`,`branch_name`,`doc_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 设计中心文档临时存储表';

CREATE TABLE `dice_api_oas3_fragment`
(
    `id`         bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    `index_id`   bigint(20) NOT NULL COMMENT 'dice_api_oas3_index primary key',
    `version_id` bigint(20) NOT NULL COMMENT 'asset version primary key',
    `operation`  text     NOT NULL COMMENT '.paths.{path}.{method}.parameters, 序列化了的 parameters JSON 片段',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 集市 oas3 片段表';

CREATE TABLE `dice_api_oas3_index`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `created_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    `updated_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    `asset_id`     varchar(191) NOT NULL COMMENT 'asset id',
    `asset_name`   varchar(191) NOT NULL COMMENT 'asset name',
    `info_version` varchar(191) NOT NULL COMMENT '.info.version value, 也即 swaggerVersion',
    `version_id`   bigint(20) NOT NULL COMMENT 'asset version primary key',
    `path`         varchar(191) NOT NULL COMMENT '.paths.{path}',
    `method`       varchar(16)  NOT NULL COMMENT '.paths.{path}.{method}',
    `operation_id` varchar(191) NOT NULL COMMENT '.paths.{path}.{method}.operationId',
    `description`  text         NOT NULL COMMENT '.path.{path}.{method}.description',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_path_method` (`version_id`,`path`,`method`) COMMENT '同一文档下, path + method 确定一个接口'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 集市 operation 搜索索引表';

CREATE TABLE `dice_api_sla_limits`
(
    `id`         bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` varchar(191)      DEFAULT NULL COMMENT 'creator id',
    `updater_id` varchar(191)      DEFAULT NULL COMMENT 'creator id',
    `sla_id`     bigint(20) DEFAULT NULL COMMENT 'SLA model id',
    `limit`      bigint(20) DEFAULT NULL COMMENT 'request limit',
    `unit`       varchar(16)       DEFAULT NULL COMMENT 's: second, m: minute, h: hour, d: day',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  COMMENT='API 集市访问管理 SLA 限制条件表';

CREATE TABLE `dice_api_slas`
(
    `id`         bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` varchar(191)      DEFAULT NULL COMMENT 'creator id',
    `updater_id` varchar(191)      DEFAULT NULL COMMENT 'creator id',
    `name`       varchar(191)      DEFAULT NULL COMMENT 'SLA name',
    `desc`       varchar(1024)     DEFAULT NULL COMMENT 'description',
    `approval`   varchar(16)       DEFAULT NULL COMMENT 'auto, manual',
    `access_id`  bigint(20) DEFAULT NULL COMMENT 'access id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  COMMENT='API 集市访问管理 Service Level Agreements 表';

