CREATE TABLE `erda_deployment_order`
(
    `id`               varchar(36)  NOT NULL DEFAULT '' COMMENT 'deployment order id',
    `type`             varchar(32)  NOT NULL COMMENT 'deployment order type',
    `description`      text         NOT NULL COMMENT 'description',
    `release_id`       varchar(64)  NOT NULL COMMENT 'release id',
    `operator`         varchar(255) NOT NULL COMMENT 'operator',
    `project_id`       bigint(20) unsigned NOT NULL COMMENT 'project id',
    `project_name`     varchar(80)  NOT NULL DEFAULT '' COMMENT 'project name',
    `application_id`   bigint(20) NOT NULL COMMENT 'application id',
    `application_name` varchar(80)  NOT NULL DEFAULT '' COMMENT 'application name',
    `workspace`        varchar(16)  NOT NULL COMMENT 'workspace',
    `status`           text         NOT NULL COMMENT 'application status',
    `params`           text         NOT NULL COMMENT 'application deploy params',
    `is_outdated`      tinyint(1) NOT NULL DEFAULT 0 COMMENT 'outdated',
    `created_at`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    `updated_at`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    `started_at`       datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT 'started time',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='erda deployment order';


ALTER TABLE `ps_v2_deployments`
    ADD COLUMN `param` text COMMENT 'deployment param',
    ADD COLUMN `deployment_order_id` varchar(36) NOT NULL DEFAULT '' COMMENT 'deployment order id';

ALTER TABLE `ps_v2_project_runtimes`
    ADD COLUMN `deployment_order_id` varchar(36) COMMENT 'deployment order id',
    ADD COLUMN `release_version` varchar(100) COMMENT 'deployment order version',
    ADD COLUMN `deployment_status` varchar(255) COMMENT 'deployment status',
    ADD COLUMN `current_deployment_id` bigint unsigned NOT NULL DEFAULT 0 COMMENT 'current deployment id';
