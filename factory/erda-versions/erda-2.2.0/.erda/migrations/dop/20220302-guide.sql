CREATE TABLE `erda_guide`
(
    `id`              varchar(36)  NOT NULL COMMENT '自增id',
    `status`          varchar(20)  NOT NULL COMMENT '处理状态 init,processed,expired',
    `kind`            varchar(20)  NOT NULL COMMENT '类型 pipeline...',
    `creator`         varchar(36)  NOT NULL DEFAULT '' COMMENT '创建者',
    `org_id`          bigint(20) NOT NULL COMMENT 'orgID',
    `org_name`        varchar(50)  NOT NULL DEFAULT '' COMMENT '组织名',
    `project_id`      bigint(20) NOT NULL DEFAULT 0 COMMENT 'projectID',
    `app_id`          bigint(20) NOT NULL DEFAULT 0 COMMENT 'appID',
    `app_name`        varchar(50) NOT NULL DEFAULT '' COMMENT 'appName',
    `branch`          varchar(36)  NOT NULL DEFAULT '' COMMENT '分支',
    `created_at`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'CREATED AT',
    `updated_at`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'UPDATED AT',
    `soft_deleted_at` bigint(20) NOT NULL DEFAULT '0' COMMENT '软删除',
    PRIMARY KEY (`id`),
    KEY               `idx_porjectid_creator_kind_status_createdat` (`soft_deleted_at`,`project_id`,`creator`,`kind`, `status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='erda_guide';