CREATE TABLE `erda_dev_flow_rule`
(
    `id`           varchar(36) NOT NULL COMMENT 'id',
    `flows`        mediumtext  NOT NULL COMMENT 'flows',
    `org_id`       bigint(20) NOT NULL COMMENT 'orgID',
    `org_name`     varchar(50) NOT NULL DEFAULT '' COMMENT 'orgName',
    `project_id`   bigint(20) NOT NULL DEFAULT 0 COMMENT 'projectID',
    `project_name` varchar(50) NOT NULL DEFAULT '' COMMENT 'projectName',
    `creator`      varchar(36) NOT NULL DEFAULT '' COMMENT 'creator',
    `updater`      varchar(36) NOT NULL DEFAULT '' COMMENT 'updater',
    `created_at`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'CREATED AT',
    `updated_at`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'UPDATED AT',
    `deleted_at`   bigint(20) NOT NULL DEFAULT '0' COMMENT 'deleted_at',
    PRIMARY KEY (`id`),
    KEY            `idx_projectid` (`project_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='erda_dev_flow_rule';